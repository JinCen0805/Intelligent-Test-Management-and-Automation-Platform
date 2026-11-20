package com.testpilot.platform.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * AI 生成/分析异步任务编排。
 *
 * <p>提交方法只做权限校验 + 落一条 RUNNING 任务并立即返回 taskId；真正的 AI 调用在
 * {@code aiTaskExecutor} 线程里执行（各 {@code *Service} 只在落库阶段开短事务，外部 AI 调用
 * 不占用数据库连接），完成后回写结果或错误。前端轮询 {@link #status} 直到终态。
 */
@Service
public class AiTaskService {

    private final AiTaskRepository aiTaskRepository;
    private final ProjectAccessService projectAccessService;
    private final UserRepository userRepository;
    private final AiGenerationService aiGenerationService;
    private final AiAutomationTaskService aiAutomationTaskService;
    private final AiFailureAnalysisService aiFailureAnalysisService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;
    private final int maxRetries;
    private final int timeoutMinutes;

    public AiTaskService(
            AiTaskRepository aiTaskRepository,
            ProjectAccessService projectAccessService,
            UserRepository userRepository,
            AiGenerationService aiGenerationService,
            AiAutomationTaskService aiAutomationTaskService,
            AiFailureAnalysisService aiFailureAnalysisService,
            ObjectMapper objectMapper,
            @Qualifier("aiTaskExecutor") TaskExecutor taskExecutor,
            @Value("${testpilot.ai.task-max-retries:1}") int maxRetries,
            @Value("${testpilot.ai.task-timeout-minutes:15}") int timeoutMinutes
    ) {
        this.aiTaskRepository = aiTaskRepository;
        this.projectAccessService = projectAccessService;
        this.userRepository = userRepository;
        this.aiGenerationService = aiGenerationService;
        this.aiAutomationTaskService = aiAutomationTaskService;
        this.aiFailureAnalysisService = aiFailureAnalysisService;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.maxRetries = Math.max(0, maxRetries);
        this.timeoutMinutes = Math.max(1, timeoutMinutes);
    }

    @Transactional
    public AiTaskDtos.SubmitResponse submitTestCases(
            Long projectId,
            Long requirementId,
            AiDtos.GenerateRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AI_GENERATE);
        AiTask task = aiTaskRepository.save(new AiTask(projectId, user.getId(), "TEST_CASE_GENERATION"));
        dispatchAfterCommit(task.getId(), () -> runGeneration(task.getId(), projectId, requirementId, request, user.getId()));
        return new AiTaskDtos.SubmitResponse(task.getId(), task.getStatus());
    }

    @Transactional
    public AiTaskDtos.SubmitResponse submitAutomationTask(
            Long projectId,
            AiAutomationDtos.GenerateRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AI_GENERATE);
        AiTask task = aiTaskRepository.save(new AiTask(projectId, user.getId(), "AUTOMATION_TASK_GENERATION"));
        dispatchAfterCommit(task.getId(), () -> runAutomationTask(task.getId(), projectId, request, user.getId()));
        return new AiTaskDtos.SubmitResponse(task.getId(), task.getStatus());
    }

    @Transactional
    public AiTaskDtos.SubmitResponse submitAnalysis(
            Long projectId,
            Long executionId,
            AiAnalysisDtos.AnalyzeRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AI_ANALYZE);
        AiTask task = aiTaskRepository.save(new AiTask(projectId, user.getId(), "FAILURE_ANALYSIS"));
        dispatchAfterCommit(task.getId(), () -> runAnalysis(task.getId(), projectId, executionId, request, user.getId()));
        return new AiTaskDtos.SubmitResponse(task.getId(), task.getStatus());
    }

    @Transactional(readOnly = true)
    public AiTaskDtos.StatusResponse status(String taskId, User user) {
        AiTask task = aiTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(40465, "AI 任务不存在"));
        String permission = "FAILURE_ANALYSIS".equals(task.getType())
                ? RbacPermissions.AI_ANALYZE
                : RbacPermissions.AI_GENERATE;
        projectAccessService.requirePermission(task.getProjectId(), user, permission);
        return AiTaskDtos.StatusResponse.from(task, objectMapper);
    }

    private void dispatchAfterCommit(String taskId, Runnable work) {
        Runnable dispatch = () -> {
            try {
                taskExecutor.execute(work);
            } catch (RuntimeException exception) {
                completeFailure(taskId, 503, "AI 任务队列已满，请稍后重试");
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

    private void runGeneration(
            String taskId,
            Long projectId,
            Long requirementId,
            AiDtos.GenerateRequest request,
            Long userId
    ) {
        User user = resolveUser(userId, taskId);
        if (user == null) {
            return;
        }
        try {
            AiDtos.GenerateResponse result = aiGenerationService.generate(projectId, requirementId, request, user);
            completeSuccess(taskId, objectMapper.writeValueAsString(result));
        } catch (BusinessException exception) {
            completeFailure(taskId, exception.getCode(), exception.getMessage());
        } catch (Exception exception) {
            retryOrFail(taskId, () -> runGeneration(taskId, projectId, requirementId, request, userId),
                    "AI 生成测试用例失败");
        }
    }

    private void runAutomationTask(
            String taskId,
            Long projectId,
            AiAutomationDtos.GenerateRequest request,
            Long userId
    ) {
        User user = resolveUser(userId, taskId);
        if (user == null) {
            return;
        }
        try {
            AiAutomationDtos.DraftResponse result = aiAutomationTaskService.generate(projectId, request, user);
            completeSuccess(taskId, objectMapper.writeValueAsString(result));
        } catch (BusinessException exception) {
            completeFailure(taskId, exception.getCode(), exception.getMessage());
        } catch (Exception exception) {
            retryOrFail(taskId, () -> runAutomationTask(taskId, projectId, request, userId),
                    "AI 生成自动化任务配置失败");
        }
    }

    private void runAnalysis(
            String taskId,
            Long projectId,
            Long executionId,
            AiAnalysisDtos.AnalyzeRequest request,
            Long userId
    ) {
        User user = resolveUser(userId, taskId);
        if (user == null) {
            return;
        }
        try {
            AiAnalysisDtos.AnalysisResponse result = aiFailureAnalysisService.analyze(
                    projectId, executionId, request, user);
            completeSuccess(taskId, objectMapper.writeValueAsString(result));
        } catch (BusinessException exception) {
            completeFailure(taskId, exception.getCode(), exception.getMessage());
        } catch (Exception exception) {
            retryOrFail(taskId, () -> runAnalysis(taskId, projectId, executionId, request, userId),
                    "AI 失败分析失败");
        }
    }

    private User resolveUser(Long userId, String taskId) {
        return userRepository.findById(userId).orElseGet(() -> {
            completeFailure(taskId, 404, "用户不存在");
            return null;
        });
    }

    private void completeSuccess(String taskId, String resultJson) {
        aiTaskRepository.findById(taskId).ifPresent(task -> {
            if ("RUNNING".equals(task.getStatus())) {
                task.success(resultJson);
                aiTaskRepository.save(task);
            }
        });
    }

    private void completeFailure(String taskId, int code, String message) {
        aiTaskRepository.findById(taskId).ifPresent(task -> {
            if ("RUNNING".equals(task.getStatus())) {
                task.fail(code, message);
                aiTaskRepository.save(task);
            }
        });
    }

    private void retryOrFail(String taskId, Runnable work, String message) {
        aiTaskRepository.findById(taskId).ifPresent(task -> {
            if (task.getRetryCount() < maxRetries && "RUNNING".equals(task.getStatus())) {
                task.incrementRetry();
                aiTaskRepository.save(task);
                dispatchAfterCommit(taskId, work);
            } else {
                completeFailure(taskId, 500, message);
            }
        });
    }

    @Scheduled(fixedDelayString = "${testpilot.ai.task-timeout-scan-ms:60000}")
    public void markTimedOutTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minus(Duration.ofMinutes(timeoutMinutes));
        aiTaskRepository.findAllRunning().stream()
                .filter(task -> task.getStartedAt() != null && task.getStartedAt().isBefore(cutoff))
                .forEach(task -> completeFailure(task.getId(), 408, "AI 任务执行超时，请稍后重试"));
    }
}
