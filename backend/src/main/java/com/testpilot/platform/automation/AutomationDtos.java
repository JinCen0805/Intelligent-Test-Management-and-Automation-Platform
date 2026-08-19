package com.testpilot.platform.automation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class AutomationDtos {

    private AutomationDtos() {
    }

    public record AutomationProjectRequest(
            @NotBlank(message = "自动化项目名称不能为空")
            @Size(max = 128, message = "名称长度不能超过 128 个字符")
            String name,
            @NotBlank(message = "自动化类型不能为空")
            String automationType,
            @NotBlank(message = "Git 仓库地址不能为空")
            @Size(max = 500, message = "仓库地址长度不能超过 500 个字符")
            String repositoryUrl,
            @NotBlank(message = "Branch 不能为空")
            @Size(max = 128, message = "Branch 长度不能超过 128 个字符")
            String branch,
            @Size(max = 128, message = "凭证引用长度不能超过 128 个字符")
            String credentialRef,
            @NotBlank(message = "Framework 不能为空")
            String framework,
            String testPath,
            String executeCommand,
            String environment,
            String jenkinsJob
    ) {
    }

    public record AutomationProjectResponse(
            Long id,
            Long projectId,
            String name,
            String automationType,
            String repositoryUrl,
            String branch,
            String credentialRef,
            String framework,
            String testPath,
            String executeCommand,
            String environment,
            String jenkinsJob,
            String status
    ) {
        public static AutomationProjectResponse from(AutomationProject project) {
            return new AutomationProjectResponse(
                    project.getId(),
                    project.getProjectId(),
                    project.getName(),
                    project.getAutomationType(),
                    project.getRepositoryUrl(),
                    project.getBranch(),
                    project.getCredentialRef(),
                    project.getFramework(),
                    project.getTestPath(),
                    project.getExecuteCommand(),
                    project.getEnvironment(),
                    project.getJenkinsJob(),
                    project.getStatus()
            );
        }
    }

    public record TestTaskRequest(
            @NotBlank(message = "测试任务名称不能为空")
            @Size(max = 128, message = "名称长度不能超过 128 个字符")
            String name,
            @NotBlank(message = "任务类型不能为空")
            String taskType,
            Long automationProjectId,
            Long pipelineBindingId,
            String parametersJson
    ) {
    }

    public record ExecuteRequest(
            String parametersJson
    ) {
    }

    public record TestTaskStatusRequest(
            @NotBlank(message = "任务状态不能为空")
            String status
    ) {
    }

    public record TestTaskResponse(
            Long id,
            Long projectId,
            String name,
            String taskType,
            Long automationProjectId,
            Long pipelineBindingId,
            String jenkinsJob,
            String parametersJson,
            String status
    ) {
        public static TestTaskResponse from(TestTask task) {
            return new TestTaskResponse(
                    task.getId(),
                    task.getProjectId(),
                    task.getName(),
                    task.getTaskType(),
                    task.getAutomationProjectId(),
                    task.getPipelineBindingId(),
                    task.getJenkinsJob(),
                    task.getParametersJson(),
                    task.getStatus()
            );
        }
    }

    public record ExecutionResultRequest(
            @NotBlank(message = "执行状态不能为空")
            String status,
            String commitSha,
            String consoleLogRef,
            String allureUrl,
            @Min(value = 0, message = "总数不能为负数")
            int totalCount,
            @Min(value = 0, message = "通过数不能为负数")
            int passedCount,
            @Min(value = 0, message = "失败数不能为负数")
            int failedCount,
            @Min(value = 0, message = "Broken 数不能为负数")
            int brokenCount,
            @Min(value = 0, message = "跳过数不能为负数")
            int skippedCount,
            String artifactRef,
            String failureDetail,
            String screenshotRef,
            String videoRef,
            String aiAnalysis
    ) {
    }

    public record TestExecutionResponse(
            Long id,
            Long projectId,
            Long testTaskId,
            Long automationProjectId,
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
            String triggeredBy,
            String triggerMethod,
            TestReportResponse report
    ) {
        public static TestExecutionResponse from(TestExecution execution, TestReport report) {
            return new TestExecutionResponse(
                    execution.getId(),
                    execution.getProjectId(),
                    execution.getTestTaskId(),
                    execution.getAutomationProjectId(),
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
                    execution.getTriggeredBy(),
                    execution.getTriggerMethod(),
                    report == null ? null : TestReportResponse.from(report)
            );
        }
    }

    public record TestReportResponse(
            Long id,
            Long executionId,
            String allureUrl,
            int totalCount,
            int passedCount,
            int failedCount,
            int brokenCount,
            int skippedCount,
            String artifactRef,
            String failureDetail,
            String screenshotRef,
            String videoRef,
            String aiAnalysis
    ) {
        public static TestReportResponse from(TestReport report) {
            return new TestReportResponse(
                    report.getId(),
                    report.getExecutionId(),
                    report.getAllureUrl(),
                    report.getTotalCount(),
                    report.getPassedCount(),
                    report.getFailedCount(),
                    report.getBrokenCount(),
                    report.getSkippedCount(),
                    report.getArtifactRef(),
                    report.getFailureDetail(),
                    report.getScreenshotRef(),
                    report.getVideoRef(),
                    report.getAiAnalysis()
            );
        }
    }
}
