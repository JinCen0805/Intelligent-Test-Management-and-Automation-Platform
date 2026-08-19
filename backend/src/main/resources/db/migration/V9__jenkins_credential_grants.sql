CREATE TABLE IF NOT EXISTS jenkins_credential_grants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    credential_ref VARCHAR(128) NOT NULL,
    credential_scope_ref VARCHAR(128),
    status VARCHAR(16) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_jenkins_credential_grant (project_id, credential_ref)
);
