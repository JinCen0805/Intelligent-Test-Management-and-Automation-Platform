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
@Table(name = "jenkins_pipeline_bindings")
public class JenkinsPipelineBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "automation_project_id", nullable = false)
    private Long automationProjectId;

    @Column(name = "jenkins_instance_id", nullable = false)
    private Long jenkinsInstanceId;

    @Column(name = "pipeline_type", nullable = false, length = 32)
    private String pipelineType;

    @Column(name = "folder_path", length = 500)
    private String folderPath;

    @Column(name = "job_name", nullable = false, length = 255)
    private String jobName;

    @Column(name = "job_url", length = 1000)
    private String jobUrl;

    @Column(name = "credential_scope_ref", length = 128)
    private String credentialScopeRef;

    @Column(name = "managed_by", nullable = false, length = 16)
    private String managedBy;

    @Column(name = "provisioning_status", nullable = false, length = 24)
    private String provisioningStatus;

    @Column(name = "parameters_json", columnDefinition = "text")
    private String parametersJson;

    @Column(name = "last_scan_at")
    private LocalDateTime lastScanAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected JenkinsPipelineBinding() {
    }

    public JenkinsPipelineBinding(Long projectId, Long automationProjectId, Long jenkinsInstanceId,
                                  String pipelineType, String folderPath, String jobName, String jobUrl,
                                  String credentialScopeRef, String managedBy, String parametersJson) {
        this.projectId = projectId;
        this.automationProjectId = automationProjectId;
        this.jenkinsInstanceId = jenkinsInstanceId;
        this.pipelineType = pipelineType;
        this.folderPath = folderPath;
        this.jobName = jobName;
        this.jobUrl = jobUrl;
        this.credentialScopeRef = credentialScopeRef;
        this.managedBy = managedBy;
        this.parametersJson = parametersJson;
        this.provisioningStatus = "READY";
    }

    public void update(String pipelineType, String folderPath, String jobName, String jobUrl,
                       String credentialScopeRef, String managedBy, String parametersJson) {
        this.pipelineType = pipelineType;
        this.folderPath = folderPath;
        this.jobName = jobName;
        this.jobUrl = jobUrl;
        this.credentialScopeRef = credentialScopeRef;
        this.managedBy = managedBy;
        this.parametersJson = parametersJson;
    }

    public void setJenkinsInstanceId(Long jenkinsInstanceId) {
        this.jenkinsInstanceId = jenkinsInstanceId;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getAutomationProjectId() { return automationProjectId; }
    public Long getJenkinsInstanceId() { return jenkinsInstanceId; }
    public String getPipelineType() { return pipelineType; }
    public String getFolderPath() { return folderPath; }
    public String getJobName() { return jobName; }
    public String getJobUrl() { return jobUrl; }
    public String getCredentialScopeRef() { return credentialScopeRef; }
    public String getManagedBy() { return managedBy; }
    public String getProvisioningStatus() { return provisioningStatus; }
    public String getParametersJson() { return parametersJson; }
    public LocalDateTime getLastScanAt() { return lastScanAt; }
    public String getLastError() { return lastError; }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
