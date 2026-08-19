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
import java.util.UUID;

@Entity
@Table(name = "automation_onboardings")
public class AutomationOnboarding {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "automation_project_id", nullable = false)
    private Long automationProjectId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "current_step", length = 64)
    private String currentStep;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_code", length = 32)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AutomationOnboarding() {
    }

    public AutomationOnboarding(Long projectId, Long automationProjectId) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.automationProjectId = automationProjectId;
        this.status = "CREATED";
        this.retryCount = 0;
    }

    public void queue() {
        this.status = "QUEUED";
    }

    public void start(String step) {
        this.status = "RUNNING";
        this.currentStep = step;
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    public void moveTo(String step) {
        this.currentStep = step;
    }

    public void ready() {
        this.status = "READY";
        this.currentStep = "DONE";
        this.finishedAt = LocalDateTime.now();
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void fail(String code, String message) {
        this.status = "FAILED";
        this.errorCode = code;
        this.errorMessage = message;
        this.finishedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = "CANCELED";
        this.finishedAt = LocalDateTime.now();
    }

    public void retry() {
        this.retryCount++;
        this.status = "QUEUED";
        this.errorCode = null;
        this.errorMessage = null;
        this.finishedAt = null;
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

    public String getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getAutomationProjectId() { return automationProjectId; }
    public String getStatus() { return status; }
    public String getCurrentStep() { return currentStep; }
    public int getRetryCount() { return retryCount; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
