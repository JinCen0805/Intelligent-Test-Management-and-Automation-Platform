package com.testpilot.platform.automation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationOnboardingStepRepository extends JpaRepository<AutomationOnboardingStep, Long> {
    List<AutomationOnboardingStep> findAllByOnboardingIdOrderByIdAsc(String onboardingId);
    Optional<AutomationOnboardingStep> findByOnboardingIdAndStepKey(String onboardingId, String stepKey);
}
