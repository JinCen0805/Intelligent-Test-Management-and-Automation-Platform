CREATE TABLE IF NOT EXISTS api_collections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS api_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    collection_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    method VARCHAR(16) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    params_json TEXT,
    headers_json TEXT,
    body_text TEXT,
    assertions_json TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS api_environment_variables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    environment VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    value_text TEXT,
    sensitive_flag BOOLEAN NOT NULL DEFAULT FALSE,
    source VARCHAR(64),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_api_environment_variable UNIQUE (project_id, environment, name)
);

CREATE TABLE IF NOT EXISTS api_execution_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    environment VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    status_code INT,
    duration_ms BIGINT,
    response_headers_json TEXT,
    response_body TEXT,
    assertion_results_json TEXT,
    started_at DATETIME NOT NULL,
    finished_at DATETIME
);

ALTER TABLE test_reports ADD COLUMN failure_detail TEXT;
ALTER TABLE test_reports ADD COLUMN screenshot_ref VARCHAR(1000);
ALTER TABLE test_reports ADD COLUMN video_ref VARCHAR(1000);
ALTER TABLE test_reports ADD COLUMN ai_analysis TEXT;
