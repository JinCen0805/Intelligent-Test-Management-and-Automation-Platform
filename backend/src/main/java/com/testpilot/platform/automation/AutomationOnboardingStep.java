package com.testpilot.platform.automation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "automation_onboarding_steps")
public class AutomationOnboardingStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "onboarding_id", nullable = false, length = 64)
    private String onboardingId;

    @Column(name = "step_key", nullable = false, length = 64)
    private String stepKey;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "input_summary", columnDefinition = "text")
    private String inputSummary;

    @Column(name = "output_summary", columnDefinition = "text")
    private String outputSummary;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
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
