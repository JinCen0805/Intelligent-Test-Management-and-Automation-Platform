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
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import com.testpilot.platform.requirement.Requirement;
import com.testpilot.platform.requirement.RequirementParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiCaseAssistantService {

    private static final String BUILT_IN_SYSTEM_PROMPT = BuiltInPrompts.TEST_CASE_CHAT;
    private static final Set<String> STANDARD_CASE_FIELDS = Set.of(
            "caseno", "case_no", "用例编号", "title", "用例标题", "modulename", "module_name", "module", "模块", "所属模块", "precondition", "pre_condition", "前置条件",
            "steps", "步骤", "测试步骤", "testdata", "test_data", "测试数据", "expectedresult", "expected_result", "预期", "预期结果",
            "priority", "优先级", "casetype", "case_type", "type", "类型", "测试类型", "suitetype", "suite_type", "套件类型", "tags", "标签"
    );

    private final AiModelService aiModelService;
    private final ProjectAccessService projectAccessService;
    private final RequirementRepository requirementRepository;
    private final TestCaseRepository testCaseRepository;
    private final ObjectMapper objectMapper;
    private final AiProviderClient aiProviderClient;
    private final AiCallAuditService aiCallAuditService;
    private final AiCaseChatSessionService chatSessionService;
    private final RequirementParser requirementParser;
    private final TransactionTemplate transactionTemplate;
    private final int maxContextChars;

    public AiCaseAssistantService(
            AiModelService aiModelService,
            ProjectAccessService projectAccessService,
            RequirementRepository requirementRepository,
            TestCaseRepository testCaseRepository,
            ObjectMapper objectMapper,
            AiProviderClient aiProviderClient,
            AiCallAuditService aiCallAuditService,
            AiCaseChatSessionService chatSessionService,
            RequirementParser requirementParser,
            TransactionTemplate transactionTemplate,
            @org.springframework.beans.factory.annotation.Value("${testpilot.ai.max-context-chars:20000}")
            int maxContextChars
    ) {
        this.aiModelService = aiModelService;
        this.projectAccessService = projectAccessService;
        this.requirementRepository = requirementRepository;
        this.testCaseRepository = testCaseRepository;
        this.objectMapper = objectMapper;
        this.aiProviderClient = aiProviderClient;
        this.aiCallAuditService = aiCallAuditService;
        this.chatSessionService = chatSessionService;
        this.requirementParser = requirementParser;
        this.transactionTemplate = transactionTemplate;
        this.maxContextChars = maxContextChars > 0 ? maxContextChars : AiTextLimits.DEFAULT_MAX_CONTEXT_CHARS;
    }

    public AiDtos.CaseChatResponse chat(Long projectId, AiDtos.CaseChatRequest request, User currentUser) {
        return chatInternal(projectId, request, currentUser, null, null);
    }

    public AiDtos.CaseChatResponse chatWithFile(Long projectId, AiDtos.CaseChatRequest request, MultipartFile file, User currentUser) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40003, "请选择要添加到对话的文件");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(40002, "文件大小不能超过 10MB");
        }
        String message = request == null ? null : request.message();
        if (message != null && message.length() > 2000) {
            throw new BusinessException(40008, "对话内容长度不能超过 2000 个字符");
        }
        String originalName;
        try {
            String rawName = file.getOriginalFilename();
            originalName = rawName == null || rawName.isBlank()
                    ? "对话附件"
                    : Path.of(rawName).getFileName().toString();
        } catch (RuntimeException exception) {
            originalName = "对话附件";
        }
        String extension = extensionOf(originalName);
        try (var inputStream = file.getInputStream()) {
            String content = requirementParser.parse(extension, inputStream);
            if (content == null || content.isBlank()) {
                throw new BusinessException(40004, "无法读取该文件内容，请更换文件后重试");
            }
            return chatInternal(projectId, request, currentUser,
                    AiTextLimits.truncate(content, maxContextChars), originalName);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(40004, "无法读取该文件内容，请确认文件格式正确");
        }
    }

    private AiDtos.CaseChatResponse chatInternal(Long projectId, AiDtos.CaseChatRequest request, User currentUser, String transientFileContent, String transientFileName) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AI_GENERATE);
        AiModel model = aiModelService.findModel(request.modelId());
        PromptTemplate prompt = findPrompt();
        String promptContent = prompt == null ? BUILT_IN_SYSTEM_PROMPT : prompt.getContent();
        String normalizedSuiteType = normalizeSuiteType(request.suiteType());
        validateRequirement(projectId, request.requirementId());
        validateSelectedCase(projectId, request.selectedCaseId(), normalizedSuiteType);
        String context = buildContext(projectId, request.requirementId(), normalizedSuiteType, transientFileContent, transientFileName);
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
            // AI 调用结束后，用例落库 + 会话消息写库放在同一个短事务里。
            AiDtos.CaseChatResponse response = transactionTemplate.execute(status -> {
                List<AiDtos.TestCaseView> generatedCases = saveGeneratedCases(
                        projectId,
                        request.requirementId(),
                        normalizedSuiteType,
                        request.selectedCaseId(),
                        currentUser,
                        payload.generatedCases()
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
            });
            aiCallAuditService.record(
                    projectId,
                    currentUser,
                    "TEST_CASE_CHAT",
                    model,
                    prompt == null ? null : prompt.getId(),
                    prompt == null ? "内置用例对话助手" : prompt.getName(),
                    prompt == null ? "V3.1" : prompt.getVersion(),
                    true,
                    null,
                    null,
                    elapsedMs(start)
            );
            return response;
        } catch (BusinessException exception) {
            aiCallAuditService.record(
                    projectId,
                    currentUser,
                    "TEST_CASE_CHAT",
                    model,
                    prompt == null ? null : prompt.getId(),
                    prompt == null ? "内置用例对话助手" : prompt.getName(),
                    prompt == null ? "V3.1" : prompt.getVersion(),
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

    private String buildContext(Long projectId, Long requirementId, String suiteType, String transientFileContent, String transientFileName) {
        StringBuilder builder = new StringBuilder();
        if (transientFileContent != null && !transientFileContent.isBlank()) {
            builder.append("本次对话临时附件").append(transientFileName == null ? "" : "（" + transientFileName + "）").append("：\n")
                    .append(transientFileContent.trim()).append("\n\n");
        } else if (requirementId != null) {
            requirementRepository.findByIdAndProjectId(requirementId, projectId).ifPresent(requirement -> {
                builder.append("需求正文：\n")
                        .append(readRequirementContent(requirement))
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

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new BusinessException(40004, "无法识别文件类型");
        }
        return fileName.substring(dot + 1).toUpperCase(Locale.ROOT);
    }

    private String readRequirementContent(Requirement requirement) {
        if (requirement.getContentText() != null && !requirement.getContentText().isBlank()) {
            return AiTextLimits.truncate(requirement.getContentText().trim(), maxContextChars);
        }
        if (requirement.getFilePath() == null || requirement.getFilePath().isBlank()) {
            return "（项目文件暂无可读取正文）";
        }
        try (var inputStream = Files.newInputStream(Path.of(requirement.getFilePath()))) {
            String content = requirementParser.parse(requirement.getFileType(), inputStream);
            return content == null || content.isBlank()
                    ? "（项目文件暂无可读取正文）"
                    : AiTextLimits.truncate(content.trim(), maxContextChars);
        } catch (IOException | RuntimeException exception) {
            return "（项目文件暂不支持读取正文）";
        }
    }

    private String buildChatPrompt(String context, String message, String suiteType, Long selectedCaseId) {
        String suiteLabel = AiGenerationService.SUITE_API.equalsIgnoreCase(suiteType) ? "接口测试" : "功能测试";
        return "如果用户选中了已有用例并要求修改，返回的第一条 testCases 用例用于覆盖更新该用例，不要新增重复用例。"
                + "\n当前选中的已有用例 ID：" + (selectedCaseId == null ? "无" : selectedCaseId)
                + "\n当前测试范围：" + suiteLabel
                + "\n当前项目上下文：\n" + context
                + "\n用户最新需求（最高业务优先级）：\n" + message;
    }

    private String mockReply(String message, String context, String suiteType) {
        String module = firstModule(context);
        String suiteLabel = AiGenerationService.SUITE_API.equalsIgnoreCase(suiteType) ? "接口测试" : "功能测试";
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            if (!hasGenerationIntent(message)) {
                payload.put("reply", "可以，我可以帮你分析需求、解释测试场景，或根据明确指令生成和优化测试用例。");
                payload.put("testCases", List.of());
                payload.put("pendingItems", List.of());
                return objectMapper.writeValueAsString(payload);
            }
            payload.put("reply", "可以，我已经按" + suiteLabel + "场景整理了可入库版本，覆盖主流程、字段校验和边界规则。");
            List<java.util.Map<String, Object>> cases = new ArrayList<>();
            cases.add(buildMockCase("TC-AI-001", "验证" + module + "核心业务流程处理正确", module,
                    "1. 测试账号已登录并具备当前模块操作权限\n2. 测试数据处于可提交状态",
                    "1. 进入" + module + "页面\n2. 输入有效业务数据\n3. 点击提交或保存",
                    "1. 页面展示当前模块及可操作控件\n2. 系统接受有效数据并完成提交\n3. 页面显示成功反馈，数据状态更新且列表可查询",
                    "P0", "FUNCTIONAL"));
            cases.add(buildMockCase("TC-AI-002", "验证" + module + "必填字段校验有效", module,
                    "1. 测试账号已登录并进入当前模块页面\n2. 当前页面存在可提交表单",
                    "1. 保持必填字段为空\n2. 填写其他必要字段\n3. 点击提交或保存",
                    "1. 系统阻止提交\n2. 空值字段被定位或标记\n3. 页面展示与该字段对应的校验提示，已有数据不被错误保存",
                    "P1", "EXCEPTION"));
            cases.add(buildMockCase("TC-AI-003", "验证" + module + "边界数据处理符合规则", module,
                    "1. 测试账号已登录并进入当前模块页面\n2. 已明确待验证字段的有效边界值",
                    "1. 输入边界值及其他有效数据\n2. 点击提交或保存",
                    "1. 边界内数据按业务规则处理\n2. 边界外数据被阻止并展示明确校验结果\n3. 系统不产生部分保存或错误状态",
                    "P1", "BOUNDARY"));
            payload.put("testCases", cases);
            payload.put("pendingItems", List.of("演示模型不会读取业务规则，已使用协议示例数据；配置真实 AI 模型后将按内置 Skill Prompt 生成"));
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
            String expectedResult,
            String priority,
            String caseType
    ) {
        java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
        item.put("caseNo", caseNo);
        item.put("title", title);
        item.put("moduleName", moduleName);
        item.put("precondition", precondition);
        item.put("steps", steps);
        item.put("expectedResult", expectedResult);
        item.put("priority", priority);
        item.put("caseType", caseType);
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
            GeneratedCase generated = limitCase(generatedCases.get(index));
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
            savedCases.add(new AiDtos.TestCaseView(saved.getId(), saved.getCaseNo(), saved.getTitle(), saved.getStatus()));
        }
        return savedCases;
    }

    private GeneratedCase limitCase(GeneratedCase generated) {
        return new GeneratedCase(
                AiTextLimits.truncateOrEmpty(generated.caseNo(), AiTextLimits.MAX_CASE_NO_CHARS),
                AiTextLimits.truncateOrEmpty(generated.title(), AiTextLimits.MAX_TITLE_CHARS),
                AiTextLimits.truncateOrEmpty(generated.moduleName(), AiTextLimits.MAX_MODULE_CHARS),
                AiTextLimits.truncate(generated.precondition(), AiTextLimits.MAX_TEXT_FIELD_CHARS),
                AiTextLimits.truncate(generated.steps(), AiTextLimits.MAX_TEXT_FIELD_CHARS),
                AiTextLimits.truncate(generated.testData(), AiTextLimits.MAX_TEXT_FIELD_CHARS),
                AiTextLimits.truncate(generated.expectedResult(), AiTextLimits.MAX_TEXT_FIELD_CHARS),
                generated.priority(),
                generated.caseType(),
                AiTextLimits.truncateOrEmpty(generated.tags(), AiTextLimits.MAX_TAGS_CHARS),
                AiTextLimits.truncate(generated.extraFieldsJson(), AiTextLimits.MAX_EXTRA_FIELDS_JSON_CHARS)
        );
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
