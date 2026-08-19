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
@Table(name = "jenkins_credential_grants")
public class JenkinsCredentialGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "credential_ref", nullable = false, length = 128)
    private String credentialRef;

    @Column(name = "credential_scope_ref", length = 128)
    private String credentialScopeRef;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
    public void revoke() { this.status = "REVOKED"; }

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
}
