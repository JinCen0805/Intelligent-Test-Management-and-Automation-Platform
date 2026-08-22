package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AutomationOnboardingRepository extends MybatisRepository<AutomationOnboarding, String> {

    public AutomationOnboardingRepository(AutomationOnboardingMapper mapper) {
        super(mapper, AutomationOnboarding.class, AutomationOnboarding::getId);
    }

    public Optional<AutomationOnboarding> findByIdAndProjectId(String id, Long projectId) {
        return findOne(Wrappers.<AutomationOnboarding>lambdaQuery()
                .eq(AutomationOnboarding::getId, id)
                .eq(AutomationOnboarding::getProjectId, projectId));
    }

    public Optional<AutomationOnboarding> findFirstByAutomationProjectIdAndStatusInOrderByCreatedAtDesc(
            Long automationProjectId, List<String> statuses) {
        return findFirst(Wrappers.<AutomationOnboarding>lambdaQuery()
                .eq(AutomationOnboarding::getAutomationProjectId, automationProjectId)
                .in(AutomationOnboarding::getStatus, statuses)
                .orderByDesc(AutomationOnboarding::getCreatedAt));
    }
}
