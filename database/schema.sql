CREATE DATABASE IF NOT EXISTS testpilot
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE testpilot;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  email VARCHAR(128),
  role VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  last_login_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS projects (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  code VARCHAR(64) NOT NULL UNIQUE,
  description TEXT,
  owner_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS project_members (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_project_member (project_id, user_id),
  CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT fk_project_members_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS requirements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  file_type VARCHAR(16) NOT NULL,
  file_path VARCHAR(500),
  version VARCHAR(32),
  parse_status VARCHAR(16) NOT NULL,
  content_text LONGTEXT,
  uploaded_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_requirements_project FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT fk_requirements_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS test_cases (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_test_case_no (project_id, case_no),
  CONSTRAINT fk_test_cases_project FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT fk_test_cases_requirement FOREIGN KEY (requirement_id) REFERENCES requirements (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS automation_projects (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_automation_projects_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS api_collections (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500),
  execute_order VARCHAR(16) NOT NULL,
  failure_policy VARCHAR(16) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_api_collections_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS api_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  collection_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  method VARCHAR(16) NOT NULL,
  url VARCHAR(1000) NOT NULL,
  headers_json JSON,
  query_json JSON,
  path_json JSON,
  body_type VARCHAR(32),
  body_text TEXT,
  pre_script TEXT,
  post_script TEXT,
  assertions_json JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_api_requests_collection FOREIGN KEY (collection_id) REFERENCES api_collections (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS api_environment_variables (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  environment VARCHAR(32) NOT NULL,
  variable_name VARCHAR(128) NOT NULL,
  encrypted_value TEXT,
  sensitive TINYINT(1) NOT NULL DEFAULT 0,
  source VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_api_variable (project_id, environment, variable_name),
  CONSTRAINT fk_api_variables_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS test_tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  task_type VARCHAR(32) NOT NULL,
  automation_project_id BIGINT NOT NULL,
  environment VARCHAR(32) NOT NULL,
  branch VARCHAR(128) NOT NULL,
  jenkins_job VARCHAR(255) NOT NULL,
  parameters_json JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_test_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT fk_test_tasks_automation FOREIGN KEY (automation_project_id) REFERENCES automation_projects (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS test_executions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_test_executions_project FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT fk_test_executions_task FOREIGN KEY (test_task_id) REFERENCES test_tasks (id),
  CONSTRAINT fk_test_executions_automation FOREIGN KEY (automation_project_id) REFERENCES automation_projects (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS test_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  execution_id BIGINT NOT NULL,
  allure_url VARCHAR(500),
  total_count INT NOT NULL DEFAULT 0,
  passed_count INT NOT NULL DEFAULT 0,
  failed_count INT NOT NULL DEFAULT 0,
  broken_count INT NOT NULL DEFAULT 0,
  skipped_count INT NOT NULL DEFAULT 0,
  artifact_ref VARCHAR(1000),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_test_reports_execution FOREIGN KEY (execution_id) REFERENCES test_executions (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS ai_models (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  endpoint VARCHAR(500) NOT NULL,
  api_key_ciphertext TEXT,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS prompt_templates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  task_type VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  version VARCHAR(32) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;
