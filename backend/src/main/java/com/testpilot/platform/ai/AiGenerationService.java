package com.testpilot.platform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.requirement.Requirement;
import com.testpilot.platform.requirement.RequirementRepository;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.testcase.TestCase;
import com.testpilot.platform.testcase.TestCaseRepository;
import com.testpilot.platform.user.User;
import java.util.ArrayList;
import java.util.HashSet;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.testpilot.platform.requirement.RequirementParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiGenerationService {

    public static final String SUITE_FUNCTIONAL = "FUNCTIONAL";
    public static final String SUITE_API = "API";

    private static final Logger log = LoggerFactory.getLogger(AiGenerationService.class);
    private static final Set<String> STANDARD_CASE_FIELDS = Set.of(
            "caseno", "case_no", "用例编号", "title", "用例标题", "modulename", "module_name", "module", "模块", "所属模块", "precondition", "pre_condition", "前置条件",
            "steps", "步骤", "测试步骤", "testdata", "test_data", "测试数据", "expectedresult", "expected_result", "预期", "预期结果",
            "priority", "优先级", "casetype", "case_type", "type", "类型", "测试类型", "suitetype", "suite_type", "套件类型", "tags", "标签"
    );

    private final AiModelService aiModelService;
    private final RequirementRepository requirementRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProjectAccessService projectAccessService;
    private final ObjectMapper objectMapper;
    private final AiProviderClient aiProviderClient;
    private final AiCallAuditService aiCallAuditService;
    private final RequirementParser requirementParser;

    public AiGenerationService(
            AiModelService aiModelService,
            RequirementRepository requirementRepository,
            TestCaseRepository testCaseRepository,
            ProjectAccessService projectAccessService,
            ObjectMapper objectMapper,
            AiProviderClient aiProviderClient,
            AiCallAuditService aiCallAuditService,
            RequirementParser requirementParser
    ) {
        this.aiModelService = aiModelService;
        this.requirementRepository = requirementRepository;
        this.testCaseRepository = testCaseRepository;
        this.projectAccessService = projectAccessService;
        this.objectMapper = objectMapper;
        this.aiProviderClient = aiProviderClient;
        this.aiCallAuditService = aiCallAuditService;
        this.requirementParser = requirementParser;
    }

    @Transactional
    public AiDtos.GenerateResponse generate(
            Long projectId,
            Long requirementId,
            AiDtos.GenerateRequest request,
            User currentUser
    ) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AI_GENERATE);
        Requirement requirement = requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new BusinessException(40402, "项目文件不存在"));
        String requirementText = readFileContent(requirement);

        AiModel model = aiModelService.findModel(request == null ? null : request.modelId());
        PromptTemplate prompt = findGenerationPrompt(request == null ? null : request.promptId());
        String rules = request == null || request.rules() == null || request.rules().isBlank()
                ? "按业务能力和风险生成可执行测试用例，覆盖主流程、必填校验、有效/无效等价类、明确边界值、权限、状态流转、异常、回显和一致性；不明确规则放入 pendingItems。"
                : request.rules().trim();
        String suiteType = normalizeSuiteType(request == null ? null : request.suiteType());
        long start = System.nanoTime();
        List<GeneratedCase> generatedCases;
        try {
            String generatedJson = "MOCK".equalsIgnoreCase(model.getProvider())
                    ? mockResponse(requirementText, rules)
                    : callModel(model, prompt, requirementText, rules, suiteType);
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
        Set<String> assignedCaseNos = new HashSet<>();
        for (GeneratedCase generated : generatedCases) {
            String caseNo = generated.caseNo();
            if (caseNo == null || caseNo.isBlank()
                    || assignedCaseNos.contains(caseNo)
                    || testCaseRepository.existsByProjectIdAndSuiteTypeAndCaseNo(projectId, suiteType, caseNo)) {
                caseNo = nextCaseNo(projectId, suiteType, assignedCaseNos, generated.caseNo());
            }
            assignedCaseNos.add(caseNo);
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
                    suiteType,
                    generated.tags(),
                    "DRAFT",
                    "AI",
                    currentUser.getId(),
                    generated.extraFieldsJson()
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
            String rules,
            String suiteType
    ) {
        String userPrompt = prompt.getContent()
                + "\n\n生成规则:\n"
                + rules
                + "\n\n测试范围:\n"
                + suiteType
                + "\n\n需求正文:\n"
                + requirementText;
        return aiProviderClient.completeJson(model, userPrompt);
    }

    private PromptTemplate findGenerationPrompt(Long promptId) {
        try {
            return aiModelService.findPrompt(promptId, "TEST_CASE_GENERATION");
        } catch (BusinessException exception) {
            if (exception.getCode() == 40421 && promptId == null) {
                return new PromptTemplate(
                        "内置测试用例生成规范",
                        "TEST_CASE_GENERATION",
                        BuiltInPrompts.TEST_CASE_GENERATION,
                        "V3.1",
                        true
                );
            }
            throw exception;
        }
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
                String title = text(item, "title", "用例标题");
                if (title.isBlank()) {
                    continue;
                }
            String module = requiredText(item, "moduleName", "module", "模块", "所属模块");
            String caseNo = requiredText(item, "caseNo", "case_no", "用例编号");
            caseNo = validateCaseNo(caseNo);
                String precondition = requiredText(item, "precondition", "前置条件");
                String steps = requiredText(item, "steps", "步骤", "测试步骤");
                String testData = text(item, "testData", "测试数据");
                String expectedResult = requiredText(item, "expectedResult", "预期结果", "预期");
                String priority = normalizePriority(text(item, "priority", "优先级"));
                String caseType = normalizeType(text(item, "caseType", "type", "类型", "测试类型"));
                result.add(new GeneratedCase(
                        caseNo,
                        title,
                        module,
                        precondition,
                        steps,
                        testData,
                        expectedResult,
                        priority,
                        caseType,
                        text(item, "tags", "标签"),
                        extractExtraFieldsJson(item)
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

    private String mockResponse(String requirementText, String rules) {
        String module = firstModule(requirementText);
        return """
                {
                  "testCases": [
                    {
                      "caseNo": "TC-AI-001",
                      "productName": "",
                      "title": "验证%s核心业务流程处理正确",
                      "moduleName": "%s",
                      "precondition": "1. 测试账号已登录并具备当前模块操作权限\\n2. 测试数据处于可提交状态",
                      "steps": "1. 进入%s页面\\n2. 输入有效业务数据\\n3. 点击提交或保存",
                      "expectedResult": "1. 页面展示当前模块及可操作控件\\n2. 系统接受有效数据并完成提交\\n3. 页面显示成功反馈，数据状态更新且列表可查询",
                      "priority": "P0",
                      "type": "FUNCTIONAL"
                    },
                    {
                      "caseNo": "TC-AI-002",
                      "productName": "",
                      "title": "验证%s必填字段校验有效",
                      "moduleName": "%s",
                      "precondition": "1. 测试账号已登录并进入当前模块页面\\n2. 当前页面存在可提交表单",
                      "steps": "1. 保持必填字段为空\\n2. 填写其他必要字段\\n3. 点击提交或保存",
                      "expectedResult": "1. 系统阻止提交\\n2. 空值字段被定位或标记\\n3. 页面展示与该字段对应的校验提示，已有数据不被错误保存",
                      "priority": "P1",
                      "type": "EXCEPTION"
                    },
                    {
                      "caseNo": "TC-AI-003",
                      "productName": "",
                      "title": "验证%s边界数据处理符合规则",
                      "moduleName": "%s",
                      "precondition": "1. 测试账号已登录并进入当前模块页面\\n2. 已明确待验证字段的有效边界值",
                      "steps": "1. 输入边界值及其他有效数据\\n2. 点击提交或保存",
                      "expectedResult": "1. 边界内数据按业务规则处理\\n2. 边界外数据被阻止并展示明确校验结果\\n3. 系统不产生部分保存或错误状态",
                      "priority": "P1",
                      "type": "BOUNDARY"
                    }
                  ]
                }
                """.formatted(module, module, module, module, module, module, module);
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

    private String extractExtraFieldsJson(JsonNode node) {
        if (!node.isObject()) {
            return "{}";
        }
        ObjectNode extras = objectMapper.createObjectNode();
        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (!STANDARD_CASE_FIELDS.contains(key.trim().toLowerCase(Locale.ROOT))) {
                extras.set(key, entry.getValue());
            }
        });
        if (extras.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(extras);
        } catch (Exception exception) {
            log.warn("AI 扩展字段序列化失败，已忽略", exception);
            return "{}";
        }
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

    private String readFileContent(Requirement requirement) {
        if (requirement.getContentText() != null && !requirement.getContentText().isBlank()) {
            return requirement.getContentText();
        }
        if (requirement.getFilePath() == null || requirement.getFilePath().isBlank()) {
            throw new BusinessException(40006, "项目文件没有可读取的内容，无法生成测试用例");
        }
        try (var inputStream = Files.newInputStream(Path.of(requirement.getFilePath()))) {
            String content = requirementParser.parse(requirement.getFileType(), inputStream);
            if (content == null || content.isBlank()) {
                throw new BusinessException(40006, "项目文件没有可读取的内容，无法生成测试用例");
            }
            return content;
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(40006, "该文件格式暂不支持 AI 读取");
        }
    }

    private String normalizeSuiteType(String suiteType) {
        String value = suiteType == null ? "" : suiteType.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case SUITE_FUNCTIONAL, SUITE_API -> value;
            default -> SUITE_FUNCTIONAL;
        };
    }

    private String requiredText(JsonNode node, String... names) {
        String value = text(node, names);
        if (value.isBlank()) {
            throw new BusinessException(50205, "AI 返回缺少必填字段: " + names[0]);
        }
        return value;
    }

    private String nextCaseNo(Long projectId, String suiteType, Set<String> reservedCaseNos, String seedCaseNo) {
        String prefix = casePrefix(seedCaseNo);
        long next = sequenceOf(seedCaseNo) + 1;
        next = Math.max(next, testCaseRepository.countByProjectIdAndSuiteType(projectId, suiteType) + 1);
        String caseNo;
        do {
            caseNo = prefix + "_" + String.format("%03d", next++);
        } while (reservedCaseNos.contains(caseNo)
                || testCaseRepository.existsByProjectIdAndSuiteTypeAndCaseNo(projectId, suiteType, caseNo));
        return caseNo;
    }

    private String casePrefix(String caseNo) {
        if (caseNo != null) {
            int separator = caseNo.lastIndexOf('_');
            if (separator > 0) {
                String prefix = caseNo.substring(0, separator).trim().toUpperCase(Locale.ROOT);
                if (!prefix.isBlank()) return prefix;
            }
        }
        return "MODULE";
    }

    private String validateCaseNo(String value) {
        String caseNo = value.trim().toUpperCase(Locale.ROOT);
        if (!caseNo.matches("[A-Z][A-Z0-9-]*_[0-9]{3}")) {
            throw new BusinessException(50205, "AI 返回的用例编号不符合规范，应为模块大写拼音缩写_三位序号，例如 DENGLU_001");
        }
        return caseNo;
    }

    private long sequenceOf(String caseNo) {
        if (caseNo == null) return 0;
        int separator = caseNo.lastIndexOf('_');
        if (separator < 0) return 0;
        try {
            return Long.parseLong(caseNo.substring(separator + 1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
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
            String tags,
            String extraFieldsJson
    ) {
    }
}
