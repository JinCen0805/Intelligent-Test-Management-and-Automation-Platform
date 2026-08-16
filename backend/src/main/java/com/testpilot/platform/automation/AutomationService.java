package com.testpilot.platform.automation;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.user.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutomationService {

    private final AutomationProjectRepository automationProjectRepository;
    private final TestTaskRepository testTaskRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestReportRepository testReportRepository;
    private final ProjectAccessService projectAccessService;
    private final JenkinsClient jenkinsClient;

    public AutomationService(
            AutomationProjectRepository automationProjectRepository,
            TestTaskRepository testTaskRepository,
            TestExecutionRepository testExecutionRepository,
            TestReportRepository testReportRepository,
            ProjectAccessService projectAccessService,
            JenkinsClient jenkinsClient
    ) {
        this.automationProjectRepository = automationProjectRepository;
        this.testTaskRepository = testTaskRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.testReportRepository = testReportRepository;
        this.projectAccessService = projectAccessService;
        this.jenkinsClient = jenkinsClient;
    }

    public List<AutomationDtos.AutomationProjectResponse> listAutomationProjects(Long projectId, User user) {
        projectAccessService.requireRead(projectId, user);
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
        projectAccessService.requireWrite(projectId, user);
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
        return AutomationDtos.AutomationProjectResponse.from(automationProjectRepository.save(project));
    }

    public List<AutomationDtos.TestTaskResponse> listTasks(Long projectId, User user) {
        projectAccessService.requireRead(projectId, user);
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
        projectAccessService.requireWrite(projectId, user);
        AutomationProject automationProject = automationProjectRepository
                .findByIdAndProjectId(request.automationProjectId(), projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        TestTask task = new TestTask(
                projectId,
                request.name().trim(),
                request.taskType().trim().toUpperCase(),
                automationProject.getId(),
                request.environment().trim().toUpperCase(),
                request.branch().trim(),
                request.jenkinsJob() == null || request.jenkinsJob().isBlank()
                        ? automationProject.getJenkinsJob()
                        : request.jenkinsJob().trim(),
                request.parametersJson()
        );
        return AutomationDtos.TestTaskResponse.from(testTaskRepository.save(task));
    }

    public List<AutomationDtos.TestExecutionResponse> listExecutions(Long projectId, User user) {
        projectAccessService.requireRead(projectId, user);
        return testExecutionRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toExecutionResponse)
                .toList();
    }

    @Transactional
    public AutomationDtos.TestExecutionResponse execute(
            Long projectId,
            Long taskId,
            User user
    ) {
        projectAccessService.requireWrite(projectId, user);
        TestTask task = testTaskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        AutomationProject automationProject = automationProjectRepository
                .findByIdAndProjectId(task.getAutomationProjectId(), projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        JenkinsClient.TriggerResult trigger = jenkinsClient.trigger(automationProject, task);
        TestExecution execution = new TestExecution(
                projectId,
                task.getId(),
                automationProject.getId(),
                task.getJenkinsJob(),
                trigger.queueUrl(),
                trigger.buildNumber(),
                "QUEUED",
                LocalDateTime.now()
        );
        return toExecutionResponse(testExecutionRepository.save(execution));
    }

    @Transactional
    public AutomationDtos.TestExecutionResponse updateResult(
            Long projectId,
            Long executionId,
            AutomationDtos.ExecutionResultRequest request,
            User currentUser
    ) {
        projectAccessService.requireWrite(projectId, currentUser);
        validateReport(request);
        TestExecution execution = testExecutionRepository.findByIdAndProjectId(executionId, projectId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        LocalDateTime finishedAt = LocalDateTime.now();
        execution.applyResult(
                request.status().trim().toUpperCase(),
                blankToNull(request.commitSha()),
                blankToNull(request.consoleLogRef()),
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
                blankToNull(request.artifactRef())
        ));
        testExecutionRepository.save(execution);
        return AutomationDtos.TestExecutionResponse.from(execution, savedReport);
    }

    @Transactional
    public AutomationDtos.TestExecutionResponse sync(
            Long projectId,
            Long executionId,
            User currentUser
    ) {
        projectAccessService.requireWrite(projectId, currentUser);
        TestExecution execution = testExecutionRepository.findByIdAndProjectId(executionId, projectId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        TestTask task = testTaskRepository.findByIdAndProjectId(execution.getTestTaskId(), projectId)
                .orElseThrow(() -> new BusinessException(40411, "测试任务不存在"));
        AutomationProject automationProject = automationProjectRepository
                .findByIdAndProjectId(execution.getAutomationProjectId(), projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        JenkinsClient.SyncResult result = jenkinsClient.sync(automationProject, task, execution);
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
                    result.totalCount(),
                    result.passedCount(),
                    result.failedCount(),
                    result.brokenCount(),
                    result.skippedCount(),
                    result.artifactRef()
            ));
        }
        return toExecutionResponse(execution);
    }

    @Transactional
    public AutomationDtos.TestExecutionResponse updateResultFromCallback(
            Long executionId,
            AutomationDtos.ExecutionResultRequest request
    ) {
        TestExecution execution = testExecutionRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        return saveReport(execution.getId(), request);
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
        validateReport(request);
        TestExecution execution = testExecutionRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        LocalDateTime finishedAt = LocalDateTime.now();
        execution.applyResult(
                request.status().trim().toUpperCase(),
                blankToNull(request.commitSha()),
                blankToNull(request.consoleLogRef()),
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
                blankToNull(request.artifactRef())
        ));
        testExecutionRepository.save(execution);
        return AutomationDtos.TestExecutionResponse.from(execution, savedReport);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateReport(AutomationDtos.ExecutionResultRequest request) {
        String status = request.status().trim().toUpperCase();
        if (!List.of("RUNNING", "PASSED", "FAILED", "ABORTED").contains(status)) {
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
