package com.testpilot.platform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.requirement.Requirement;
import com.testpilot.platform.requirement.RequirementRepository;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.testcase.TestCase;
import com.testpilot.platform.testcase.TestCaseRepository;
import com.testpilot.platform.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AiGenerationService.class);

    private final AiModelService aiModelService;
    private final RequirementRepository requirementRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProjectAccessService projectAccessService;
    private final ObjectMapper objectMapper;
    private final AiProviderClient aiProviderClient;
    private final AiCallAuditService aiCallAuditService;

    public AiGenerationService(
            AiModelService aiModelService,
            RequirementRepository requirementRepository,
            TestCaseRepository testCaseRepository,
            ProjectAccessService projectAccessService,
            ObjectMapper objectMapper,
            AiProviderClient aiProviderClient,
            AiCallAuditService aiCallAuditService
    ) {
        this.aiModelService = aiModelService;
        this.requirementRepository = requirementRepository;
        this.testCaseRepository = testCaseRepository;
        this.projectAccessService = projectAccessService;
        this.objectMapper = objectMapper;
        this.aiProviderClient = aiProviderClient;
        this.aiCallAuditService = aiCallAuditService;
    }

    @Transactional
    public AiDtos.GenerateResponse generate(
            Long projectId,
            Long requirementId,
            AiDtos.GenerateRequest request,
            User currentUser
    ) {
        projectAccessService.requireWrite(projectId, currentUser);
        Requirement requirement = requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new BusinessException(40402, "需求文档不存在"));
        if (!"PARSED".equals(requirement.getParseStatus())
                || requirement.getContentText() == null
                || requirement.getContentText().isBlank()) {
            throw new BusinessException(40006, "需求文档尚未解析完成，无法生成测试用例");
        }

        AiModel model = aiModelService.findModel(request == null ? null : request.modelId());
        PromptTemplate prompt = aiModelService.findPrompt(
                request == null ? null : request.promptId(),
                "TEST_CASE_GENERATION"
        );
        String rules = request == null || request.rules() == null || request.rules().isBlank()
                ? "覆盖正向、异常和边界场景，输出结构化测试用例。"
                : request.rules().trim();
        long start = System.nanoTime();
        List<GeneratedCase> generatedCases;
        try {
            String generatedJson = "MOCK".equalsIgnoreCase(model.getProvider())
                    ? mockResponse(requirement, rules)
                    : callModel(model, prompt, requirement.getContentText(), rules);
            generatedCases = parseGeneratedCases(generatedJson);
            aiCallAuditService.record(projectId, currentUser, "TEST_CASE_GENERATION", model,
                    prompt.getId(), prompt.getName(), prompt.getVersion(),
                    true, null, null, elapsedMs(start));
        } catch (BusinessException exception) {
            aiCallAuditService.record(projectId, currentUser, "TEST_CASE_GENERATION", model,
                    prompt.getId(), prompt.getName(), prompt.getVersion(),
                    false, exception.getCode(), exception.getMessage(), elapsedMs(start));
            throw exception;
        }
        List<AiDtos.TestCaseView> savedCases = new ArrayList<>();
        for (GeneratedCase generated : generatedCases) {
            String caseNo = generated.caseNo();
            if (caseNo == null || caseNo.isBlank()
                    || testCaseRepository.existsByProjectIdAndCaseNo(projectId, caseNo)) {
                caseNo = nextCaseNo(projectId);
            }
            TestCase testCase = new TestCase(
                    projectId,
                    requirementId,
                    caseNo,
                    generated.title(),
                    generated.moduleName(),
                    generated.precondition(),
                    generated.steps(),
                    generated.testData(),
                    generated.expectedResult(),
                    generated.priority(),
                    generated.caseType(),
                    generated.tags(),
                    "DRAFT",
                    "AI",
                    currentUser.getId()
            );
            TestCase saved = testCaseRepository.save(testCase);
            savedCases.add(new AiDtos.TestCaseView(
                    saved.getId(),
                    saved.getCaseNo(),
                    saved.getTitle(),
                    saved.getStatus()
            ));
        }
        return new AiDtos.GenerateResponse(
                requirementId,
                model.getId(),
                model.getProvider(),
                savedCases
        );
    }

    private String callModel(
            AiModel model,
            PromptTemplate prompt,
            String requirementText,
            String rules
    ) {
        String userPrompt = prompt.getContent()
                + "\n\n生成规则:\n"
                + rules
                + "\n\n需求正文:\n"
                + requirementText;
        return aiProviderClient.completeJson(model, userPrompt);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private List<GeneratedCase> parseGeneratedCases(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(rawJson));
            JsonNode casesNode = root.isArray() ? root : root.path("testCases");
            if (!casesNode.isArray() || casesNode.isEmpty()) {
                throw new BusinessException(50202, "AI 未返回有效测试用例");
            }
            List<GeneratedCase> result = new ArrayList<>();
            for (JsonNode item : casesNode) {
                String title = text(item, "title");
                if (title.isBlank()) {
                    continue;
                }
                String module = requiredText(item, "moduleName", "module", "模块");
                String precondition = requiredText(item, "precondition", "前置条件");
                String steps = requiredText(item, "steps", "测试步骤");
                String testData = requiredText(item, "testData", "测试数据");
                String expectedResult = requiredText(item, "expectedResult", "预期结果");
                String priority = normalizePriority(text(item, "priority", "优先级"));
                String caseType = normalizeType(text(item, "caseType", "type", "测试类型"));
                result.add(new GeneratedCase(
                        text(item, "caseNo"),
                        title,
                        module,
                        precondition,
                        steps,
                        testData,
                        expectedResult,
                        priority,
                        caseType,
                        text(item, "tags")
                ));
                if (result.size() >= 50) {
                    break;
                }
            }
            if (result.isEmpty()) {
                throw new BusinessException(50202, "AI 返回的测试用例缺少标题");
            }
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            String preview = rawJson == null ? "<null>" : rawJson.substring(0, Math.min(800, rawJson.length()));
            log.error("AI 返回内容无法解析为 JSON，原始内容: {}", preview, exception);
            throw new BusinessException(50202, "AI 返回的 JSON 格式不正确");
        }
    }

    private String mockResponse(Requirement requirement, String rules) {
        String module = firstModule(requirement.getContentText());
        return """
                {
                  "testCases": [
                    {
                      "caseNo": "TC-AI-001",
                      "title": "核心流程正向场景",
                      "module": "%s",
                      "precondition": "测试环境和必要数据已准备完成",
                      "steps": "1. 按需求输入有效数据\\n2. 提交请求或操作",
                      "testData": "有效业务数据",
                      "expectedResult": "操作成功，结果符合需求描述",
                      "priority": "P0",
                      "type": "FUNCTIONAL",
                      "tags": "AI,DRAFT"
                    },
                    {
                      "caseNo": "TC-AI-002",
                      "title": "非法数据异常场景",
                      "module": "%s",
                      "precondition": "测试环境可用",
                      "steps": "1. 输入非法或缺失数据\\n2. 提交请求或操作",
                      "testData": "非法业务数据",
                      "expectedResult": "系统拒绝操作并返回明确提示",
                      "priority": "P1",
                      "type": "EXCEPTION",
                      "tags": "AI,DRAFT"
                    },
                    {
                      "caseNo": "TC-AI-003",
                      "title": "边界数据校验场景",
                      "module": "%s",
                      "precondition": "测试环境可用",
                      "steps": "1. 输入边界值\\n2. 提交请求或操作",
                      "testData": "边界业务数据",
                      "expectedResult": "系统按照边界规则正确处理",
                      "priority": "P1",
                      "type": "BOUNDARY",
                      "tags": "AI,DRAFT"
                    }
                  ]
                }
                """.formatted(module, module, module);
    }

    private String firstModule(String content) {
        for (String line : content.split("\\R")) {
            String trimmed = line.replace("#", "").trim();
            if (!trimmed.isBlank()) {
                return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
            }
        }
        return "未分类模块";
    }

    private String extractJson(String rawJson) {
        if (rawJson == null) {
            return "";
        }
        String value = rawJson.trim();
        int start = -1;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return value;
        }
        char close = value.charAt(start) == '[' ? ']' : '}';
        int end = value.lastIndexOf(close);
        if (end <= start) {
            return value;
        }
        return value.substring(start, end + 1);
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isMissingNode() || value.isNull()) {
                continue;
            }
            if (value.isArray()) {
                StringBuilder joined = new StringBuilder();
                for (JsonNode element : value) {
                    if (element.isNull()) {
                        continue;
                    }
                    String part = element.asText("").trim();
                    if (part.isEmpty()) {
                        continue;
                    }
                    if (joined.length() > 0) {
                        joined.append('\n');
                    }
                    joined.append(part);
                }
                return joined.toString();
            }
            if (value.isObject()) {
                return value.toString();
            }
            return value.asText("").trim();
        }
        return "";
    }

    private String normalizePriority(String priority) {
        String value = priority == null ? "" : priority.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "P0", "HIGH", "CRITICAL", "BLOCKER", "URGENT", "高", "紧急", "严重" -> "P0";
            case "P1", "MEDIUM", "MAJOR", "中", "重要" -> "P1";
            case "P2", "NORMAL", "MINOR", "低", "一般" -> "P2";
            case "P3", "LOW", "TRIVIAL", "较低", "轻微" -> "P3";
            default -> {
                log.warn("AI 返回了无法识别的优先级值，已回退为 P2: {}", priority);
                yield "P2";
            }
        };
    }

    private String normalizeType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "FUNCTIONAL", "POSITIVE", "NORMAL", "SUCCESS", "正向" -> "FUNCTIONAL";
            case "EXCEPTION", "NEGATIVE", "FAILURE", "ERROR", "异常", "反向" -> "EXCEPTION";
            case "BOUNDARY", "边界" -> "BOUNDARY";
            default -> {
                log.warn("AI 返回了无法识别的测试类型值，已回退为 FUNCTIONAL: {}", type);
                yield "FUNCTIONAL";
            }
        };
    }

    private String requiredText(JsonNode node, String... names) {
        String value = text(node, names);
        if (value.isBlank()) {
            throw new BusinessException(50205, "AI 返回缺少必填字段: " + names[0]);
        }
        return value;
    }

    private String nextCaseNo(Long projectId) {
        return "TC-AI-" + String.format("%03d", testCaseRepository.countByProjectId(projectId) + 1);
    }

    private record GeneratedCase(
            String caseNo,
            String title,
            String moduleName,
            String precondition,
            String steps,
            String testData,
            String expectedResult,
            String priority,
            String caseType,
            String tags
    ) {
    }
}
