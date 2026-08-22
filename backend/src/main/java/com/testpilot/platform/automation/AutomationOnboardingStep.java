package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("automation_onboarding_steps")
public class AutomationOnboardingStep {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String onboardingId;
    private String stepKey;
    private String status;
    private String inputSummary;
    private String outputSummary;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    protected AutomationOnboardingStep() {
    }

    public AutomationOnboardingStep(String onboardingId, String stepKey, String inputSummary) {
        this.onboardingId = onboardingId;
        this.stepKey = stepKey;
        this.status = "PENDING";
        this.inputSummary = inputSummary;
    }

    public void start() {
        status = "RUNNING";
        startedAt = LocalDateTime.now();
    }

    public void complete(String outputSummary) {
        status = "READY";
        this.outputSummary = outputSummary;
        finishedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        status = "FAILED";
        this.errorMessage = errorMessage;
        finishedAt = LocalDateTime.now();
    }

    public void retry() {
        retryCount++;
        status = "PENDING";
        errorMessage = null;
        finishedAt = null;
    }

    public Long getId() { return id; }
    public String getOnboardingId() { return onboardingId; }
    public String getStepKey() { return stepKey; }
    public String getStatus() { return status; }
    public String getInputSummary() { return inputSummary; }
    public String getOutputSummary() { return outputSummary; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}

