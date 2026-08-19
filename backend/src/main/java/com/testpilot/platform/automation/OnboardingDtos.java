package com.testpilot.platform.automation;

import java.time.LocalDateTime;
import java.util.List;

public final class OnboardingDtos {

    private OnboardingDtos() {
    }

    public record StartResponse(String onboardingId, String status) {
    }

    public record StepResponse(
            Long id,
            String stepKey,
            String status,
            String inputSummary,
            String outputSummary,
            String errorMessage,
            int retryCount,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        static StepResponse from(AutomationOnboardingStep step) {
            return new StepResponse(
                    step.getId(), step.getStepKey(), step.getStatus(), step.getInputSummary(),
                    step.getOutputSummary(), step.getErrorMessage(), step.getRetryCount(),
                    step.getStartedAt(), step.getFinishedAt()
            );
        }
    }

    public record StatusResponse(
            String onboardingId,
            Long automationProjectId,
            String status,
            String currentStep,
            int retryCount,
            String errorCode,
            String errorMessage,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            List<StepResponse> steps
    ) {
        static StatusResponse from(AutomationOnboarding onboarding, List<AutomationOnboardingStep> steps) {
            return new StatusResponse(
                    onboarding.getId(),
                    onboarding.getAutomationProjectId(),
                    onboarding.getStatus(),
                    onboarding.getCurrentStep(),
                    onboarding.getRetryCount(),
                    onboarding.getErrorCode(),
                    onboarding.getErrorMessage(),
                    onboarding.getStartedAt(),
                    onboarding.getFinishedAt(),
                    steps.stream().map(StepResponse::from).toList()
            );
        }
    }
}
