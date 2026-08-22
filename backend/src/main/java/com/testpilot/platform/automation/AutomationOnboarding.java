package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.UUID;

@TableName("automation_onboardings")
public class AutomationOnboarding {

    @TableId(type = IdType.INPUT)
    private String id;
    private Long projectId;
    private Long automationProjectId;
    private String status;
    private String currentStep;
    private int retryCount;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
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
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
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

