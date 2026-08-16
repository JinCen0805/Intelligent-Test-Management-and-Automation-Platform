package com.testpilot.platform.automation;

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
@Table(name = "automation_projects")
public class AutomationProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "automation_type", nullable = false, length = 16)
    private String automationType;

    @Column(name = "repository_url", nullable = false, length = 500)
    private String repositoryUrl;

    @Column(nullable = false, length = 128)
    private String branch;

    @Column(name = "credential_ref", length = 128)
    private String credentialRef;

    @Column(nullable = false, length = 64)
    private String framework;

    @Column(name = "test_path", length = 255)
    private String testPath;

    @Column(name = "execute_command", length = 1000)
    private String executeCommand;

    @Column(length = 32)
    private String environment;

    @Column(name = "jenkins_job", length = 255)
    private String jenkinsJob;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
