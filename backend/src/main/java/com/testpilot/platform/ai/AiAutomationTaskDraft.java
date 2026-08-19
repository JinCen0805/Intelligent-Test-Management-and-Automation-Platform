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
@Table(name = "ai_automation_task_drafts")
public class AiAutomationTaskDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    @Column(name = "automation_type", nullable = false, length = 16)
    private String automationType;

    @Column(name = "repository_url", nullable = false, length = 500)
    private String repositoryUrl;

    @Column(nullable = false, length = 128)
    private String branch;

    @Column(nullable = false, length = 64)
    private String framework;

    @Column(name = "test_path", length = 255)
    private String testPath;

    @Column(name = "execute_command", length = 1000)
    private String executeCommand;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(name = "parameters_json", columnDefinition = "text")
    private String parametersJson;

    @Column(name = "pipeline_type", length = 32)
    private String pipelineType;

    @Column(name = "allure_enabled", nullable = false)
    private boolean allureEnabled;

    @Column(name = "jenkins_job", length = 255)
    private String jenkinsJob;

    @Column(name = "missing_items_json", columnDefinition = "text")
    private String missingItemsJson;

    @Column(name = "risk_hints_json", columnDefinition = "text")
    private String riskHintsJson;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(length = 64)
    private String provider;

    @Column(name = "prompt_name", length = 128)
    private String promptName;

    @Column(name = "prompt_version", length = 32)
    private String promptVersion;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
