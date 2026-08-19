package com.testpilot.platform.automation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationOnboardingRepository extends JpaRepository<AutomationOnboarding, String> {
    Optional<AutomationOnboarding> findByIdAndProjectId(String id, Long projectId);
    Optional<AutomationOnboarding> findFirstByAutomationProjectIdAndStatusInOrderByCreatedAtDesc(
            Long automationProjectId, java.util.List<String> statuses);
}
