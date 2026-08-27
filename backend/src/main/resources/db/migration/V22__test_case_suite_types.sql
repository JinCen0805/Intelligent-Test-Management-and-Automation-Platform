CREATE TABLE IF NOT EXISTS ai_case_chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    suite_type VARCHAR(16) NOT NULL DEFAULT 'FUNCTIONAL',
    session_key BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    messages_json LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_ai_case_chat_session_suite UNIQUE (project_id, user_id, suite_type, session_key),
    KEY idx_ai_case_chat_sessions_project_user_suite_updated (project_id, user_id, suite_type, updated_at)
);

ALTER TABLE test_cases
    ADD COLUMN suite_type VARCHAR(16) NOT NULL DEFAULT 'FUNCTIONAL' AFTER case_type;

ALTER TABLE test_cases
    DROP INDEX uk_test_case_no;

ALTER TABLE test_cases
    ADD CONSTRAINT uk_test_case_suite_no UNIQUE (project_id, suite_type, case_no);

ALTER TABLE test_cases
    ADD KEY idx_test_cases_project_suite_status (project_id, suite_type, status);
