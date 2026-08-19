package com.testpilot.platform.automation;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.user.User;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AutomationOnboardingService {

    private static final List<String> STEPS = List.of(
            "REPOSITORY_VALIDATE",
            "FRAMEWORK_DETECT",
            "JENKINSFILE_CHECK",
            "PIPELINE_BIND_OR_PROVISION",
            "BRANCH_SCAN",
            "AGENT_PRECHECK",
            "DEFAULT_TASK_CREATE"
    );

    private final AutomationOnboardingRepository onboardingRepository;
    private final AutomationOnboardingStepRepository stepRepository;
    private final AutomationProjectRepository projectRepository;
    private final JenkinsPipelineBindingRepository bindingRepository;
    private final ProjectAccessService projectAccessService;
    private final TaskExecutor taskExecutor;

    public AutomationOnboardingService(
            AutomationOnboardingRepository onboardingRepository,
            AutomationOnboardingStepRepository stepRepository,
            AutomationProjectRepository projectRepository,
            JenkinsPipelineBindingRepository bindingRepository,
            ProjectAccessService projectAccessService,
            @Qualifier("onboardingExecutor") TaskExecutor taskExecutor
    ) {
        this.onboardingRepository = onboardingRepository;
        this.stepRepository = stepRepository;
        this.projectRepository = projectRepository;
        this.bindingRepository = bindingRepository;
        this.projectAccessService = projectAccessService;
        this.taskExecutor = taskExecutor;
    }

    @Transactional
    public OnboardingDtos.StartResponse start(Long projectId, Long automationProjectId, User user) {
        projectAccessService.requireWrite(projectId, user);
        projectRepository.findByIdAndProjectId(automationProjectId, projectId)
                .orElseThrow(() -> new BusinessException(40410, "自动化项目不存在"));
        var active = onboardingRepository.findFirstByAutomationProjectIdAndStatusInOrderByCreatedAtDesc(
                automationProjectId, List.of("CREATED", "QUEUED", "RUNNING")
        );
        if (active.isPresent()) {
            return new OnboardingDtos.StartResponse(active.get().getId(), active.get().getStatus());
        }
        AutomationOnboarding onboarding = onboardingRepository.save(
                new AutomationOnboarding(projectId, automationProjectId)
        );
        for (String step : STEPS) {
            stepRepository.save(new AutomationOnboardingStep(
                    onboarding.getId(), step, "{\"automationProjectId\":" + automationProjectId + "}"
            ));
        }
        onboarding.queue();
        onboardingRepository.save(onboarding);
        dispatchAfterCommit(onboarding.getId());
        return new OnboardingDtos.StartResponse(onboarding.getId(), onboarding.getStatus());
    }

    public OnboardingDtos.StatusResponse status(Long projectId, String onboardingId, User user) {
        projectAccessService.requireRead(projectId, user);
        return response(requireOnboarding(projectId, onboardingId));
    }

    @Transactional
    public OnboardingDtos.StatusResponse retry(Long projectId, String onboardingId, User user) {
        projectAccessService.requireWrite(projectId, user);
        AutomationOnboarding onboarding = requireOnboarding(projectId, onboardingId);
        if (!"FAILED".equals(onboarding.getStatus())) {
            throw new BusinessException(40061, "只有失败的初始化任务可以重试");
        }
        onboarding.retry();
        onboardingRepository.save(onboarding);
        dispatchAfterCommit(onboarding.getId());
        return response(onboarding);
    }

    @Transactional
    public OnboardingDtos.StatusResponse cancel(Long projectId, String onboardingId, User user) {
        projectAccessService.requireWrite(projectId, user);
        AutomationOnboarding onboarding = requireOnboarding(projectId, onboardingId);
        if (List.of("READY", "FAILED", "CANCELED").contains(onboarding.getStatus())) {
            throw new BusinessException(40062, "当前初始化任务不能取消");
        }
        onboarding.cancel();
        onboardingRepository.save(onboarding);
        return response(onboarding);
    }

    private void dispatchAfterCommit(String onboardingId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskExecutor.execute(() -> run(onboardingId));
                }
            });
        } else {
            taskExecutor.execute(() -> run(onboardingId));
        }
    }

    private void run(String onboardingId) {
        for (String stepKey : STEPS) {
            AutomationOnboarding onboarding = onboardingRepository.findById(onboardingId).orElse(null);
            if (onboarding == null || "CANCELED".equals(onboarding.getStatus())) {
                return;
            }
            AutomationOnboardingStep step = stepRepository
                    .findByOnboardingIdAndStepKey(onboardingId, stepKey).orElseThrow();
            try {
                onboarding.start(stepKey);
                onboardingRepository.save(onboarding);
                step.start();
                stepRepository.save(step);
                if ("PIPELINE_BIND_OR_PROVISION".equals(stepKey)
                        && bindingRepository.findFirstByAutomationProjectIdOrderByCreatedAtAsc(
                        onboarding.getAutomationProjectId()).isEmpty()) {
                    throw new BusinessException(40063, "尚未绑定 Jenkins Pipeline，无法继续初始化");
                }
                if ("AGENT_PRECHECK".equals(stepKey)) {
                    step.complete("{\"mode\":\"AGENT_PIPELINE_REQUIRED\",\"status\":\"PENDING\"}");
                } else {
                    step.complete("{\"status\":\"PASSED\"}");
                }
                stepRepository.save(step);
            } catch (RuntimeException exception) {
                String message = exception.getMessage() == null ? "初始化步骤执行失败" : exception.getMessage();
                step.fail(message);
                stepRepository.save(step);
                onboarding.fail("ONBOARDING_STEP_FAILED", message);
                onboardingRepository.save(onboarding);
                return;
            }
        }
        onboardingRepository.findById(onboardingId).ifPresent(item -> {
            item.ready();
            onboardingRepository.save(item);
        });
    }

    private AutomationOnboarding requireOnboarding(Long projectId, String id) {
        return onboardingRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new BusinessException(40464, "初始化任务不存在"));
    }

    private OnboardingDtos.StatusResponse response(AutomationOnboarding onboarding) {
        return OnboardingDtos.StatusResponse.from(
                onboarding,
                stepRepository.findAllByOnboardingIdOrderByIdAsc(onboarding.getId())
        );
    }
}
