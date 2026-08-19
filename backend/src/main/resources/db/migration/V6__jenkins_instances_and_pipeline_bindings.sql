CREATE TABLE IF NOT EXISTS jenkins_instances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    username VARCHAR(128) NOT NULL,
    api_token_ciphertext TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    connection_status VARCHAR(16) NOT NULL,
    connect_timeout_ms INT NOT NULL,
    read_timeout_ms INT NOT NULL,
    poll_interval_ms INT NOT NULL,
    max_wait_minutes INT NOT NULL,
    last_checked_at DATETIME,
    last_error VARCHAR(1000),
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS jenkins_pipeline_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    automation_project_id BIGINT NOT NULL,
    jenkins_instance_id BIGINT NOT NULL,
    pipeline_type VARCHAR(32) NOT NULL,
    folder_path VARCHAR(500),
    job_name VARCHAR(255) NOT NULL,
    job_url VARCHAR(1000),
    credential_scope_ref VARCHAR(128),
    managed_by VARCHAR(16) NOT NULL,
    provisioning_status VARCHAR(24) NOT NULL,
    parameters_json TEXT,
    last_scan_at DATETIME,
    last_error VARCHAR(1000),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

ALTER TABLE test_tasks ADD COLUMN pipeline_binding_id BIGINT;
