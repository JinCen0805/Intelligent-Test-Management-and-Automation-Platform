package com.testpilot.platform.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis_records")
public class AiAnalysisRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false, unique = true)
    private Long executionId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "test_task_id")
    private Long testTaskId;

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

    @Column(length = 32)
    private String category;

    @Column(name = "result_json", columnDefinition = "text")
    private String resultJson;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AiAnalysisRecord() {
    }

    public AiAnalysisRecord(
            Long executionId,
            Long projectId,
            Long testTaskId,
            Long modelId,
            String modelName,
            String provider,
            Long promptId,
            String promptName,
            String promptVersion,
            String status,
            String category,
            String resultJson,
            String errorMessage,
            Long createdBy
    ) {
        this.executionId = executionId;
        this.projectId = projectId;
        this.testTaskId = testTaskId;
        this.modelId = modelId;
        this.modelName = modelName;
        this.provider = provider;
        this.promptId = promptId;
        this.promptName = promptName;
        this.promptVersion = promptVersion;
        this.status = status;
        this.category = category;
        this.resultJson = resultJson;
        this.errorMessage = errorMessage;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getExecutionId() { return executionId; }
    public Long getProjectId() { return projectId; }
    public Long getTestTaskId() { return testTaskId; }
    public Long getModelId() { return modelId; }
    public String getModelName() { return modelName; }
    public String getProvider() { return provider; }
    public Long getPromptId() { return promptId; }
    public String getPromptName() { return promptName; }
    public String getPromptVersion() { return promptVersion; }
    public String getStatus() { return status; }
    public String getCategory() { return category; }
    public String getResultJson() { return resultJson; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
