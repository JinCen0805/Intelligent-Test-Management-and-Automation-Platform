package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("jenkins_pipeline_bindings")
public class JenkinsPipelineBinding {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long automationProjectId;
    private Long jenkinsInstanceId;
    private String pipelineType;
    private String folderPath;
    private String jobName;
    private String jobUrl;
    private String credentialScopeRef;
    private String managedBy;
    private String provisioningStatus;
    private String parametersJson;
    private LocalDateTime lastScanAt;
    private String lastError;
    private LocalDateTime createdAt;
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}

