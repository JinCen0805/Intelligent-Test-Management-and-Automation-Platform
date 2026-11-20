package com.testpilot.platform.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.automation.AutomationProject;
import com.testpilot.platform.automation.AutomationProjectRepository;
import com.testpilot.platform.automation.JenkinsAdminService;
import com.testpilot.platform.automation.JenkinsClient;
import com.testpilot.platform.automation.JenkinsPipelineBinding;
import com.testpilot.platform.automation.JenkinsPipelineBindingRepository;
import com.testpilot.platform.automation.TestExecution;
import com.testpilot.platform.automation.TestExecutionRepository;
import com.testpilot.platform.automation.TestReport;
import com.testpilot.platform.automation.TestReportRepository;
import com.testpilot.platform.automation.TestTask;
import com.testpilot.platform.automation.TestTaskRepository;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.common.SensitiveDataSanitizer;
import com.testpilot.platform.project.Project;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.user.User;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AiFailureAnalysisService {

    private static final String DISCLAIMER =
            "AI 分析结果，仅供参考，请结合 Jenkins 日志与 Allure 报告人工复核。";
    private static final String BUILT_IN_PROMPT =
            "你是一名资深测试开发工程师，请根据自动化测试失败信息进行根因分析。仅输出 JSON，不要 Markdown 代码块。"
                    + "JSON 结构必须包含：category（取值 ENVIRONMENT/TEST_DATA/API_CHANGE/TEST_CODE/NETWORK/UNKNOWN 之一）、"
                    + "possibleCauses、relatedErrors、checkLocations、fixDirections、riskWarnings，"
                    + "其中后五项为字符串数组。";
    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "ENVIRONMENT", "环境问题",
            "TEST_DATA", "测试数据问题",
            "API_CHANGE", "接口或页面变更",
            "TEST_CODE", "测试代码问题",
            "NETWORK", "网络问题",
            "UNKNOWN", "未知问题"
    );

    private final AiModelService aiModelService;
    private final AiAnalysisRecordRepository recordRepository;
    private final TestExecutionRepository executionRepository;
    private final TestTaskRepository testTaskRepository;
    private final AutomationProjectRepository automationProjectRepository;
    private final TestReportRepository testReportRepository;
    private final ProjectAccessService projectAccessService;
    private final JenkinsClient jenkinsClient;
    private final JenkinsPipelineBindingRepository bindingRepository;
    private final JenkinsAdminService jenkinsAdminService;
    private final ObjectMapper objectMapper;
    private final AiProviderClient aiProviderClient;
    private final AiCallAuditService aiCallAuditService;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;
    private final TransactionTemplate transactionTemplate;

    public AiFailureAnalysisService(
            AiModelService aiModelService,
            AiAnalysisRecordRepository recordRepository,
            TestExecutionRepository executionRepository,
            TestTaskRepository testTaskRepository,
            AutomationProjectRepository automationProjectRepository,
            TestReportRepository testReportRepository,
            ProjectAccessService projectAccessService,
            JenkinsClient jenkinsClient,
            JenkinsPipelineBindingRepository bindingRepository,
            JenkinsAdminService jenkinsAdminService,
            ObjectMapper objectMapper,
            AiProviderClient aiProviderClient,
            AiCallAuditService aiCallAuditService,
            SensitiveDataSanitizer sensitiveDataSanitizer,
            TransactionTemplate transactionTemplate
    ) {
        this.aiModelService = aiModelService;
        this.recordRepository = recordRepository;
        this.executionRepository = executionRepository;
        this.testTaskRepository = testTaskRepository;
        this.automationProjectRepository = automationProjectRepository;
        this.testReportRepository = testReportRepository;
        this.projectAccessService = projectAccessService;
        this.jenkinsClient = jenkinsClient;
        this.bindingRepository = bindingRepository;
        this.jenkinsAdminService = jenkinsAdminService;
        this.objectMapper = objectMapper;
        this.aiProviderClient = aiProviderClient;
        this.aiCallAuditService = aiCallAuditService;
        this.sensitiveDataSanitizer = sensitiveDataSanitizer;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Jenkins 日志读取与 AI 调用不占用数据库事务，只有分析结果落库阶段使用短事务。
     */
    public AiAnalysisDtos.AnalysisResponse analyze(
            Long projectId,
            Long executionId,
            AiAnalysisDtos.AnalyzeRequest request,
            User currentUser
    ) {
        Project project = projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AI_ANALYZE);
        TestExecution execution = executionRepository.findByIdAndProjectId(executionId, projectId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        if (!List.of("FAILED", "ABORTED", "TIMEOUT").contains(execution.getStatus())) {
            throw new BusinessException(40018, "只有失败、中止或超时的执行可以发起 AI 分析");
        }
        TestTask task = testTaskRepository.findByIdAndProjectId(execution.getTestTaskId(), projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        AutomationProject automationProject = automationProjectRepository
                .findByIdAndProjectId(execution.getAutomationProjectId(), projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        TestReport report = testReportRepository.findByExecutionId(executionId).orElse(null);

        AiModel model = aiModelService.findModel(request == null ? null : request.modelId());
        PromptTemplate prompt = findPrompt(request == null ? null : request.promptId());
        String promptName = prompt == null ? "内置失败分析模板" : prompt.getName();
        String promptVersion = prompt == null ? "V1.0" : prompt.getVersion();

        String consoleLog = sensitiveDataSanitizer.sanitize(fetchConsoleLog(execution, task, projectId));
        String failureDetail = report == null ? null : sensitiveDataSanitizer.sanitize(report.getFailureDetail());
        String context = buildContext(project, task, automationProject, execution, report, consoleLog, failureDetail);

        long start = System.nanoTime();
        try {
            String resultJson = "MOCK".equalsIgnoreCase(model.getProvider())
                    ? mockAnalysis(consoleLog, failureDetail)
                    : callModelWithRetry(model, prompt == null ? BUILT_IN_PROMPT : prompt.getContent(), context);
            AnalysisResult result = parseResult(resultJson, consoleLog, failureDetail);
            AiAnalysisRecord record = transactionTemplate.execute(status -> {
                clearExisting(executionId);
                AiAnalysisRecord saved = recordRepository.save(new AiAnalysisRecord(
                        executionId,
                        projectId,
                        task.getId(),
                        model.getId(),
                        model.getName(),
                        model.getProvider(),
                        prompt == null ? null : prompt.getId(),
                        promptName,
                        promptVersion,
                        "SUCCESS",
                        result.category(),
                        serializeBody(result),
                        null,
                        currentUser.getId()
                ));
                if (report != null) {
                    report.setAiAnalysis(buildSummary(result));
                    testReportRepository.save(report);
                }
                return saved;
            });
            aiCallAuditService.record(projectId, currentUser, "FAILURE_ANALYSIS", model,
                    prompt == null ? null : prompt.getId(), promptName, promptVersion,
                    true, null, null, elapsedMs(start));
            return toResponse(project, task, automationProject, execution, record, result, model, promptName, promptVersion, null);
        } catch (BusinessException exception) {
            aiCallAuditService.record(projectId, currentUser, "FAILURE_ANALYSIS", model,
                    prompt == null ? null : prompt.getId(), promptName, promptVersion,
                    false, exception.getCode(), exception.getMessage(), elapsedMs(start));
            AiAnalysisRecord failed = transactionTemplate.execute(status -> saveFailedRecord(
                    executionId, projectId, task.getId(), model, promptName, promptVersion,
                    currentUser.getId(), exception.getMessage()
            ));
            return toResponse(project, task, automationProject, execution, failed, emptyResult(), model,
                    promptName, promptVersion, exception.getMessage());
        }
    }

    private AiAnalysisDtos.AnalysisResponse toResponse(
            Project project,
            TestTask task,
            AutomationProject automationProject,
            TestExecution execution,
            AiAnalysisRecord record,
            AnalysisResult result,
            AiModel model,
            String promptName,
            String promptVersion,
            String errorMessage
    ) {
        return new AiAnalysisDtos.AnalysisResponse(
                execution.getId(),
                project.getId(),
                project.getName(),
                task.getId(),
                task.getName(),
                execution.getBuildNumber(),
                automationProject.getBranch(),
                execution.getCommitSha(),
                automationProject.getEnvironment(),
                record.getStatus(),
                result.category(),
                CATEGORY_LABELS.getOrDefault(result.category(), result.category()),
                result.possibleCauses(),
                result.relatedErrors(),
                result.checkLocations(),
                result.fixDirections(),
                result.riskWarnings(),
                model.getName(),
                model.getProvider(),
                promptName,
                promptVersion,
                record.getCreatedAt(),
                DISCLAIMER,
                errorMessage
        );
    }

    private AnalysisResult emptyResult() {
        return new AnalysisResult("UNKNOWN", List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private AiAnalysisRecord saveFailedRecord(
            Long executionId,
            Long projectId,
            Long testTaskId,
            AiModel model,
            String promptName,
            String promptVersion,
            Long createdBy,
            String errorMessage
    ) {
        clearExisting(executionId);
        String message = errorMessage == null ? null : errorMessage;
        if (message != null && message.length() > 4000) {
            message = message.substring(0, 4000);
        }
        return recordRepository.save(new AiAnalysisRecord(
                executionId,
                projectId,
                testTaskId,
                model.getId(),
                model.getName(),
                model.getProvider(),
                null,
                promptName,
                promptVersion,
                "FAILED",
                null,
                null,
                message,
                createdBy
        ));
    }

    private void clearExisting(Long executionId) {
        recordRepository.findByExecutionId(executionId).ifPresent(record -> {
            recordRepository.delete(record);
            recordRepository.flush();
        });
    }

    private PromptTemplate findPrompt(Long promptId) {
        try {
            return aiModelService.findPrompt(promptId, "FAILURE_ANALYSIS");
        } catch (BusinessException exception) {
            if (exception.getCode() == 40421 && promptId == null) {
                return null;
            }
            throw exception;
        }
    }

    private String fetchConsoleLog(TestExecution execution, TestTask task, Long projectId) {
        JenkinsPipelineBinding binding = resolveBinding(task, projectId);
        JenkinsClient.JenkinsTarget target = null;
        if (binding != null) {
            target = jenkinsAdminService.toTarget(
                    jenkinsAdminService.requireEnabledInstance(binding.getJenkinsInstanceId())
            );
        }
        try {
            return jenkinsClient.fetchConsoleLog(target, execution.getConsoleLogRef());
        } catch (BusinessException exception) {
            return null;
        }
    }

    private JenkinsPipelineBinding resolveBinding(TestTask task, Long projectId) {
        if (task.getPipelineBindingId() != null) {
            return bindingRepository.findByIdAndProjectId(task.getPipelineBindingId(), projectId).orElse(null);
        }
        return bindingRepository.findFirstByAutomationProjectIdOrderByCreatedAtAsc(task.getAutomationProjectId())
                .orElse(null);
    }

    private String buildContext(
            Project project,
            TestTask task,
            AutomationProject automationProject,
            TestExecution execution,
            TestReport report,
            String consoleLog,
            String failureDetail
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("项目：").append(project.getName()).append("\n");
        builder.append("测试任务：").append(task.getName()).append("\n");
        builder.append("自动化项目：").append(automationProject.getName()).append("\n");
        builder.append("Jenkins Job：").append(execution.getJenkinsJob() == null ? "" : execution.getJenkinsJob()).append("\n");
        builder.append("Build：").append(execution.getBuildNumber() == null ? "" : execution.getBuildNumber()).append("\n");
        builder.append("Branch：").append(automationProject.getBranch() == null ? "" : automationProject.getBranch()).append("\n");
        builder.append("Commit：").append(execution.getCommitSha() == null ? "" : execution.getCommitSha()).append("\n");
        builder.append("环境：").append(automationProject.getEnvironment() == null ? "" : automationProject.getEnvironment()).append("\n");
        if (report != null) {
            builder.append("用例统计：总数 ").append(report.getTotalCount())
                    .append("，通过 ").append(report.getPassedCount())
                    .append("，失败 ").append(report.getFailedCount())
                    .append("，Broken ").append(report.getBrokenCount()).append("\n");
        }
        builder.append("\n失败详情：\n").append(failureDetail == null ? "（无）" : failureDetail).append("\n");
        builder.append("\nJenkins 控制台日志（节选）：\n").append(truncate(consoleLog, 6000)).append("\n");
        return builder.toString();
    }

    private String callModelWithRetry(AiModel model, String promptContent, String context) {
        String userPrompt = promptContent + "\n\n失败信息:\n" + context;
        return aiProviderClient.completeJson(model, userPrompt);
    }

    private AnalysisResult parseResult(String rawJson, String consoleLog, String failureDetail) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(rawJson));
            String category = normalizeCategory(root.path("category").asText(""));
            if (category == null) {
                category = classify(consoleLog, failureDetail);
            }
            return new AnalysisResult(
                    category,
                    stringList(root, "possibleCauses", "可能原因"),
                    stringList(root, "relatedErrors", "相关报错"),
                    stringList(root, "checkLocations", "排查位置"),
                    stringList(root, "fixDirections", "修复方向"),
                    stringList(root, "riskWarnings", "风险提示")
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(50202, "AI 返回的 JSON 格式不正确");
        }
    }

    private String normalizeCategory(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return CATEGORY_LABELS.containsKey(normalized) ? normalized : null;
    }

    private List<String> stringList(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode node = root.path(name);
            if (node.isArray()) {
                List<String> items = new ArrayList<>();
                node.forEach(item -> {
                    String text = item.asText("").trim();
                    if (!text.isBlank()) {
                        items.add(text);
                    }
                });
                return items;
            }
        }
        return List.of();
    }

    private String mockAnalysis(String consoleLog, String failureDetail) {
        String category = classify(consoleLog, failureDetail);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("category", category);
        body.put("possibleCauses", causes(category));
        body.put("relatedErrors", relatedErrors(category, consoleLog, failureDetail));
        body.put("checkLocations", checkLocations(category));
        body.put("fixDirections", fixDirections(category));
        body.put("riskWarnings", riskWarnings(category));
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(50220, "AI 分析结果序列化失败");
        }
    }

    private String classify(String consoleLog, String failureDetail) {
        String text = ((consoleLog == null ? "" : consoleLog) + "\n" + (failureDetail == null ? "" : failureDetail))
                .toLowerCase(Locale.ROOT);
        if (containsAny(text, "null pointer", "nullpointer", "attributeerror", "import error",
                "syntaxerror", "nameerror", "typeerror", "module not found", "代码", "语法")) {
            return "TEST_CODE";
        }
        if (containsAny(text, "404", "500", "503", "schema", "response mismatch", "接口", "字段", "field")) {
            return "API_CHANGE";
        }
        if (containsAny(text, "connection refused", "connection reset", "connectionerror",
                "timeout", "timed out", "unreachable", "socket", "网络", "超时")) {
            return "NETWORK";
        }
        if (containsAny(text, "chromedriver", "geckodriver", "driver", "no such element",
                "nosuchelement", "browser", "chrome", "permission denied", "环境", "权限")) {
            return "ENVIRONMENT";
        }
        if (containsAny(text, "assertion", "assert", "expected", "fixture", "数据", "断言")) {
            return "TEST_DATA";
        }
        return "UNKNOWN";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> causes(String category) {
        return switch (category) {
            case "NETWORK" -> List.of(
                    "测试环境与 Jenkins/被测服务之间网络不稳定",
                    "被测服务未启动或端口未开放",
                    "Jenkins Agent 无法访问目标环境"
            );
            case "ENVIRONMENT" -> List.of(
                    "测试环境中缺少浏览器驱动或驱动版本不匹配",
                    "测试执行环境依赖未安装完整",
                    "被测环境配置与用例预期不一致"
            );
            case "TEST_DATA" -> List.of(
                    "测试数据过期或被其他任务污染",
                    "前置数据准备失败",
                    "断言依赖的数据与接口返回值不一致"
            );
            case "API_CHANGE" -> List.of(
                    "接口响应字段或状态码发生变化",
                    "页面元素定位方式变更",
                    "接口契约与用例预期不一致"
            );
            case "TEST_CODE" -> List.of(
                    "用例代码存在空指针或类型错误",
                    "测试代码缺少必要的 import 或依赖",
                    "用例逻辑与业务规则不一致"
            );
            default -> List.of(
                    "失败信息不足以自动判定根因",
                    "可能存在多因素叠加导致失败"
            );
        };
    }

    private List<String> relatedErrors(String category, String consoleLog, String failureDetail) {
        List<String> result = new ArrayList<>();
        if (failureDetail != null && !failureDetail.isBlank()) {
            result.add(failureDetail.length() > 300 ? failureDetail.substring(0, 300) : failureDetail);
        }
        if (consoleLog != null) {
            for (String line : consoleLog.split("\\R")) {
                if (line.contains("ERROR") || line.contains("Error") || line.contains("Exception")) {
                    result.add(line.trim());
                    if (result.size() >= 4) {
                        break;
                    }
                }
            }
        }
        if (result.isEmpty()) {
            result.add("未捕获到明确的报错堆栈，建议结合完整 Jenkins 日志进一步排查。");
        }
        return result;
    }

    private List<String> checkLocations(String category) {
        return switch (category) {
            case "NETWORK" -> List.of("被测服务健康检查与端口监听状态", "Agent 到目标环境的网络连通性", "防火墙与安全组规则");
            case "ENVIRONMENT" -> List.of("Jenkins Agent 的 PATH 环境变量", "浏览器驱动安装路径与版本", "被测环境服务健康检查");
            case "TEST_DATA" -> List.of("测试数据准备脚本", "被测数据的状态与时效", "断言所依赖字段的取值");
            case "API_CHANGE" -> List.of("接口文档与响应契约", "页面元素定位器", "接口版本与灰度配置");
            case "TEST_CODE" -> List.of("用例失败行附近的代码逻辑", "依赖与 import 声明", "业务规则与断言逻辑");
            default -> List.of("完整 Jenkins 日志", "Allure 失败用例详情", "历史执行对比");
        };
    }

    private List<String> fixDirections(String category) {
        return switch (category) {
            case "NETWORK" -> List.of("确认被测服务可用后重试", "修复 Agent 到目标环境的网络链路", "放宽或调整网络超时配置");
            case "ENVIRONMENT" -> List.of("在 Agent 上安装与浏览器版本匹配的驱动", "补齐测试执行依赖并加入 PATH", "修正被测环境配置后重跑");
            case "TEST_DATA" -> List.of("刷新或重新生成测试数据", "修复前置数据准备脚本", "修正断言依赖的字段取值");
            case "API_CHANGE" -> List.of("按最新契约更新用例断言", "更新页面元素定位器", "与研发确认变更后同步用例");
            case "TEST_CODE" -> List.of("修复用例代码中的空指针/类型错误", "补齐缺失依赖与 import", "修正用例业务逻辑");
            default -> List.of("补充更完整的失败信息后重新分析", "结合 Allure 报告人工定位根因");
        };
    }

    private List<String> riskWarnings(String category) {
        return switch (category) {
            case "NETWORK", "ENVIRONMENT" -> List.of("若为环境或网络问题，重试不会改善结果", "需区分环境缺失与用例断言失败两类根因");
            case "TEST_DATA", "TEST_CODE" -> List.of("修复后需重新执行验证，避免只修正局部掩盖其他问题");
            case "API_CHANGE" -> List.of("接口变更可能影响多个用例，建议回归相关用例集合");
            default -> List.of("当前根因未明确，建议结合人工分析避免误判");
        };
    }

    private String serializeBody(AnalysisResult result) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "possibleCauses", result.possibleCauses(),
                    "relatedErrors", result.relatedErrors(),
                    "checkLocations", result.checkLocations(),
                    "fixDirections", result.fixDirections(),
                    "riskWarnings", result.riskWarnings()
            ));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(50220, "AI 分析结果序列化失败");
        }
    }

    private String buildSummary(AnalysisResult result) {
        String label = CATEGORY_LABELS.getOrDefault(result.category(), result.category());
        String cause = result.possibleCauses().isEmpty() ? "" : result.possibleCauses().get(0);
        return "AI 失败分析（" + label + "）：" + cause + " 分析结果仅供参考。";
    }

    private String stripCodeFence(String rawJson) {
        String value = rawJson.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        return value;
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "\n...(日志过长，已截断)";
    }

    private record AnalysisResult(
            String category,
            List<String> possibleCauses,
            List<String> relatedErrors,
            List<String> checkLocations,
            List<String> fixDirections,
            List<String> riskWarnings
    ) {
    }
}
