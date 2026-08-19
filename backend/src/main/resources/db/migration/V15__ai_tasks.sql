CREATE TABLE IF NOT EXISTS ai_tasks (
    id VARCHAR(64) PRIMARY KEY,
    project_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    result_json TEXT,
    error_code INT,
    error_message VARCHAR(1000),
    started_at DATETIME,
    finished_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_ai_tasks_project (project_id)
);
