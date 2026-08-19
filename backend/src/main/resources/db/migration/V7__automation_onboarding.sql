CREATE TABLE IF NOT EXISTS automation_onboardings (
    id VARCHAR(64) PRIMARY KEY,
    project_id BIGINT NOT NULL,
    automation_project_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    current_step VARCHAR(64),
    retry_count INT NOT NULL DEFAULT 0,
    error_code VARCHAR(32),
    error_message VARCHAR(1000),
    started_at DATETIME,
    finished_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS automation_onboarding_steps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    onboarding_id VARCHAR(64) NOT NULL,
    step_key VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    input_summary TEXT,
    output_summary TEXT,
    error_message VARCHAR(1000),
    retry_count INT NOT NULL DEFAULT 0,
    started_at DATETIME,
    finished_at DATETIME
);
