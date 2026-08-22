package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AutomationOnboardingStepRepository extends MybatisRepository<AutomationOnboardingStep, Long> {

    public AutomationOnboardingStepRepository(AutomationOnboardingStepMapper mapper) {
        super(mapper, AutomationOnboardingStep.class, AutomationOnboardingStep::getId);
    }

    public List<AutomationOnboardingStep> findAllByOnboardingIdOrderByIdAsc(String onboardingId) {
        return list(Wrappers.<AutomationOnboardingStep>lambdaQuery()
                .eq(AutomationOnboardingStep::getOnboardingId, onboardingId)
                .orderByAsc(AutomationOnboardingStep::getId));
    }

    public Optional<AutomationOnboardingStep> findByOnboardingIdAndStepKey(String onboardingId, String stepKey) {
        return findOne(Wrappers.<AutomationOnboardingStep>lambdaQuery()
                .eq(AutomationOnboardingStep::getOnboardingId, onboardingId)
                .eq(AutomationOnboardingStep::getStepKey, stepKey));
    }
}
