package com.testpilot.platform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.requirement.RequirementRepository;
import com.testpilot.platform.testcase.TestCase;
import com.testpilot.platform.testcase.TestCaseRepository;
import com.testpilot.platform.user.User;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiCaseAssistantService {

    private static final String BUILT_IN_SYSTEM_PROMPT = "你是一名资深测试用例助手，帮助用户优化 AI 生成的测试用例、补充边界场景、梳理 Review 意见，并给出简洁可执行的建议。";
    private static final Set<String> STANDARD_CASE_FIELDS = Set.of(
            "caseno", "case_no", "title", "modulename", "module_name", "module", "模块", "precondition", "pre_condition", "前置条件",
            "steps", "测试步骤", "testdata", "test_data", "测试数据", "expectedresult", "expected_result", "预期结果",
            "priority", "优先级", "casetype", "case_type", "type", "测试类型", "suitetype", "suite_type", "套件类型", "tags", "标签"
    );

    private final AiModelService aiModelService;
    private final ProjectAccessService projectAccessService;
    private final RequirementRepository requirementRepository;
    private final TestCaseRepository testCaseRepository;
    private final ObjectMapper objectMapper;
    private final AiProviderClient aiProviderClient;
    private final AiCallAuditService aiCallAuditService;
    private final AiCaseChatSessionService chatSessionService;

    public AiCaseAssistantService(
            AiModelService aiModelService,
            ProjectAccessService projectAccessService,
            RequirementRepository requirementRepository,
            TestCaseRepository testCaseRepository,
            ObjectMapper objectMapper,
            AiProviderClient aiProviderClient,
            AiCallAuditService aiCallAuditService,
            AiCaseChatSessionService chatSessionService
    ) {
        this.aiModelService = aiModelService;
        this.projectAccessService = projectAccessService;
        this.requirementRepository = requirementRepository;
        this.testCaseRepository = testCaseRepository;
        this.objectMapper = objectMapper;
        this.aiProviderClient = aiProviderClient;
        this.aiCallAuditService = aiCallAuditService;
        this.chatSessionService = chatSessionService;
    }

    @Transactional
    public AiDtos.CaseChatResponse chat(Long projectId, AiDtos.CaseChatRequest request, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AI_GENERATE);
        AiModel model = aiModelService.findModel(request.modelId());
        PromptTemplate prompt = findPrompt();
        String promptContent = prompt == null ? BUILT_IN_SYSTEM_PROMPT : prompt.getContent();
        String normalizedSuiteType = normalizeSuiteType(request.suiteType());
        validateRequirement(projectId, request.requirementId());
        validateSelectedCase(projectId, request.selectedCaseId(), normalizedSuiteType);
        String context = buildContext(projectId, request.requirementId(), normalizedSuiteType);
        String message = request.message().trim();
        String userPrompt = buildChatPrompt(context, message, normalizedSuiteType, request.selectedCaseId());
        long start = System.nanoTime();
        try {
            String rawReply = "MOCK".equalsIgnoreCase(model.getProvider())
                    ? mockReply(message, context, normalizedSuiteType)
                    : aiProviderClient.completeJson(model, promptContent, userPrompt);
            ChatPayload payload = parseChatPayload(rawReply, hasGenerationIntent(message));
            if (hasGenerationIntent(message) && payload.generatedCases().isEmpty()) {
                throw new BusinessException(50202, "AI 未返回可落库的测试用例");
            }
            List<AiDtos.TestCaseView> generatedCases = saveGeneratedCases(
                    projectId,
                    request.requirementId(),
                    normalizedSuiteType,
                    request.selectedCaseId(),
                    currentUser,
                    payload.generatedCases()
            );
            aiCallAuditService.record(
                    projectId,
                    currentUser,
                    "TEST_CASE_CHAT",
                    model,
                    prompt == null ? null : prompt.getId(),
                    prompt == null ? "内置用例对话助手" : prompt.getName(),
                    prompt == null ? "V1.0" : prompt.getVersion(),
                    true,
                    null,
                    null,
                    elapsedMs(start)
            );
            AiDtos.CaseChatSessionResponse session = chatSessionService.appendTurn(
                    projectId,
                    currentUser,
                    normalizedSuiteType,
                    request.sessionId(),
                    message,
                    payload.reply()
            );
            return new AiDtos.CaseChatResponse(
                    session.sessionId(),
                    model.getId(),
                    model.getProvider(),
                    payload.reply(),
                    generatedCases
            );
        } catch (BusinessException exception) {
            aiCallAuditService.record(
                    projectId,
                    currentUser,
                    "TEST_CASE_CHAT",
                    model,
                    prompt == null ? null : prompt.getId(),
                    prompt == null ? "内置用例对话助手" : prompt.getName(),
                    prompt == null ? "V1.0" : prompt.getVersion(),
                    false,
                    exception.getCode(),
                    exception.getMessage(),
                    elapsedMs(start)
            );
            throw exception;
        }
    }

    private PromptTemplate findPrompt() {
        try {
            return aiModelService.findPrompt(null, "TEST_CASE_CHAT");
        } catch (BusinessException exception) {
            if (exception.getCode() == 40421) {
                return null;
            }
            throw exception;
        }
    }

    private String buildContext(Long projectId, Long requirementId, String suiteType) {
        StringBuilder builder = new StringBuilder();
        if (requirementId != null) {
            requirementRepository.findByIdAndProjectId(requirementId, projectId).ifPresent(requirement -> {
                builder.append("需求正文：\n")
                        .append(requirement.getContentText() == null ? "" : requirement.getContentText().trim())
                        .append("\n\n");
            });
        }
        String normalizedSuiteType = suiteType == null || suiteType.isBlank()
                ? AiGenerationService.SUITE_FUNCTIONAL
                : suiteType.trim().toUpperCase(Locale.ROOT);
        builder.append("当前项目最近测试用例（").append(AiGenerationService.SUITE_API.equals(normalizedSuiteType) ? "接口测试" : "功能测试").append("）：\n");
        List<TestCase> cases = testCaseRepository.findTop200ByProjectIdAndSuiteTypeOrderByCreatedAtDesc(projectId, normalizedSuiteType);
        if (cases.isEmpty()) {
            builder.append("暂无测试用例。\n");
            return builder.toString();
        }
        builder.append(cases.stream()
                .limit(20)
                .map(item -> String.format(Locale.ROOT,
                        "- %s | %s | %s | %s | %s",
                        item.getCaseNo(),
                        safe(item.getTitle()),
                        safe(item.getModuleName()),
                        safe(item.getPriority()),
                        safe(item.getStatus())))
                .collect(Collectors.joining("\n")));
        return builder.toString();
    }

    private String buildChatPrompt(String context, String message, String suiteType, Long selectedCaseId) {
        String suiteLabel = AiGenerationService.SUITE_API.equalsIgnoreCase(suiteType) ? "接口测试" : "功能测试";
        return """
                你是一名资深测试用例助手。请用简洁中文回答用户，并在需要时补充可直接入库的测试用例。
                你必须只返回 JSON，对象结构如下。除示例中的标准字段外，如果用户要求补充接口、设备、性能、权限等信息，可以增加同名字段，系统会原样保留：
                {
                  "reply": "给用户的中文回答",
                  "testCases": [
                    {
                      "caseNo": "可选，留空则由系统自动编号",
                      "title": "测试标题",
                      "moduleName": "功能模块",
                      "precondition": "前置条件",
                      "steps": "测试步骤",
                      "testData": "测试数据",
                      "expectedResult": "预期结果",
                      "priority": "P0|P1|P2|P3",
                      "caseType": "FUNCTIONAL|EXCEPTION|BOUNDARY",
                      "tags": "标签，多个用逗号分隔"
                    }
                  ]
                }
                如果当前问题不需要新增测试用例，testCases 传空数组。
                如果用户选中了已有用例并要求修改，返回的第一条 testCases 用例用于覆盖更新该用例，不要新增重复用例。
                当前选中的已有用例 ID：%s
                当前测试范围：%s
                当前项目上下文：
                %s
                用户问题：
                %s
                """.formatted(selectedCaseId == null ? "无" : selectedCaseId, suiteLabel, context, message);
    }

    private String mockReply(String message, String context, String suiteType) {
        String module = firstModule(context);
        String suiteLabel = AiGenerationService.SUITE_API.equalsIgnoreCase(suiteType) ? "接口测试" : "功能测试";
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            if (!hasGenerationIntent(message)) {
                payload.put("reply", "可以，我可以帮你分析需求、解释测试场景，或根据明确指令生成和优化测试用例。");
                payload.put("testCases", List.of());
                return objectMapper.writeValueAsString(payload);
            }
            payload.put("reply", "可以，我已经按" + suiteLabel + "场景给你整理了可入库版本。你可以先看这三类：正向、异常、边界。");
            List<java.util.Map<String, Object>> cases = new ArrayList<>();
            cases.add(buildMockCase("TC-AI-001", "核心流程正向场景", module, "测试环境和必要数据已准备完成", "1. 按需求输入有效数据\n2. 提交请求或操作", "有效业务数据", "操作成功，结果符合需求描述", "P0", "FUNCTIONAL", "AI,CHAT"));
            cases.add(buildMockCase("TC-AI-002", "非法数据异常场景", module, "测试环境可用", "1. 输入非法或缺失数据\n2. 提交请求或操作", "非法业务数据", "系统拒绝操作并返回明确提示", "P1", "EXCEPTION", "AI,CHAT"));
            cases.add(buildMockCase("TC-AI-003", "边界数据校验场景", module, "测试环境可用", "1. 输入边界值\n2. 提交请求或操作", "边界业务数据", "系统按照边界规则正确处理", "P1", "BOUNDARY", "AI,CHAT"));
            payload.put("testCases", cases);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new BusinessException(50050, "模拟 AI 对话失败");
        }
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

    private java.util.Map<String, Object> buildMockCase(
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
        java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
        item.put("caseNo", caseNo);
        item.put("title", title);
        item.put("moduleName", moduleName);
        item.put("precondition", precondition);
        item.put("steps", steps);
        item.put("testData", testData);
        item.put("expectedResult", expectedResult);
        item.put("priority", priority);
        item.put("caseType", caseType);
        item.put("tags", tags);
        return item;
    }

    private ChatPayload parseChatPayload(String rawJson, boolean strict) {
        String raw = rawJson == null ? "" : rawJson.trim();
        if (raw.isBlank()) {
            if (strict) {
                throw new BusinessException(50202, "AI 返回格式不正确，未生成测试用例");
            }
            return new ChatPayload("", List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            String reply = text(root, "reply", "message", "content", "answer");
            if (reply.isBlank()) {
                reply = root.isTextual() ? root.asText("").trim() : raw;
            }
            List<GeneratedCase> generatedCases = parseGeneratedCases(root.isArray() ? root : root.path("testCases"));
            return new ChatPayload(reply.isBlank() ? raw : reply, generatedCases);
        } catch (Exception exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            if (strict) {
                throw new BusinessException(50202, "AI 返回格式不正确，未生成测试用例");
            }
            return new ChatPayload(raw, List.of());
        }
    }

    private void validateRequirement(Long projectId, Long requirementId) {
        if (requirementId != null && requirementRepository.findByIdAndProjectId(requirementId, projectId).isEmpty()) {
            throw new BusinessException(40402, "项目文件不存在");
        }
    }

    private void validateSelectedCase(Long projectId, Long selectedCaseId, String suiteType) {
        if (selectedCaseId == null) {
            return;
        }
        TestCase testCase = testCaseRepository.findByIdAndProjectId(selectedCaseId, projectId)
                .orElseThrow(() -> new BusinessException(40403, "测试用例不存在"));
        if (!suiteType.equalsIgnoreCase(testCase.getSuiteType())) {
            throw new BusinessException(40006, "选中的测试用例不属于当前测试范围");
        }
    }

    private List<AiDtos.TestCaseView> saveGeneratedCases(
            Long projectId,
            Long requirementId,
            String suiteType,
            Long selectedCaseId,
            User currentUser,
            List<GeneratedCase> generatedCases
    ) {
        if (generatedCases.isEmpty()) {
            return List.of();
        }
        List<AiDtos.TestCaseView> savedCases = new ArrayList<>();
        Set<String> assignedCaseNos = new HashSet<>();
        for (int index = 0; index < generatedCases.size(); index++) {
            GeneratedCase generated = generatedCases.get(index);
            if (index == 0 && selectedCaseId != null) {
                TestCase selected = testCaseRepository.findByIdAndProjectId(selectedCaseId, projectId)
                        .orElseThrow(() -> new BusinessException(40403, "测试用例不存在"));
                selected.updateFrom(new com.testpilot.platform.testcase.TestCaseDtos.UpsertRequest(
                        requirementId,
                        selected.getCaseNo(),
                        generated.title(),
                        generated.moduleName(),
                        generated.precondition(),
                        generated.steps(),
                        generated.testData(),
                        generated.expectedResult(),
                        generated.priority(),
                        generated.caseType(),
                        suiteType,
                        generated.tags()
                ));
                selected.setExtraFieldsJson(generated.extraFieldsJson());
                selected.setStatus("DRAFT");
                TestCase saved = testCaseRepository.save(selected);
                savedCases.add(new AiDtos.TestCaseView(saved.getId(), saved.getCaseNo(), saved.getTitle(), saved.getStatus()));
                continue;
            }
            String caseNo = generated.caseNo();
            if (caseNo == null || caseNo.isBlank()
                    || assignedCaseNos.contains(caseNo)
                    || testCaseRepository.existsByProjectIdAndSuiteTypeAndCaseNo(projectId, suiteType, caseNo)) {
                caseNo = nextCaseNo(projectId, suiteType, assignedCaseNos);
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
            savedCases.add(new AiDtos.TestCaseView(saved.getId(), saved.getCaseNo(), saved.getTitle(), saved.getStatus()));
        }
        return savedCases;
    }

    private boolean hasGenerationIntent(String message) {
        String value = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return value.contains("生成")
                || value.contains("补充")
                || value.contains("新增")
                || value.contains("整理成用例")
                || value.contains("落库")
                || value.contains("场景")
                || value.contains("修改")
                || value.contains("优化")
                || value.contains("改成");
    }

    private List<GeneratedCase> parseGeneratedCases(JsonNode casesNode) {
        if (casesNode == null || !casesNode.isArray() || casesNode.isEmpty()) {
            return List.of();
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
                    text(item, "tags"),
                    extractExtraFieldsJson(item)
            ));
            if (result.size() >= 50) {
                break;
            }
        }
        return result;
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
            return "{}";
        }
    }

    private String requiredText(JsonNode node, String... names) {
        String value = text(node, names);
        if (value.isBlank()) {
            throw new BusinessException(50205, "AI 返回缺少必填字段: " + names[0]);
        }
        return value;
    }

    private String normalizePriority(String priority) {
        String value = priority == null ? "" : priority.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "P0", "HIGH", "CRITICAL", "BLOCKER", "URGENT", "高", "紧急", "严重" -> "P0";
            case "P1", "MEDIUM", "MAJOR", "中", "重要" -> "P1";
            case "P2", "NORMAL", "MINOR", "低", "一般" -> "P2";
            case "P3", "LOW", "TRIVIAL", "较低", "轻微" -> "P3";
            default -> "P2";
        };
    }

    private String normalizeType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "FUNCTIONAL", "POSITIVE", "NORMAL", "SUCCESS", "正向" -> "FUNCTIONAL";
            case "EXCEPTION", "NEGATIVE", "FAILURE", "ERROR", "异常", "反向" -> "EXCEPTION";
            case "BOUNDARY", "边界" -> "BOUNDARY";
            default -> "FUNCTIONAL";
        };
    }

    private String normalizeSuiteType(String suiteType) {
        String value = suiteType == null ? "" : suiteType.trim().toUpperCase(Locale.ROOT);
        return AiGenerationService.SUITE_API.equals(value) ? AiGenerationService.SUITE_API : AiGenerationService.SUITE_FUNCTIONAL;
    }

    private String nextCaseNo(Long projectId, String suiteType, Set<String> reservedCaseNos) {
        long next = testCaseRepository.countByProjectIdAndSuiteType(projectId, suiteType) + 1;
        String caseNo;
        do {
            caseNo = "TC-AI-" + String.format("%03d", next++);
        } while (reservedCaseNos.contains(caseNo)
                || testCaseRepository.existsByProjectIdAndSuiteTypeAndCaseNo(projectId, suiteType, caseNo));
        return caseNo;
    }

    private record ChatPayload(String reply, List<GeneratedCase> generatedCases) {
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

    private String safe(String value) {
        return value == null || value.isBlank() ? "--" : value.trim();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
