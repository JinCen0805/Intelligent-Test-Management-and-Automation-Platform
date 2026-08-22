package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("automation_projects")
public class AutomationProject {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String name;
    private String automationType;
    private String repositoryUrl;
    private String branch;
    private String credentialRef;
    private String framework;
    private String testPath;
    private String executeCommand;
    private String environment;
    private String jenkinsJob;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected AutomationProject() {
    }

    public AutomationProject(
            Long projectId,
            String name,
            String automationType,
            String repositoryUrl,
            String branch,
            String credentialRef,
            String framework,
            String testPath,
            String executeCommand,
            String environment,
            String jenkinsJob
    ) {
        this.projectId = projectId;
        this.name = name;
        this.automationType = automationType;
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.credentialRef = credentialRef;
        this.framework = framework;
        this.testPath = testPath;
        this.executeCommand = executeCommand;
        this.environment = environment;
        this.jenkinsJob = jenkinsJob;
        this.status = "READY";
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
    public Long getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getAutomationType() { return automationType; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public String getBranch() { return branch; }
    public String getCredentialRef() { return credentialRef; }
    public String getFramework() { return framework; }
    public String getTestPath() { return testPath; }
    public String getExecuteCommand() { return executeCommand; }
    public String getEnvironment() { return environment; }
    public String getJenkinsJob() { return jenkinsJob; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setName(String name) { this.name = name; }
    public void setAutomationType(String automationType) { this.automationType = automationType; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    public void setBranch(String branch) { this.branch = branch; }
    public void setCredentialRef(String credentialRef) { this.credentialRef = credentialRef; }
    public void setFramework(String framework) { this.framework = framework; }
    public void setTestPath(String testPath) { this.testPath = testPath; }
    public void setExecuteCommand(String executeCommand) { this.executeCommand = executeCommand; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public void setJenkinsJob(String jenkinsJob) { this.jenkinsJob = jenkinsJob; }
}

