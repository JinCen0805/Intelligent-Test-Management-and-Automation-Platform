package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 通用 AI 调用审计记录，覆盖用例生成、失败分析和自动化任务生成三类调用。
 *
 * <p>记录发起用户、项目、模型、Prompt、任务类型、时间与调用状态，供管理员回溯。
 */
@TableName("ai_call_audits")
public class AiCallAudit {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long userId;
    private String userName;
    private String taskType;
    private Long modelId;
    private String modelName;
    private String provider;
    private Long promptId;
    private String promptName;
    private String promptVersion;
    private String status;
    private Integer statusCode;
    private String errorMessage;
    private Long latencyMs;
    private LocalDateTime createdAt;

    protected AiCallAudit() {
    }

    public AiCallAudit(
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
            Long latencyMs
    ) {
        this.projectId = projectId;
        this.userId = userId;
        this.userName = userName;
        this.taskType = taskType;
        this.modelId = modelId;
        this.modelName = modelName;
        this.provider = provider;
        this.promptId = promptId;
        this.promptName = promptName;
        this.promptVersion = promptVersion;
        this.status = status;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.latencyMs = latencyMs;
    }
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getTaskType() { return taskType; }
    public Long getModelId() { return modelId; }
    public String getModelName() { return modelName; }
    public String getProvider() { return provider; }
    public Long getPromptId() { return promptId; }
    public String getPromptName() { return promptName; }
    public String getPromptVersion() { return promptVersion; }
    public String getStatus() { return status; }
    public Integer getStatusCode() { return statusCode; }
    public String getErrorMessage() { return errorMessage; }
    public Long getLatencyMs() { return latencyMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

