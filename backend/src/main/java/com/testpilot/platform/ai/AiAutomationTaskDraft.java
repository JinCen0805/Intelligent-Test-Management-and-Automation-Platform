package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ai_automation_task_drafts")
public class AiAutomationTaskDraft {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String name;
    private String taskType;
    private String automationType;
    private String repositoryUrl;
    private String branch;
    private String framework;
    private String testPath;
    private String executeCommand;
    private String environment;
    private String parametersJson;
    private String pipelineType;
    private boolean allureEnabled;
    private String jenkinsJob;
    private String missingItemsJson;
    private String riskHintsJson;
    private String status;
    private String modelName;
    private String provider;
    private String promptName;
    private String promptVersion;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected AiAutomationTaskDraft() {
    }

    public AiAutomationTaskDraft(
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
            boolean allureEnabled,
            String jenkinsJob,
            String missingItemsJson,
            String riskHintsJson,
            String modelName,
            String provider,
            String promptName,
            String promptVersion,
            Long createdBy
    ) {
        this.projectId = projectId;
        this.name = name;
        this.taskType = taskType;
        this.automationType = automationType;
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.framework = framework;
        this.testPath = testPath;
        this.executeCommand = executeCommand;
        this.environment = environment;
        this.parametersJson = parametersJson;
        this.pipelineType = pipelineType;
        this.allureEnabled = allureEnabled;
        this.jenkinsJob = jenkinsJob;
        this.missingItemsJson = missingItemsJson;
        this.riskHintsJson = riskHintsJson;
        this.status = "DRAFT";
        this.modelName = modelName;
        this.provider = provider;
        this.promptName = promptName;
        this.promptVersion = promptVersion;
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

    public void update(
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
            boolean allureEnabled,
            String jenkinsJob
    ) {
        this.name = name;
        this.taskType = taskType;
        this.automationType = automationType;
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.framework = framework;
        this.testPath = testPath;
        this.executeCommand = executeCommand;
        this.environment = environment;
        this.parametersJson = parametersJson;
        this.pipelineType = pipelineType;
        this.allureEnabled = allureEnabled;
        this.jenkinsJob = jenkinsJob;
    }

    public void markConfirmed() {
        this.status = "CONFIRMED";
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getTaskType() { return taskType; }
    public String getAutomationType() { return automationType; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public String getBranch() { return branch; }
    public String getFramework() { return framework; }
    public String getTestPath() { return testPath; }
    public String getExecuteCommand() { return executeCommand; }
    public String getEnvironment() { return environment; }
    public String getParametersJson() { return parametersJson; }
    public String getPipelineType() { return pipelineType; }
    public boolean isAllureEnabled() { return allureEnabled; }
    public String getJenkinsJob() { return jenkinsJob; }
    public String getMissingItemsJson() { return missingItemsJson; }
    public String getRiskHintsJson() { return riskHintsJson; }
    public String getStatus() { return status; }
    public String getModelName() { return modelName; }
    public String getProvider() { return provider; }
    public String getPromptName() { return promptName; }
    public String getPromptVersion() { return promptVersion; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

