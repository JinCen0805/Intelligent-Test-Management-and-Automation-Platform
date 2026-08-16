CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    role VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    last_login_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL UNIQUE,
    description TEXT,
    owner_id BIGINT NOT NULL,
    default_environment VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS project_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    joined_at DATETIME NOT NULL,
    CONSTRAINT uk_project_member UNIQUE (project_id, user_id)
);

CREATE TABLE IF NOT EXISTS requirements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_type VARCHAR(16) NOT NULL,
    file_path VARCHAR(500),
    version VARCHAR(32),
    parse_status VARCHAR(16) NOT NULL,
    content_text TEXT,
    parse_error TEXT,
    uploaded_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS test_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    requirement_id BIGINT,
    case_no VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    module_name VARCHAR(128),
    precondition TEXT,
    steps TEXT,
    test_data TEXT,
    expected_result TEXT,
    priority VARCHAR(8) NOT NULL,
    case_type VARCHAR(32) NOT NULL,
    tags VARCHAR(500),
    status VARCHAR(16) NOT NULL,
    source VARCHAR(16) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_test_case_no UNIQUE (project_id, case_no)
);

CREATE TABLE IF NOT EXISTS automation_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    automation_type VARCHAR(16) NOT NULL,
    repository_url VARCHAR(500) NOT NULL,
    branch VARCHAR(128) NOT NULL,
    credential_ref VARCHAR(128),
    framework VARCHAR(64) NOT NULL,
    test_path VARCHAR(255),
    execute_command VARCHAR(1000),
    environment VARCHAR(32),
    jenkins_job VARCHAR(255),
    status VARCHAR(16) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS test_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    automation_project_id BIGINT NOT NULL,
    environment VARCHAR(32) NOT NULL,
    branch VARCHAR(128) NOT NULL,
    jenkins_job VARCHAR(255) NOT NULL,
    parameters_json TEXT,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS test_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    test_task_id BIGINT NOT NULL,
    automation_project_id BIGINT NOT NULL,
    build_number VARCHAR(64),
    commit_sha VARCHAR(64),
    status VARCHAR(16) NOT NULL,
    started_at DATETIME,
    finished_at DATETIME,
    duration_ms BIGINT,
    console_log_ref VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS test_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT NOT NULL UNIQUE,
    allure_url VARCHAR(500),
    total_count INT NOT NULL DEFAULT 0,
    passed_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    broken_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    artifact_ref VARCHAR(1000),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS ai_models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    api_key_ciphertext TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS prompt_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    version VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
