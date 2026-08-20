package com.testpilot.platform.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class AiDtos {

    private AiDtos() {
    }

    public record ModelRequest(
            @NotBlank(message = "模型名称不能为空")
            @Size(max = 128, message = "模型名称长度不能超过 128 个字符")
            String name,
            @NotBlank(message = "Provider 不能为空")
            @Size(max = 64, message = "Provider 长度不能超过 64 个字符")
            String provider,
            @Size(max = 500, message = "AI 请求地址长度不能超过 500 个字符")
            String endpoint,
            String apiKey,
            Boolean enabled
    ) {
    }

    public record ModelResponse(
            Long id,
            String name,
            String provider,
            String endpoint,
            boolean hasApiKey,
            boolean enabled,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static ModelResponse from(AiModel model) {
            return new ModelResponse(
                    model.getId(),
                    model.getName(),
                    model.getProvider(),
                    model.getEndpoint(),
                    model.getApiKey() != null && !model.getApiKey().isBlank(),
                    model.isEnabled(),
                    model.getCreatedAt(),
                    model.getUpdatedAt()
            );
        }
    }

    public record PromptRequest(
            @NotBlank(message = "Prompt 名称不能为空")
            @Size(max = 128, message = "名称长度不能超过 128 个字符")
            String name,
            @NotBlank(message = "任务类型不能为空")
            @Size(max = 32, message = "任务类型长度不能超过 32 个字符")
            String taskType,
            @NotBlank(message = "Prompt 内容不能为空")
            String content,
            @NotBlank(message = "版本不能为空")
            @Size(max = 32, message = "版本长度不能超过 32 个字符")
            String version,
            Boolean enabled
    ) {
    }

    public record PromptResponse(
            Long id,
            String name,
            String taskType,
            String content,
            String version,
            boolean enabled
    ) {
        public static PromptResponse from(PromptTemplate prompt) {
            return new PromptResponse(
                    prompt.getId(),
                    prompt.getName(),
                    prompt.getTaskType(),
                    prompt.getContent(),
                    prompt.getVersion(),
                    prompt.isEnabled()
            );
        }
    }

    public record PromptPublishRequest(
            @Size(max = 32, message = "版本长度不能超过 32 个字符")
            String version
    ) {
    }

    public record PromptEnabledRequest(
            @NotNull(message = "enabled 不能为空")
            Boolean enabled
    ) {
    }

    public record ModelEnabledRequest(
            @NotNull(message = "enabled 不能为空")
            Boolean enabled
    ) {
    }

    public record GenerateRequest(
            Long modelId,
            Long promptId,
            @Size(max = 1000, message = "生成规则长度不能超过 1000 个字符")
            String rules
    ) {
    }

    public record GenerateResponse(
            Long requirementId,
            Long modelId,
            String provider,
            List<TestCaseView> testCases
    ) {
    }

    public record TestCaseView(
            Long id,
            String caseNo,
            String title,
            String status
    ) {
    }

    public record AiCallAuditResponse(
            Long id,
            Long projectId,
            Long userId,
            String userName,
            String taskType,
            Long modelId,
            String modelName,
            String provider,
            Long promptId,
            String promptName,
            String promptVersion,
            String status,
            Integer statusCode,
            String errorMessage,
            Long latencyMs,
            LocalDateTime createdAt
    ) {
        public static AiCallAuditResponse from(AiCallAudit audit) {
            return new AiCallAuditResponse(
                    audit.getId(),
                    audit.getProjectId(),
                    audit.getUserId(),
                    audit.getUserName(),
                    audit.getTaskType(),
                    audit.getModelId(),
                    audit.getModelName(),
                    audit.getProvider(),
                    audit.getPromptId(),
                    audit.getPromptName(),
                    audit.getPromptVersion(),
                    audit.getStatus(),
                    audit.getStatusCode(),
                    audit.getErrorMessage(),
                    audit.getLatencyMs(),
                    audit.getCreatedAt()
            );
        }
    }
}
