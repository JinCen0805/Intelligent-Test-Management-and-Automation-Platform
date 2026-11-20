-- 轮询与列表接口的高频过滤列索引：此前核心业务表除主键/唯一键外没有任何索引。
-- 说明：这里只做纯 CREATE INDEX，兼容 H2（测试）与 MySQL；生产大表请在低峰窗口执行。
CREATE INDEX idx_test_executions_status_updated ON test_executions (status, updated_at);
CREATE INDEX idx_test_executions_project_created ON test_executions (project_id, created_at);
CREATE INDEX idx_test_executions_task ON test_executions (test_task_id);
CREATE INDEX idx_test_tasks_project ON test_tasks (project_id);
CREATE INDEX idx_requirements_project_created ON requirements (project_id, created_at);
CREATE INDEX idx_automation_projects_project ON automation_projects (project_id);
CREATE INDEX idx_project_members_user ON project_members (user_id);
CREATE INDEX idx_ai_tasks_status ON ai_tasks (status);
CREATE INDEX idx_prompt_templates_task_type ON prompt_templates (task_type);
