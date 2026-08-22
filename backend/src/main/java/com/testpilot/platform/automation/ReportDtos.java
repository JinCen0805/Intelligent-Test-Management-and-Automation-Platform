package com.testpilot.platform.automation;

import com.testpilot.platform.project.Project;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record ExecutionView(
            Long id,
            Long projectId,
            String projectName,
            Long testTaskId,
            String taskName,
            String jenkinsJob,
            String queueUrl,
            String buildNumber,
            String commitSha,
            String status,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            Long durationMs,
            String consoleLogRef,
            String syncFailureMessage,
            int syncFailureCount,
            AutomationDtos.TestReportResponse report
    ) {
        static ExecutionView from(
                TestExecution execution,
                String projectName,
                String taskName,
                TestReport report
        ) {
            return new ExecutionView(
                    execution.getId(),
                    execution.getProjectId(),
                    projectName,
                    execution.getTestTaskId(),
                    taskName,
                    execution.getJenkinsJob(),
                    execution.getQueueUrl(),
                    execution.getBuildNumber(),
                    execution.getCommitSha(),
                    execution.getStatus(),
                    execution.getStartedAt(),
                    execution.getFinishedAt(),
                    execution.getDurationMs(),
                    execution.getConsoleLogRef(),
                    execution.getSyncFailureMessage(),
                    execution.getSyncFailureCount(),
                    report == null ? null : AutomationDtos.TestReportResponse.from(report)
            );
        }
    }

    public record ExecutionDetail(
            Long id,
            Long projectId,
            String projectName,
            Long testTaskId,
            String taskName,
            String taskType,
            Long automationProjectId,
            String automationProjectName,
            String jenkinsJob,
            String queueUrl,
            String buildNumber,
            String commitSha,
            String branch,
            String environment,
            String status,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            Long durationMs,
            String consoleLogRef,
            String syncFailureMessage,
            int syncFailureCount,
            String triggeredBy,
            String triggerMethod,
            AutomationDtos.TestReportResponse report
    ) {
        static ExecutionDetail from(
                TestExecution execution,
                Project project,
                TestTask task,
                AutomationProject automationProject,
                TestReport report
        ) {
            return new ExecutionDetail(
                    execution.getId(),
                    execution.getProjectId(),
                    project == null ? null : project.getName(),
                    execution.getTestTaskId(),
                    task == null ? null : task.getName(),
                    task == null ? null : task.getTaskType(),
                    execution.getAutomationProjectId(),
                    automationProject == null ? null : automationProject.getName(),
                    execution.getJenkinsJob(),
                    execution.getQueueUrl(),
                    execution.getBuildNumber(),
                    execution.getCommitSha(),
                    task == null ? null : task.getBranch(),
                    task == null ? null : task.getEnvironment(),
                    execution.getStatus(),
                    execution.getStartedAt(),
                    execution.getFinishedAt(),
                    execution.getDurationMs(),
                    execution.getConsoleLogRef(),
                    execution.getSyncFailureMessage(),
                    execution.getSyncFailureCount(),
                    execution.getTriggeredBy(),
                    execution.getTriggerMethod(),
                    report == null ? null : AutomationDtos.TestReportResponse.from(report)
            );
        }
    }

    public record Summary(
            int total,
            int passed,
            int failed,
            int broken,
            int skipped,
            int executionCount
    ) {
    }

    public record Trend(
            LocalDate date,
            int total,
            int passed,
            int failed,
            int broken,
            int skipped,
            double passRate
    ) {
    }

    public record CenterResponse(
            List<ExecutionView> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            Summary summary,
            List<Trend> trend
    ) {
    }
}
