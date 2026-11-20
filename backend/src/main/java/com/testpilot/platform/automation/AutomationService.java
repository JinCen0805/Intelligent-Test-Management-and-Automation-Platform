package com.testpilot.platform.automation;

import com.testpilot.platform.audit.OperationAuditService;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.user.User;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AutomationService {

    /**
     * 控制台日志引用只允许指向某次构建的 consoleText，防止客户端把任意 Jenkins API 路径
     * 交给平台服务账号去读取。
     */
    private static final Pattern SAFE_CONSOLE_LOG_REF =
            Pattern.compile("^/job/[A-Za-z0-9_./-]+/\\d+/consoleText$");

    private final AutomationProjectRepository automationProjectRepository;
    private final TestTaskRepository testTaskRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestReportRepository testReportRepository;
    private final ProjectAccessService projectAccessService;
    private final JenkinsClient jenkinsClient;
    private final JenkinsPipelineBindingRepository bindingRepository;
    private final JenkinsAdminService jenkinsAdminService;
    private final AllureReportStorage allureReportStorage;
    private final OperationAuditService operationAuditService;
    private final TransactionTemplate transactionTemplate;
    private final int maxWaitMinutes;

    public AutomationService(
            AutomationProjectRepository automationProjectRepository,
            TestTaskRepository testTaskRepository,
            TestExecutionRepository testExecutionRepository,
            TestReportRepository testReportRepository,
            ProjectAccessService projectAccessService,
            JenkinsClient jenkinsClient,
            JenkinsPipelineBindingRepository bindingRepository,
            JenkinsAdminService jenkinsAdminService,
            AllureReportStorage allureReportStorage,
            OperationAuditService operationAuditService,
            TransactionTemplate transactionTemplate,
            @Value("${testpilot.jenkins.max-wait-minutes:30}") int maxWaitMinutes
    ) {
        this.automationProjectRepository = automationProjectRepository;
        this.testTaskRepository = testTaskRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.testReportRepository = testReportRepository;
        this.projectAccessService = projectAccessService;
        this.jenkinsClient = jenkinsClient;
        this.bindingRepository = bindingRepository;
        this.jenkinsAdminService = jenkinsAdminService;
        this.allureReportStorage = allureReportStorage;
        this.operationAuditService = operationAuditService;
        this.transactionTemplate = transactionTemplate;
        this.maxWaitMinutes = maxWaitMinutes;
    }

    public List<AutomationDtos.AutomationProjectResponse> listAutomationProjects(Long projectId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        return automationProjectRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(AutomationDtos.AutomationProjectResponse::from)
                .toList();
    }

    @Transactional
    public AutomationDtos.AutomationProjectResponse createAutomationProject(
            Long projectId,
            AutomationDtos.AutomationProjectRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        AutomationProject project = new AutomationProject(
                projectId,
                request.name().trim(),
                request.automationType().trim().toUpperCase(),
                request.repositoryUrl().trim(),
                request.branch().trim(),
                blankToNull(request.credentialRef()),
                request.framework().trim(),
                blankToNull(request.testPath()),
                blankToNull(request.executeCommand()),
                blankToNull(request.environment()),
                blankToNull(request.jenkinsJob())
        );
        jenkinsAdminService.validateCredentialRef(projectId, request.credentialRef());
        return AutomationDtos.AutomationProjectResponse.from(automationProjectRepository.save(project));
    }

    @Transactional
    public AutomationDtos.AutomationProjectResponse updateAutomationProject(
            Long projectId,
            Long automationProjectId,
            AutomationDtos.AutomationProjectRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        AutomationProject project = automationProjectRepository
                .findByIdAndProjectId(automationProjectId, projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        project.setName(request.name().trim());
        project.setAutomationType(request.automationType().trim().toUpperCase());
        project.setRepositoryUrl(request.repositoryUrl().trim());
        project.setBranch(request.branch().trim());
        project.setCredentialRef(blankToNull(request.credentialRef()));
        project.setFramework(request.framework().trim());
        project.setTestPath(blankToNull(request.testPath()));
        project.setExecuteCommand(blankToNull(request.executeCommand()));
        project.setEnvironment(blankToNull(request.environment()));
        project.setJenkinsJob(blankToNull(request.jenkinsJob()));
        jenkinsAdminService.validateCredentialRef(projectId, request.credentialRef());
        return AutomationDtos.AutomationProjectResponse.from(automationProjectRepository.save(project));
    }

    public List<AutomationDtos.TestTaskResponse> listTasks(Long projectId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        return testTaskRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(AutomationDtos.TestTaskResponse::from)
                .toList();
    }

    @Transactional
    public AutomationDtos.TestTaskResponse createTask(
            Long projectId,
            AutomationDtos.TestTaskRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        AutomationProject automationProject = automationProjectRepository
                .findByIdAndProjectId(request.automationProjectId(), projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        jenkinsAdminService.validateCredentialRef(
                projectId, automationProject.getCredentialRef()
        );
        TaskTarget target = resolveTaskTarget(
                projectId, automationProject, request.pipelineBindingId()
        );
        TestTask task = new TestTask(
                projectId,
                request.name().trim(),
                request.taskType().trim().toUpperCase(),
                automationProject.getId(),
                target.binding() == null ? null : target.binding().getId(),
                request.environment().trim().toUpperCase(),
                request.branch().trim(),
                target.jobName(),
                request.parametersJson()
        );
        return AutomationDtos.TestTaskResponse.from(testTaskRepository.save(task));
    }

    @Transactional
    public AutomationDtos.TestTaskResponse updateTask(
            Long projectId,
            Long taskId,
            AutomationDtos.TestTaskRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        TestTask task = testTaskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        AutomationProject automationProject = automationProjectRepository
                .findByIdAndProjectId(request.automationProjectId(), projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        jenkinsAdminService.validateCredentialRef(
                projectId, automationProject.getCredentialRef()
        );
        TaskTarget target = resolveTaskTarget(
                projectId, automationProject, request.pipelineBindingId()
        );
        task.update(
                request.name().trim(),
                request.taskType().trim().toUpperCase(),
                automationProject.getId(),
                target.binding() == null ? null : target.binding().getId(),
                request.environment().trim().toUpperCase(),
                request.branch().trim(),
                target.jobName(),
                request.parametersJson()
        );
        return AutomationDtos.TestTaskResponse.from(testTaskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long projectId, Long taskId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        TestTask task = testTaskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        if (testExecutionRepository.existsActiveByTaskId(projectId, taskId)) {
            throw new BusinessException(40021, "该任务存在排队中或运行中的执行，无法删除");
        }
        testTaskRepository.delete(task);
    }

    @Transactional
    public AutomationDtos.TestTaskResponse updateTaskStatus(
            Long projectId,
            Long taskId,
            AutomationDtos.TestTaskStatusRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        TestTask task = testTaskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        String status = request.status().trim().toUpperCase();
        if (!List.of("ENABLED", "DISABLED").contains(status)) {
            throw new BusinessException(40022, "任务状态只能是 ENABLED 或 DISABLED");
        }
        task.setStatus(status);
        return AutomationDtos.TestTaskResponse.from(testTaskRepository.save(task));
    }

    @Transactional
    public AutomationDtos.TestExecutionResponse stopTask(Long projectId, Long taskId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        TestTask task = testTaskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        TestExecution active = testExecutionRepository.findLatestActiveByTaskId(projectId, task.getId())
                .orElseThrow(() -> new BusinessException(40020, "该任务当前没有排队中或运行中的执行"));
        return cancel(projectId, active.getId(), user);
    }

    private record TaskTarget(JenkinsPipelineBinding binding, String jobName) {
    }

    private TaskTarget resolveTaskTarget(
            Long projectId,
            AutomationProject automationProject,
            Long pipelineBindingId
    ) {
        JenkinsPipelineBinding binding = pipelineBindingId == null
                ? bindingRepository.findFirstByProjectIdAndAutomationProjectIdOrderByCreatedAtAsc(
                        projectId, automationProject.getId()).orElse(null)
                : bindingRepository.findByIdAndProjectId(pipelineBindingId, projectId)
                    .orElseThrow(() -> new BusinessException(40452, "Pipeline Binding 不存在"));
        if (binding != null && !binding.getAutomationProjectId().equals(automationProject.getId())) {
            throw new BusinessException(40057, "Pipeline Binding 不属于当前自动化项目");
        }
        String jobName = binding == null
                ? automationProject.getJenkinsJob()
                : binding.getJobName();
        if (jobName == null || jobName.isBlank()) {
            throw new BusinessException(40008, "未配置可执行的 Jenkins Job");
        }
        return new TaskTarget(binding, jobName);
    }

    public List<AutomationDtos.TestExecutionResponse> listExecutions(Long projectId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        return testExecutionRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toExecutionResponse)
                .toList();
    }

    /**
     * Jenkins 触发是外部 HTTP 调用，不占用数据库事务；只有执行记录落库使用短事务。
     */
    public AutomationDtos.TestExecutionResponse execute(
            Long projectId,
            Long taskId,
            AutomationDtos.ExecuteRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        TestTask task = testTaskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        if (!"ENABLED".equals(task.getStatus())) {
            throw new BusinessException(40028, "测试任务已禁用，无法执行");
        }
        AutomationProject automationProject = automationProjectRepository
                .findByIdAndProjectId(task.getAutomationProjectId(), projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        jenkinsAdminService.validateCredentialRef(projectId, automationProject.getCredentialRef());
        JenkinsPipelineBinding binding = resolveBinding(task, projectId);
        if (binding != null) {
            jenkinsAdminService.validateCredentialScope(projectId, binding.getCredentialScopeRef());
        }
        JenkinsClient.TriggerResult trigger = jenkinsClient.trigger(
                automationProject, task,
                binding == null ? null : jenkinsAdminService.toTarget(
                        jenkinsAdminService.requireEnabledInstance(binding.getJenkinsInstanceId())
                ),
                request == null ? null : request.parametersJson()
        );
        TestExecution execution = new TestExecution(
                projectId,
                task.getId(),
                automationProject.getId(),
                binding == null ? task.getJenkinsJob() : binding.getJobName(),
                trigger.queueUrl(),
                trigger.buildNumber(),
                "QUEUED",
                LocalDateTime.now()
        );
        execution.recordTrigger(user.getUsername(), "MANUAL");
        TestExecution saved = transactionTemplate.execute(status -> {
            TestExecution persisted = testExecutionRepository.save(execution);
            operationAuditService.record(projectId, user, "EXECUTION_TRIGGER", "EXECUTION", persisted.getId(),
                    "触发执行任务 " + task.getName(), true);
            return persisted;
        });
        return toExecutionResponse(saved);
    }

    public AutomationDtos.TestExecutionResponse retry(
            Long projectId,
            Long executionId,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        TestExecution previous = testExecutionRepository.findByIdAndProjectId(executionId, projectId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        if (!List.of("FAILED", "ABORTED", "TIMEOUT").contains(previous.getStatus())) {
            throw new BusinessException(40018, "只有失败或中止的执行可以重试");
        }
        TestTask task = testTaskRepository.findByIdAndProjectId(previous.getTestTaskId(), projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        AutomationProject automationProject = automationProjectRepository
                .findByIdAndProjectId(previous.getAutomationProjectId(), projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        jenkinsAdminService.validateCredentialRef(projectId, automationProject.getCredentialRef());
        JenkinsPipelineBinding binding = resolveBinding(task, projectId);
        if (binding != null) {
            jenkinsAdminService.validateCredentialScope(projectId, binding.getCredentialScopeRef());
        }
        JenkinsClient.TriggerResult trigger = jenkinsClient.trigger(
                automationProject, task, binding == null ? null : jenkinsAdminService.toTarget(
                        jenkinsAdminService.requireEnabledInstance(binding.getJenkinsInstanceId())
                )
        );
        TestExecution retry = new TestExecution(
                projectId,
                task.getId(),
                automationProject.getId(),
                binding == null ? task.getJenkinsJob() : binding.getJobName(),
                trigger.queueUrl(),
                trigger.buildNumber(),
                "QUEUED",
                LocalDateTime.now()
        );
        retry.recordTrigger(user.getUsername(), "RETRY");
        TestExecution savedRetry = transactionTemplate.execute(status -> {
            TestExecution persisted = testExecutionRepository.save(retry);
            operationAuditService.record(projectId, user, "EXECUTION_RETRY", "EXECUTION", persisted.getId(),
                    "重试执行任务 " + task.getName(), true);
            return persisted;
        });
        return toExecutionResponse(savedRetry);
    }

    @Transactional
    public AutomationDtos.TestExecutionResponse updateResult(
            Long projectId,
            Long executionId,
            AutomationDtos.ExecutionResultRequest request,
            User currentUser
    ) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AUTOMATION_EXECUTE);
        testExecutionRepository.findByIdAndProjectId(executionId, projectId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        // 手工上报来自普通项目成员：consoleLogRef 只能指向本次构建的 consoleText。
        return saveReport(executionId, request, false);
    }

    public AutomationDtos.TestExecutionResponse sync(
            Long projectId,
            Long executionId,
            User currentUser
    ) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AUTOMATION_EXECUTE);
        TestExecution execution = testExecutionRepository.findByIdAndProjectId(executionId, projectId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        TestTask task = testTaskRepository.findByIdAndProjectId(execution.getTestTaskId(), projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        AutomationProject automationProject = automationProjectRepository
                .findByIdAndProjectId(execution.getAutomationProjectId(), projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        return syncExecution(execution, task, automationProject);
    }

    public AutomationDtos.TestExecutionResponse cancel(
            Long projectId,
            Long executionId,
            User currentUser
    ) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AUTOMATION_EXECUTE);
        TestExecution execution = testExecutionRepository.findByIdAndProjectId(executionId, projectId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        if (!List.of("QUEUED", "RUNNING").contains(execution.getStatus())) {
            throw new BusinessException(40019, "只有排队中或运行中的执行可以取消");
        }
        TestTask task = testTaskRepository.findByIdAndProjectId(execution.getTestTaskId(), projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        AutomationProject automationProject = automationProjectRepository
                .findByIdAndProjectId(execution.getAutomationProjectId(), projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        jenkinsAdminService.validateCredentialRef(projectId, automationProject.getCredentialRef());
        JenkinsPipelineBinding binding = resolveBinding(task, projectId);
        if (binding != null) {
            jenkinsAdminService.validateCredentialScope(projectId, binding.getCredentialScopeRef());
        }
        jenkinsClient.cancel(
                automationProject,
                task,
                execution,
                binding == null ? null : jenkinsAdminService.toTarget(
                        jenkinsAdminService.requireEnabledInstance(binding.getJenkinsInstanceId())
                )
        );
        transactionTemplate.executeWithoutResult(status -> {
            LocalDateTime finishedAt = LocalDateTime.now();
            execution.applyJenkinsSync(
                    execution.getBuildNumber(),
                    execution.getQueueUrl(),
                    "ABORTED",
                    execution.getCommitSha(),
                    execution.getConsoleLogRef(),
                    execution.getStartedAt(),
                    finishedAt
            );
            testExecutionRepository.save(execution);
            saveReport(execution.getId(), new AutomationDtos.ExecutionResultRequest(
                    "ABORTED",
                    execution.getCommitSha(),
                    execution.getConsoleLogRef(),
                    null,
                    0, 0, 0, 0, 0,
                    null,
                    "用户取消了 Jenkins 执行。",
                    null,
                    null,
                    null
            ));
            operationAuditService.record(projectId, currentUser, "EXECUTION_STOP", "EXECUTION", execution.getId(),
                    "取消执行任务 " + task.getName(), true);
        });
        return toExecutionResponse(execution);
    }

    @Transactional
    public void deleteExecution(Long projectId, Long executionId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        TestExecution execution = testExecutionRepository.findByIdAndProjectId(executionId, projectId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        if (List.of("QUEUED", "RUNNING").contains(execution.getStatus())) {
            throw new BusinessException(40023, "该执行正在排队或运行中，无法删除");
        }
        testReportRepository.findByExecutionId(executionId)
                .ifPresent(testReportRepository::delete);
        allureReportStorage.delete(executionId);
        testExecutionRepository.delete(execution);
        operationAuditService.record(projectId, user, "EXECUTION_DELETE", "EXECUTION", executionId,
                "删除执行记录 #" + executionId, true);
    }

    public String fetchConsoleLog(Long projectId, Long executionId, User user) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        TestExecution execution = testExecutionRepository.findByIdAndProjectId(executionId, projectId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        if (execution.getConsoleLogRef() == null || execution.getConsoleLogRef().isBlank()) {
            return null;
        }
        TestTask task = testTaskRepository.findByIdAndProjectId(execution.getTestTaskId(), projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        JenkinsPipelineBinding binding = resolveBinding(task, projectId);
        JenkinsClient.JenkinsTarget target = binding == null ? null : jenkinsAdminService.toTarget(
                jenkinsAdminService.requireEnabledInstance(binding.getJenkinsInstanceId())
        );
        return jenkinsClient.fetchConsoleLog(target, execution.getConsoleLogRef());
    }

    @Scheduled(fixedDelayString = "${testpilot.jenkins.scheduler-delay-ms:1000}")
    public void pollExecutions() {
        testExecutionRepository.findAllByStatusIn(List.of("QUEUED", "RUNNING"))
                .forEach(execution -> {
                    try {
                        TestTask task = testTaskRepository.findByIdAndProjectId(
                                execution.getTestTaskId(), execution.getProjectId()
                        ).orElse(null);
                        AutomationProject automationProject = task == null ? null : automationProjectRepository
                                .findByIdAndProjectId(execution.getAutomationProjectId(), execution.getProjectId())
                                .orElse(null);
                        if (task == null || automationProject == null) {
                            markPollingFailure(execution, "关联的测试任务或自动化项目不存在");
                        } else if (!isPollingDue(execution, task)) {
                            return;
                        } else {
                            syncExecution(execution, task, automationProject);
                        }
                    } catch (RuntimeException exception) {
                        markPollingFailure(execution, exception.getMessage() == null
                                ? "Jenkins 状态同步失败"
                                : exception.getMessage());
                    }
                });
    }

    private AutomationDtos.TestExecutionResponse syncExecution(
            TestExecution execution,
            TestTask task,
            AutomationProject automationProject
    ) {
        JenkinsPipelineBinding binding = resolveBinding(task, execution.getProjectId());
        if (List.of("QUEUED", "RUNNING").contains(execution.getStatus())
                && execution.getCreatedAt() != null
                && Duration.between(execution.getCreatedAt(), LocalDateTime.now()).toMinutes()
                >= maxWaitMinutes(binding)) {
            LocalDateTime finishedAt = LocalDateTime.now();
            transactionTemplate.executeWithoutResult(status -> {
                execution.applyJenkinsSync(
                        execution.getBuildNumber(),
                        execution.getQueueUrl(),
                        "TIMEOUT",
                        execution.getCommitSha(),
                        execution.getConsoleLogRef(),
                        execution.getStartedAt(),
                        finishedAt
                );
                testExecutionRepository.save(execution);
                saveReport(execution.getId(), new AutomationDtos.ExecutionResultRequest(
                        "TIMEOUT",
                        execution.getCommitSha(),
                        execution.getConsoleLogRef(),
                        null,
                        0,
                        0,
                        0,
                        0,
                        0,
                        null,
                        "Jenkins 在 " + maxWaitMinutes(binding) + " 分钟内未返回完成结果。",
                        null,
                        null,
                        "超时分析：Jenkins 构建未在最大等待时间内完成，请检查队列、Agent 和构建日志。"
                ));
            });
            return toExecutionResponse(execution);
        }
        JenkinsClient.JenkinsTarget target = binding == null ? null : jenkinsAdminService.toTarget(
                jenkinsAdminService.requireEnabledInstance(binding.getJenkinsInstanceId())
        );
        JenkinsClient.SyncResult result = jenkinsClient.sync(
                automationProject,
                task,
                execution,
                target
        );
        String artifactRef = result.artifactRef();
        JenkinsClient.AllureSummary allureSummary = null;
        if (List.of("PASSED", "FAILED", "ABORTED").contains(result.status()) && target != null) {
            try {
                allureSummary = jenkinsClient.downloadAllureSummary(
                        automationProject, task, execution, target
                );
                byte[] archive = jenkinsClient.downloadAllureArchive(
                        automationProject, task, execution, target
                );
                if (allureReportStorage.store(execution.getId(), archive)) {
                    artifactRef = "OFFLINE_ALLURE";
                }
            } catch (BusinessException exception) {
                if (exception.getCode() != 40457) {
                    throw exception;
                }
                // A legacy Job may not archive allure-report.zip; keep its Jenkins URL as fallback.
            }
        }
        JenkinsClient.AllureSummary finalSummary = allureSummary;
        String finalArtifactRef = artifactRef;
        transactionTemplate.executeWithoutResult(status -> {
            execution.applyJenkinsSync(
                    result.buildNumber(),
                    result.queueUrl(),
                    result.status(),
                    result.commitSha(),
                    result.consoleLogRef(),
                    result.startedAt(),
                    result.finishedAt()
            );
            testExecutionRepository.save(execution);
            if (List.of("PASSED", "FAILED", "ABORTED").contains(result.status())) {
                saveReport(execution.getId(), new AutomationDtos.ExecutionResultRequest(
                        result.status(),
                        result.commitSha(),
                        result.consoleLogRef(),
                        result.allureUrl(),
                        finalSummary == null ? result.totalCount() : finalSummary.totalCount(),
                        finalSummary == null ? result.passedCount() : finalSummary.passedCount(),
                        finalSummary == null ? result.failedCount() : finalSummary.failedCount(),
                        finalSummary == null ? result.brokenCount() : finalSummary.brokenCount(),
                        finalSummary == null ? result.skippedCount() : finalSummary.skippedCount(),
                        finalArtifactRef,
                        null,
                        null,
                        null,
                        result.status().equals("FAILED")
                                ? "基础失败分析：执行结果为失败，请结合 Jenkins 日志和 Allure 失败用例定位。"
                                : null
                ));
            }
        });
        return toExecutionResponse(execution);
    }

    private boolean isPollingDue(TestExecution execution, TestTask task) {
        JenkinsPipelineBinding binding = resolveBinding(task, execution.getProjectId());
        if (binding == null || execution.getUpdatedAt() == null) {
            return true;
        }
        JenkinsInstance instance;
        try {
            instance = jenkinsAdminService.requireEnabledInstance(binding.getJenkinsInstanceId());
        } catch (BusinessException exception) {
            return true;
        }
        return Duration.between(execution.getUpdatedAt(), LocalDateTime.now()).toMillis()
                >= instance.getPollIntervalMs();
    }

    private int maxWaitMinutes(JenkinsPipelineBinding binding) {
        if (binding == null) {
            return maxWaitMinutes;
        }
        try {
            return jenkinsAdminService.requireEnabledInstance(binding.getJenkinsInstanceId())
                    .getMaxWaitMinutes();
        } catch (BusinessException exception) {
            return maxWaitMinutes;
        }
    }

    private void markPollingFailure(TestExecution execution, String message) {
        message = message == null ? "Jenkins 状态同步失败" : message;
        if (message.length() > 1000) {
            message = message.substring(0, 1000);
        }
        execution.recordSyncFailure(message);
        String failureMessage = message;
        transactionTemplate.executeWithoutResult(status -> {
            if (execution.getSyncFailureCount() >= 3) {
                execution.applyJenkinsSync(
                        execution.getBuildNumber(),
                        execution.getQueueUrl(),
                        "FAILED",
                        execution.getCommitSha(),
                        execution.getConsoleLogRef(),
                        execution.getStartedAt(),
                        LocalDateTime.now()
                );
                saveReport(execution.getId(), new AutomationDtos.ExecutionResultRequest(
                        "FAILED",
                        execution.getCommitSha(),
                        execution.getConsoleLogRef(),
                        null,
                        0,
                        0,
                        0,
                        0,
                        0,
                        null,
                        failureMessage,
                        null,
                        null,
                        "基础分析：连续多次 Jenkins 状态同步失败，执行已终止。"
                ));
            }
            testExecutionRepository.save(execution);
        });
    }

    @Transactional
    public AutomationDtos.TestExecutionResponse updateResultFromCallback(
            Long executionId,
            AutomationDtos.ExecutionResultRequest request,
            String callbackJob
    ) {
        TestExecution execution = testExecutionRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        if (callbackJob != null && !callbackJob.isBlank()
                && !callbackJob.trim().equals(execution.getJenkinsJob())) {
            throw new BusinessException(40312, "Jenkins 回调 Job 与执行记录不匹配");
        }
        String incomingStatus = request.status().trim().toUpperCase();
        if ("RUNNING".equals(incomingStatus)) {
            throw new BusinessException(40009, "Jenkins 回调只能提交终态结果");
        }
        if (List.of("PASSED", "FAILED", "ABORTED", "TIMEOUT").contains(execution.getStatus())) {
            if (incomingStatus.equals(execution.getStatus())
                    && testReportRepository.findByExecutionId(executionId).isPresent()) {
                return toExecutionResponse(execution);
            }
            throw new BusinessException(40912, "执行已完成，禁止覆盖终态结果");
        }
        // 回调同样来自外部：consoleLogRef 只允许指向本次构建的 consoleText。
        return saveReport(execution.getId(), request, false);
    }

    private AutomationDtos.TestExecutionResponse toExecutionResponse(TestExecution execution) {
        return AutomationDtos.TestExecutionResponse.from(
                execution,
                testReportRepository.findByExecutionId(execution.getId()).orElse(null)
        );
    }

    private AutomationDtos.TestExecutionResponse saveReport(
            Long executionId,
            AutomationDtos.ExecutionResultRequest request
    ) {
        return saveReport(executionId, request, true);
    }

    private AutomationDtos.TestExecutionResponse saveReport(
            Long executionId,
            AutomationDtos.ExecutionResultRequest request,
            boolean trustedSource
    ) {
        validateReport(request);
        TestExecution execution = testExecutionRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        LocalDateTime finishedAt = LocalDateTime.now();
        String consoleLogRef = trustedSource
                ? trustedConsoleLogRef(execution, request.consoleLogRef())
                : resolveExternalConsoleLogRef(execution, request.consoleLogRef());
        execution.applyResult(
                request.status().trim().toUpperCase(),
                blankToNull(request.commitSha()),
                consoleLogRef,
                finishedAt
        );
        testReportRepository.findByExecutionId(executionId)
                .ifPresent(report -> {
                    testReportRepository.delete(report);
                    testReportRepository.flush();
                });
        TestReport savedReport = testReportRepository.save(new TestReport(
                executionId,
                blankToNull(request.allureUrl()),
                request.totalCount(),
                request.passedCount(),
                request.failedCount(),
                request.brokenCount(),
                request.skippedCount(),
                blankToNull(request.artifactRef()),
                blankToNull(request.failureDetail()),
                blankToNull(request.screenshotRef()),
                blankToNull(request.videoRef()),
                blankToNull(request.aiAnalysis())
        ));
        testExecutionRepository.save(execution);
        return AutomationDtos.TestExecutionResponse.from(execution, savedReport);
    }

    private String trustedConsoleLogRef(TestExecution execution, String incoming) {
        String provided = blankToNull(incoming);
        return provided != null ? provided : deriveConsoleLogRef(execution);
    }

    /**
     * 外部上报（手工上报 / Jenkins 回调）的 consoleLogRef 校验：只允许本次构建的 consoleText。
     * 允许两种形式：以 "/" 开头的 Jenkins 路径，或完整 http(s) URL（host 会被替换为配置的 Jenkins 地址）。
     */
    private String resolveExternalConsoleLogRef(TestExecution execution, String incoming) {
        String provided = blankToNull(incoming);
        if (provided == null) {
            return deriveConsoleLogRef(execution);
        }
        String path = extractJenkinsPath(provided);
        if (path == null || !SAFE_CONSOLE_LOG_REF.matcher(path).matches()) {
            throw new BusinessException(40043, "控制台日志引用不合法");
        }
        return path;
    }

    private String extractJenkinsPath(String value) {
        if (value.startsWith("/")) {
            return value;
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return null;
            }
            return uri.getPath();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String deriveConsoleLogRef(TestExecution execution) {
        String job = execution.getJenkinsJob();
        String buildNumber = execution.getBuildNumber();
        if (job == null || job.isBlank() || buildNumber == null || buildNumber.isBlank()) {
            return null;
        }
        String jobPath = Arrays.stream(job.split("/"))
                .filter(segment -> !segment.isBlank())
                .map(segment -> "job/" + segment)
                .collect(Collectors.joining("/"));
        return "/" + jobPath + "/" + buildNumber + "/consoleText";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private JenkinsPipelineBinding resolveBinding(TestTask task, Long projectId) {
        if (task.getPipelineBindingId() != null) {
            return bindingRepository.findByIdAndProjectId(task.getPipelineBindingId(), projectId).orElse(null);
        }
        return bindingRepository.findFirstByProjectIdAndAutomationProjectIdOrderByCreatedAtAsc(
                projectId, task.getAutomationProjectId()).orElse(null);
    }

    private void validateReport(AutomationDtos.ExecutionResultRequest request) {
        String status = request.status().trim().toUpperCase();
        if (!List.of("RUNNING", "PASSED", "FAILED", "ABORTED", "TIMEOUT").contains(status)) {
            throw new BusinessException(40009, "执行状态不合法");
        }
        int sum = request.passedCount()
                + request.failedCount()
                + request.brokenCount()
                + request.skippedCount();
        if (request.totalCount() != sum) {
            throw new BusinessException(40010, "报告总数必须等于通过、失败、Broken 和跳过数量之和");
        }
    }
}
