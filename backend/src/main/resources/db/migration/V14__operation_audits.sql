CREATE TABLE IF NOT EXISTS operation_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    user_id BIGINT,
    user_name VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(32),
    target_id BIGINT,
    detail TEXT,
    result VARCHAR(16),
    created_at DATETIME NOT NULL,
    KEY idx_operation_audits_project (project_id),
    KEY idx_operation_audits_user (user_id),
    KEY idx_operation_audits_action (action),
    KEY idx_operation_audits_created (created_at)
);
