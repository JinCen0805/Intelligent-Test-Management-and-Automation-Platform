ALTER TABLE test_executions ADD COLUMN triggered_by VARCHAR(64);
ALTER TABLE test_executions ADD COLUMN trigger_method VARCHAR(16) NOT NULL DEFAULT 'MANUAL';
