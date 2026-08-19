package com.testpilot.platform.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.automation.AutomationDtos;
import com.testpilot.platform.automation.AutomationService;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.user.User;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAutomationTaskService {

    private static final String DISCLAIMER =
            "AI 生成配置草稿，仅供参考，请在确认前人工复核。AI 不会修改 Git 代码、提交代码或创建 PR。";
    private static final String BUILT_IN_PROMPT =
            "你是一名资深测试架构师，请根据 Git 仓库、Branch 和自然语言描述生成自动化测试任务配置。"
                    + "仅输出 JSON，不要 Markdown 代码块。JSON 字段：name（任务名称）、taskType（SMOKE/REGRESSION）、"
                    + "automationType（API/UI）、framework（PYTEST/PLAYWRIGHT/SELENIUM）、testPath、executeCommand、"
                    + "environment（TEST/STAGING/PROD）、parametersJson（JSON 字符串）、pipelineType（PIPELINE/FREESTYLE）、"
                    + "jenkinsJob。";
    private static final Pattern TEST_PATH_PATTERN = Pattern.compile("tests/[A-Za-z0-9_\\-/.]+");

    private final AiModelService aiModelService;
    private final AiAutomationTaskDraftRepository draftRepository;
    private final ProjectAccessService projectAccessService;
    private final AutomationService automationService;
    private final ObjectMapper objectMapper;
    private final AiProviderClient aiProviderClient;
    private final AiCallAuditService aiCallAuditService;

    public AiAutomationTaskService(
            AiModelService aiModelService,
            AiAutomationTaskDraftRepository draftRepository,
            ProjectAccessService projectAccessService,
            AutomationService automationService,
            ObjectMapper objectMapper,
            AiProviderClient aiProviderClient,
            AiCallAuditService aiCallAuditService
    ) {
        this.aiModelService = aiModelService;
        this.draftRepository = draftRepository;
        this.projectAccessService = projectAccessService;
        this.automationService = automationService;
        this.objectMapper = objectMapper;
        this.aiProviderClient = aiProviderClient;
        this.aiCallAuditService = aiCallAuditService;
    }

    @Transactional
    public AiAutomationDtos.DraftResponse generate(
            Long projectId,
            AiAutomationDtos.GenerateRequest request,
            User currentUser
    ) {
        projectAccessService.requireWrite(projectId, currentUser);
        String repositoryUrl = request.repositoryUrl().trim();
        String branch = request.branch().trim();
        String description = request.description().trim();

        AiModel model = aiModelService.findModel(request.modelId());
        PromptTemplate prompt = findPrompt(request.promptId());

        long start = System.nanoTime();
        InferredConfig config;
        try {
            config = "MOCK".equalsIgnoreCase(model.getProvider())
                    ? inferByKeywords(description, repositoryUrl, branch)
                    : inferFromModel(model, prompt, description, repositoryUrl, branch);
            aiCallAuditService.record(projectId, currentUser, "AUTOMATION_TASK_GENERATION", model,
                    prompt == null ? null : prompt.getId(),
                    prompt == null ? "内置自动化任务生成模板" : prompt.getName(),
                    prompt == null ? "V1.0" : prompt.getVersion(),
                    true, null, null, elapsedMs(start));
        } catch (BusinessException exception) {
            aiCallAuditService.record(projectId, currentUser, "AUTOMATION_TASK_GENERATION", model,
                    prompt == null ? null : prompt.getId(),
                    prompt == null ? "内置自动化任务生成模板" : prompt.getName(),
                    prompt == null ? "V1.0" : prompt.getVersion(),
                    false, exception.getCode(), exception.getMessage(), elapsedMs(start));
            throw exception;
        }

        String missingItemsJson = toJson(buildMissingItems(config));
        String riskHintsJson = toJson(buildRiskHints());
        AiAutomationTaskDraft draft = draftRepository.save(new AiAutomationTaskDraft(
                projectId,
                config.name(),
                config.taskType(),
                config.automationType(),
                config.repositoryUrl(),
                config.branch(),
                config.framework(),
                blankToNull(config.testPath()),
                blankToNull(config.executeCommand()),
                config.environment(),
                blankToNull(config.parametersJson()),
                config.pipelineType(),
                config.allureEnabled(),
                blankToNull(config.jenkinsJob()),
                missingItemsJson,
                riskHintsJson,
                model.getName(),
                model.getProvider(),
                prompt == null ? "内置自动化任务生成模板" : prompt.getName(),
                prompt == null ? "V1.0" : prompt.getVersion(),
                currentUser.getId()
        ));
        return toResponse(draft);
    }

    public AiAutomationDtos.DraftResponse get(Long projectId, Long draftId, User currentUser) {
        projectAccessService.requireRead(projectId, currentUser);
        return toResponse(requireDraft(projectId, draftId));
    }

    @Transactional
    public AiAutomationDtos.DraftResponse update(
            Long projectId,
            Long draftId,
            AiAutomationDtos.DraftUpdateRequest request,
            User currentUser
    ) {
        projectAccessService.requireWrite(projectId, currentUser);
        AiAutomationTaskDraft draft = requireDraft(projectId, draftId);
        if ("CONFIRMED".equals(draft.getStatus())) {
            throw new BusinessException(40023, "草稿已确认，无法修改或重复确认");
        }
        draft.update(
                request.name().trim(),
                normalizeInSet(request.taskType(), List.of("SMOKE", "REGRESSION"), "REGRESSION", "任务类型"),
                normalizeInSet(request.automationType(), List.of("API", "UI"), "API", "自动化类型"),
                request.repositoryUrl().trim(),
                request.branch().trim(),
                normalizeInSet(request.framework(), List.of("PYTEST", "PLAYWRIGHT", "SELENIUM"), "PYTEST", "Framework"),
                blankToNull(request.testPath()),
                blankToNull(request.executeCommand()),
                normalizeInSet(request.environment(), List.of("TEST", "STAGING", "PROD"), "TEST", "执行环境"),
                blankToNull(request.parametersJson()),
                normalizeInSet(request.pipelineType(), List.of("PIPELINE", "FREESTYLE"), "FREESTYLE", "Pipeline 类型"),
                request.allureEnabled(),
                blankToNull(request.jenkinsJob())
        );
        return toResponse(draftRepository.save(draft));
    }

    @Transactional
    public AiAutomationDtos.ConfirmResponse confirm(Long projectId, Long draftId, User currentUser) {
        projectAccessService.requireWrite(projectId, currentUser);
        AiAutomationTaskDraft draft = requireDraft(projectId, draftId);
        if ("CONFIRMED".equals(draft.getStatus())) {
            throw new BusinessException(40023, "草稿已确认，无法修改或重复确认");
        }
        AutomationDtos.AutomationProjectResponse automationProject = automationService.createAutomationProject(
                projectId,
                new AutomationDtos.AutomationProjectRequest(
                        draft.getName(),
                        draft.getAutomationType(),
                        draft.getRepositoryUrl(),
                        draft.getBranch(),
                        null,
                        draft.getFramework(),
                        draft.getTestPath(),
                        draft.getExecuteCommand(),
                        draft.getEnvironment(),
                        draft.getJenkinsJob()
                ),
                currentUser
        );
        AutomationDtos.TestTaskResponse task = automationService.createTask(
                projectId,
                new AutomationDtos.TestTaskRequest(
                        draft.getName(),
                        draft.getTaskType(),
                        automationProject.id(),
                        null,
                        draft.getParametersJson()
                ),
                currentUser
        );
        draft.markConfirmed();
        draftRepository.save(draft);
        return new AiAutomationDtos.ConfirmResponse(
                draft.getId(),
                automationProject.id(),
                task.id(),
                task,
                DISCLAIMER
        );
    }

    private AiAutomationDtos.DraftResponse toResponse(AiAutomationTaskDraft draft) {
        return AiAutomationDtos.DraftResponse.from(
                draft,
                draft.isAllureEnabled() ? allureConfig() : null,
                fromJson(draft.getMissingItemsJson()),
                fromJson(draft.getRiskHintsJson()),
                DISCLAIMER
        );
    }

    private AiAutomationTaskDraft requireDraft(Long projectId, Long draftId) {
        return draftRepository.findByIdAndProjectId(draftId, projectId)
                .orElseThrow(() -> new BusinessException(40423, "AI 自动化任务草稿不存在"));
    }

    private PromptTemplate findPrompt(Long promptId) {
        try {
            return aiModelService.findPrompt(promptId, "AUTOMATION_TASK_GENERATION");
        } catch (BusinessException exception) {
            if (exception.getCode() == 40421 && promptId == null) {
                return null;
            }
            throw exception;
        }
    }

    private InferredConfig inferByKeywords(String description, String repositoryUrl, String branch) {
        String text = description == null ? "" : description.toLowerCase(Locale.ROOT);
        String automationType = inferAutomationType(text);
        String framework = inferFramework(text);
        String taskType = inferTaskType(text);
        String environment = inferEnvironment(description);
        String testPath = inferTestPath(description);
        String executeCommand = buildCommand(framework, testPath);
        String pipelineType = inferPipelineType(text);
        String jenkinsJob = inferJob(repositoryUrl, taskType);
        String name = inferName(repositoryUrl, taskType);
        String parametersJson = buildParameters(testPath, environment);
        return new InferredConfig(
                name,
                taskType,
                automationType,
                repositoryUrl,
                branch,
                framework,
                testPath,
                executeCommand,
                environment,
                parametersJson,
                pipelineType,
                true,
                jenkinsJob
        );
    }

    private InferredConfig inferFromModel(
            AiModel model,
            PromptTemplate prompt,
            String description,
            String repositoryUrl,
            String branch
    ) {
        InferredConfig baseline = inferByKeywords(description, repositoryUrl, branch);
        String rawJson = callModel(model, prompt == null ? BUILT_IN_PROMPT : prompt.getContent(),
                description, repositoryUrl, branch);
        JsonNode root;
        try {
            root = objectMapper.readTree(stripCodeFence(rawJson));
        } catch (JsonProcessingException exception) {
            return baseline;
        }
        return new InferredConfig(
                pickText(root, "name", baseline.name()),
                normalizeInSet(text(root, "taskType"), List.of("SMOKE", "REGRESSION"), baseline.taskType(), null),
                normalizeInSet(text(root, "automationType"), List.of("API", "UI"), baseline.automationType(), null),
                textOr(root, "repositoryUrl", baseline.repositoryUrl()),
                textOr(root, "branch", baseline.branch()),
                normalizeInSet(text(root, "framework"), List.of("PYTEST", "PLAYWRIGHT", "SELENIUM"),
                        baseline.framework(), null),
                textOr(root, "testPath", baseline.testPath()),
                textOr(root, "executeCommand", baseline.executeCommand()),
                normalizeInSet(text(root, "environment"), List.of("TEST", "STAGING", "PROD"),
                        baseline.environment(), null),
                textOr(root, "parametersJson", baseline.parametersJson()),
                normalizeInSet(text(root, "pipelineType"), List.of("PIPELINE", "FREESTYLE"),
                        baseline.pipelineType(), null),
                true,
                textOr(root, "jenkinsJob", baseline.jenkinsJob())
        );
    }

    private String callModel(AiModel model, String promptContent, String description,
                             String repositoryUrl, String branch) {
        String userPrompt = promptContent
                + "\n\nGit 仓库：" + repositoryUrl
                + "\nBranch：" + branch
                + "\n自然语言描述：" + description;
        return aiProviderClient.completeJson(model, userPrompt);
    }

    private String inferAutomationType(String text) {
        if (containsAny(text, "playwright", "selenium", "ui", "页面", "浏览器", "端到端", "e2e")) {
            return "UI";
        }
        return "API";
    }

    private String inferFramework(String text) {
        if (text.contains("selenium")) {
            return "SELENIUM";
        }
        if (text.contains("playwright")) {
            return "PLAYWRIGHT";
        }
        return "PYTEST";
    }

    private String inferTaskType(String text) {
        if (containsAny(text, "smoke", "冒烟")) {
            return "SMOKE";
        }
        return "REGRESSION";
    }

    private String inferEnvironment(String description) {
        String text = description == null ? "" : description.toLowerCase(Locale.ROOT);
        if (containsAny(text, "prod", "生产")) {
            return "PROD";
        }
        if (containsAny(text, "staging", "预发", "预发布")) {
            return "STAGING";
        }
        return "TEST";
    }

    private String inferTestPath(String description) {
        if (description == null) {
            return "tests";
        }
        Matcher matcher = TEST_PATH_PATTERN.matcher(description);
        if (matcher.find()) {
            String path = matcher.group().replaceAll("[.,，。]+$", "");
            return path.isBlank() ? "tests" : path;
        }
        return "tests";
    }

    private String buildCommand(String framework, String testPath) {
        if ("PLAYWRIGHT".equals(framework)) {
            return "npx playwright test " + testPath;
        }
        return "pytest " + testPath + " --alluredir=allure-results";
    }

    private String inferPipelineType(String text) {
        if (containsAny(text, "pipeline", "流水线")) {
            return "PIPELINE";
        }
        return "FREESTYLE";
    }

    private String inferJob(String repositoryUrl, String taskType) {
        String base = repoBase(repositoryUrl);
        return base + "-" + taskType.toLowerCase(Locale.ROOT);
    }

    private String inferName(String repositoryUrl, String taskType) {
        String base = repoBase(repositoryUrl);
        return base + ("SMOKE".equals(taskType) ? " 冒烟任务" : " 回归任务");
    }

    private String repoBase(String repositoryUrl) {
        String value = repositoryUrl == null ? "" : repositoryUrl.trim();
        String base = value.replaceAll("(?i)\\.git$", "");
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0 && slash < base.length() - 1) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^A-Za-z0-9_\\-]", "-");
        return base.isBlank() ? "ai-automation" : base;
    }

    private String buildParameters(String testPath, String environment) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("testPath", testPath);
        params.put("environment", environment);
        params.put("alluredir", "allure-results");
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(50220, "AI 任务配置序列化失败");
        }
    }

    private List<String> buildMissingItems(InferredConfig config) {
        List<String> items = new ArrayList<>();
        items.add("未提供 Jenkins 实例与 Pipeline Binding，确认后仅创建任务，执行前需绑定 Jenkins 实例。");
        items.add("未提供 Git 凭证（credentialRef），若仓库为私有请在确认前补充授权。");
        items.add("推断的测试路径为 " + config.testPath() + "，请确认与实际仓库目录一致。");
        if (config.jenkinsJob() != null && !config.jenkinsJob().isBlank()) {
            items.add("已根据仓库名推断 Jenkins Job 为 " + config.jenkinsJob() + "，请确认是否与实际 Job 一致。");
        }
        return items;
    }

    private List<String> buildRiskHints() {
        return List.of(
                "AI 仅生成配置草稿，不会修改 Git 代码、提交代码或创建 PR。",
                "推断结果可能不准确，请人工复核框架、执行命令与环境。",
                "Allure 报告依赖仓库中已正确集成 Allure 插件。"
        );
    }

    private String allureConfig() {
        return "{\"enabled\":true,\"reportDir\":\"allure-results\",\"archive\":\"allure-report.zip\"}";
    }

    private String normalizeInSet(String value, List<String> allowed, String fallback, String label) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (allowed.contains(normalized)) {
            return normalized;
        }
        if (label == null) {
            return fallback;
        }
        throw new BusinessException(40024, label + "不合法");
    }

    private String pickText(JsonNode root, String name, String fallback) {
        String value = text(root, name);
        return value.isBlank() ? fallback : value;
    }

    private String textOr(JsonNode root, String name, String fallback) {
        String value = text(root, name);
        return value.isBlank() ? fallback : value;
    }

    private String text(JsonNode root, String name) {
        JsonNode node = root.path(name);
        return node.isMissingNode() || node.isNull() ? "" : node.asText("").trim();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String stripCodeFence(String rawJson) {
        String value = rawJson.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        return value;
    }

    private String toJson(List<String> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(50220, "AI 任务配置序列化失败");
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> items = new ArrayList<>();
            node.forEach(item -> {
                String text = item.asText("").trim();
                if (!text.isBlank()) {
                    items.add(text);
                }
            });
            return items;
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record InferredConfig(
            String name,
            String taskType,
            String automationType,
            String repositoryUrl,
            String branch,
            String framework,
            String testPath,
            String executeCommand,
            String environment,
            String parametersJson,
            String pipelineType,
            boolean allureEnabled,
            String jenkinsJob
    ) {
    }
}
