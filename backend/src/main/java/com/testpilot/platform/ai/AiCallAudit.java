package com.testpilot.platform.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 通用 AI 调用审计记录，覆盖用例生成、失败分析和自动化任务生成三类调用。
 *
 * <p>记录发起用户、项目、模型、Prompt、任务类型、时间与调用状态，供管理员回溯。
 */
@Entity
@Table(name = "ai_call_audits")
public class AiCallAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", length = 64)
    private String userName;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(length = 64)
    private String provider;

    @Column(name = "prompt_id")
    private Long promptId;

    @Column(name = "prompt_name", length = 128)
    private String promptName;

    @Column(name = "prompt_version", length = 32)
    private String promptVersion;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at", nullable = false)
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

    @PrePersist
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
