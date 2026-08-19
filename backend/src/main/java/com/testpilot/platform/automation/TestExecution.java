package com.testpilot.platform.automation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_executions")
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "test_task_id", nullable = false)
    private Long testTaskId;

    @Column(name = "automation_project_id", nullable = false)
    private Long automationProjectId;

    @Column(name = "jenkins_job", nullable = false, length = 255)
    private String jenkinsJob;

    @Column(name = "queue_url", length = 1000)
    private String queueUrl;

    @Column(name = "build_number", length = 64)
    private String buildNumber;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "console_log_ref", length = 500)
    private String consoleLogRef;

    @Column(name = "sync_failure_message", length = 1000)
    private String syncFailureMessage;

    @Column(name = "sync_failure_count", nullable = false)
    private int syncFailureCount;

    @Column(name = "triggered_by", length = 64)
    private String triggeredBy;

    @Column(name = "trigger_method", nullable = false, length = 16)
    private String triggerMethod = "MANUAL";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
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
