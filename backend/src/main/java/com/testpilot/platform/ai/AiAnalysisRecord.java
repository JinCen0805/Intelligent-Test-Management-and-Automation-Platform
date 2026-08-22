package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ai_analysis_records")
public class AiAnalysisRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long executionId;
    private Long projectId;
    private Long testTaskId;
    private Long modelId;
    private String modelName;
    private String provider;
    private Long promptId;
    private String promptName;
    private String promptVersion;
    private String status;
    private String category;
    private String resultJson;
    private String errorMessage;
    private Long createdBy;
    private LocalDateTime createdAt;
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
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
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

