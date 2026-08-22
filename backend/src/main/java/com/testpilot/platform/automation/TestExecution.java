package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("test_executions")
public class TestExecution {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long testTaskId;
    private Long automationProjectId;
    private String jenkinsJob;
    private String queueUrl;
    private String buildNumber;
    private String commitSha;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
    private String consoleLogRef;
    private String syncFailureMessage;
    private int syncFailureCount;
    private String triggeredBy;
    private String triggerMethod = "MANUAL";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected TestExecution() {
    }

    public TestExecution(
            Long projectId,
            Long testTaskId,
            Long automationProjectId,
            String jenkinsJob,
            String queueUrl,
            String buildNumber,
            String status,
            LocalDateTime startedAt
    ) {
        this.projectId = projectId;
        this.testTaskId = testTaskId;
        this.automationProjectId = automationProjectId;
        this.jenkinsJob = jenkinsJob;
        this.queueUrl = queueUrl;
        this.buildNumber = buildNumber;
        this.status = status;
        this.startedAt = startedAt;
    }
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getTestTaskId() { return testTaskId; }
    public Long getAutomationProjectId() { return automationProjectId; }
    public String getJenkinsJob() { return jenkinsJob; }
    public String getQueueUrl() { return queueUrl; }
    public String getBuildNumber() { return buildNumber; }
    public String getCommitSha() { return commitSha; }
    public String getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public Long getDurationMs() { return durationMs; }
    public String getConsoleLogRef() { return consoleLogRef; }
    public String getSyncFailureMessage() { return syncFailureMessage; }
    public int getSyncFailureCount() { return syncFailureCount; }
    public String getTriggeredBy() { return triggeredBy; }
    public String getTriggerMethod() { return triggerMethod; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void recordTrigger(String triggeredBy, String triggerMethod) {
        this.triggeredBy = triggeredBy;
        this.triggerMethod = triggerMethod == null || triggerMethod.isBlank() ? "MANUAL" : triggerMethod;
    }
    public void applyJenkinsSync(
            String buildNumber,
            String queueUrl,
            String status,
            String commitSha,
            String consoleLogRef,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        this.buildNumber = buildNumber;
        this.queueUrl = queueUrl;
        this.status = status;
        this.commitSha = commitSha;
        this.consoleLogRef = consoleLogRef;
        this.syncFailureMessage = null;
        this.syncFailureCount = 0;
        if (startedAt != null) {
            this.startedAt = startedAt;
        }
        this.finishedAt = finishedAt;
        if (this.startedAt != null && finishedAt != null) {
            this.durationMs = java.time.Duration.between(this.startedAt, finishedAt).toMillis();
        }
    }
    public void recordSyncFailure(String message) {
        this.syncFailureCount++;
        this.syncFailureMessage = message;
    }
    public void clearSyncFailure() {
        this.syncFailureCount = 0;
        this.syncFailureMessage = null;
    }
    public void applyResult(String status, String commitSha, String consoleLogRef, LocalDateTime finishedAt) {
        this.status = status;
        this.commitSha = commitSha;
        this.consoleLogRef = consoleLogRef;
        this.finishedAt = finishedAt;
        if (startedAt != null && finishedAt != null) {
            this.durationMs = java.time.Duration.between(startedAt, finishedAt).toMillis();
        }
    }
}

