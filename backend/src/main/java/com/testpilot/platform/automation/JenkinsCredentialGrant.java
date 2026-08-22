package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("jenkins_credential_grants")
public class JenkinsCredentialGrant {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String credentialRef;
    private String credentialScopeRef;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected JenkinsCredentialGrant() {
    }

    public JenkinsCredentialGrant(
            Long projectId,
            String credentialRef,
            String credentialScopeRef,
            Long createdBy
    ) {
        this.projectId = projectId;
        this.credentialRef = credentialRef;
        this.credentialScopeRef = credentialScopeRef;
        this.createdBy = createdBy;
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getCredentialRef() { return credentialRef; }
    public String getCredentialScopeRef() { return credentialScopeRef; }
    public String getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void revoke() { this.status = "REVOKED"; }
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

