package com.testpilot.platform.automation;

import com.testpilot.platform.common.BusinessException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Jenkins 鎵ц杞璋冨害锛堜粠 AutomationService 鎷嗗嚭锛夛細鎸?updated_at 鍒嗘壒鎷夊彇娲昏穬鎵ц锛? * 鎸夊疄渚嬭疆璇㈤棿闅斾笌澶辫触閫€閬垮喅瀹氭槸鍚﹀悓姝ワ紝杩炵画澶辫触杈惧埌闃堝€煎悗鏍囪 FAILED銆? */
@Component
public class ExecutionPollingService {

    private final AutomationService automationService;
    private final TestExecutionRepository testExecutionRepository;
    private final TestTaskRepository testTaskRepository;
    private final AutomationProjectRepository automationProjectRepository;
    private final JenkinsPipelineBindingRepository bindingRepository;
    private final JenkinsAdminService jenkinsAdminService;
    private final ExecutionReportWriter reportWriter;
    private final TransactionTemplate transactionTemplate;
    private final int pollBatchSize;
    private final long defaultPollIntervalMs;
    private final int syncFailureThreshold;

    public ExecutionPollingService(
            AutomationService automationService,
            TestExecutionRepository testExecutionRepository,
            TestTaskRepository testTaskRepository,
            AutomationProjectRepository automationProjectRepository,
            JenkinsPipelineBindingRepository bindingRepository,
            JenkinsAdminService jenkinsAdminService,
            ExecutionReportWriter reportWriter,
            TransactionTemplate transactionTemplate,
            @Value("${testpilot.jenkins.poll-batch-size:50}") int pollBatchSize,
            @Value("${testpilot.jenkins.poll-interval-ms:10000}") long defaultPollIntervalMs,
            @Value("${testpilot.jenkins.sync-failure-threshold:5}") int syncFailureThreshold
    ) {
        this.automationService = automationService;
        this.testExecutionRepository = testExecutionRepository;
        this.testTaskRepository = testTaskRepository;
        this.automationProjectRepository = automationProjectRepository;
        this.bindingRepository = bindingRepository;
        this.jenkinsAdminService = jenkinsAdminService;
        this.reportWriter = reportWriter;
        this.transactionTemplate = transactionTemplate;
        this.pollBatchSize = Math.max(1, pollBatchSize);
        this.defaultPollIntervalMs = Math.max(0L, defaultPollIntervalMs);
        this.syncFailureThreshold = Math.max(1, syncFailureThreshold);
    }

    @Scheduled(fixedDelayString = "${testpilot.jenkins.scheduler-delay-ms:1000}")
    public void pollExecutions() {
        List<TestExecution> activeExecutions = testExecutionRepository
                .findTopNByStatusInOrderByUpdatedAtAsc(List.of("QUEUED", "RUNNING"), pollBatchSize);
        for (TestExecution execution : activeExecutions) {
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
                    continue;
                } else {
                    automationService.syncExecution(execution, task, automationProject);
                }
            } catch (RuntimeException exception) {
                markPollingFailure(execution, exception.getMessage() == null
                        ? "Jenkins 状态同步失败"
                        : exception.getMessage());
            }
        }
    }

    private boolean isPollingDue(TestExecution execution, TestTask task) {
        if (execution.getUpdatedAt() == null) {
            return true;
        }
        JenkinsPipelineBinding binding = automationService.resolveBinding(task, execution.getProjectId());
        long interval = defaultPollIntervalMs;
        if (binding != null) {
            try {
                JenkinsInstance instance = jenkinsAdminService.requireEnabledInstance(binding.getJenkinsInstanceId());
                interval = instance.getPollIntervalMs();
            } catch (BusinessException exception) {
                // 实例不可用：沿用默认间隔，避免退化成每秒轮询。
            }
        }
        return Duration.between(execution.getUpdatedAt(), LocalDateTime.now()).toMillis()
                >= interval * (1L << Math.min(execution.getSyncFailureCount(), 4));
    }

    private void markPollingFailure(TestExecution execution, String message) {
        message = message == null ? "Jenkins 状态同步失败" : message;
        if (message.length() > 1000) {
            message = message.substring(0, 1000);
        }
        execution.recordSyncFailure(message);
        String failureMessage = message;
        transactionTemplate.executeWithoutResult(status -> {
            if (execution.getSyncFailureCount() >= syncFailureThreshold) {
                execution.applyJenkinsSync(
                        execution.getBuildNumber(),
                        execution.getQueueUrl(),
                        "FAILED",
                        execution.getCommitSha(),
                        execution.getConsoleLogRef(),
                        execution.getStartedAt(),
                        LocalDateTime.now()
                );
                reportWriter.saveReport(execution, new AutomationDtos.ExecutionResultRequest(
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
                ), true);
            } else {
                reportWriter.persistActiveProgress(execution);
            }
        });
    }
}