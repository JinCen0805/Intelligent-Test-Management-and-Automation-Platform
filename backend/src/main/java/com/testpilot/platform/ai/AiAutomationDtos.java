package com.testpilot.platform.ai;

import com.testpilot.platform.automation.AutomationDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class AiAutomationDtos {

    private AiAutomationDtos() {
    }

    public record GenerateRequest(
            @NotBlank(message = "Git 仓库地址不能为空")
            @Size(max = 500, message = "仓库地址长度不能超过 500 个字符")
            String repositoryUrl,
            @NotBlank(message = "Branch 不能为空")
            @Size(max = 128, message = "Branch 长度不能超过 128 个字符")
            String branch,
            @NotBlank(message = "自然语言描述不能为空")
            @Size(max = 2000, message = "描述长度不能超过 2000 个字符")
            String description,
            Long modelId,
            Long promptId
    ) {
    }

    public record DraftUpdateRequest(
            @NotBlank(message = "任务名称不能为空")
            @Size(max = 128, message = "任务名称长度不能超过 128 个字符")
            String name,
            @NotBlank(message = "任务类型不能为空")
            String taskType,
            @NotBlank(message = "自动化类型不能为空")
            String automationType,
            @NotBlank(message = "Git 仓库地址不能为空")
            @Size(max = 500, message = "仓库地址长度不能超过 500 个字符")
            String repositoryUrl,
            @NotBlank(message = "Branch 不能为空")
            @Size(max = 128, message = "Branch 长度不能超过 128 个字符")
            String branch,
            @NotBlank(message = "Framework 不能为空")
            String framework,
            String testPath,
            String executeCommand,
            @NotBlank(message = "执行环境不能为空")
            String environment,
            String parametersJson,
            String pipelineType,
            String jenkinsJob,
            boolean allureEnabled
    ) {
    }

    public record DraftResponse(
            Long id,
            Long projectId,
            String name,
            String taskType,
            String automationType,
            String repositoryUrl,
            String branch,
            String framework,
            String testPath,
            String executeCommand,
            String environment,
            String parametersJson,
            String pipelineType,
            String jenkinsJob,
            boolean allureEnabled,
            String allureConfig,
            List<String> missingItems,
            List<String> riskHints,
            String status,
            String modelName,
            String provider,
            String promptName,
            String promptVersion,
            LocalDateTime createdAt,
            String disclaimer
    ) {
        static DraftResponse from(AiAutomationTaskDraft draft, String allureConfig,
                                  List<String> missingItems, List<String> riskHints, String disclaimer) {
            return new DraftResponse(
                    draft.getId(),
                    draft.getProjectId(),
                    draft.getName(),
                    draft.getTaskType(),
                    draft.getAutomationType(),
                    draft.getRepositoryUrl(),
                    draft.getBranch(),
                    draft.getFramework(),
                    draft.getTestPath(),
                    draft.getExecuteCommand(),
                    draft.getEnvironment(),
                    draft.getParametersJson(),
                    draft.getPipelineType(),
                    draft.getJenkinsJob(),
                    draft.isAllureEnabled(),
                    allureConfig,
                    missingItems,
                    riskHints,
                    draft.getStatus(),
                    draft.getModelName(),
                    draft.getProvider(),
                    draft.getPromptName(),
                    draft.getPromptVersion(),
                    draft.getCreatedAt(),
                    disclaimer
            );
        }
    }

    public record ConfirmResponse(
            Long draftId,
            Long automationProjectId,
            Long testTaskId,
            AutomationDtos.TestTaskResponse task,
            String disclaimer
    ) {
    }
}
