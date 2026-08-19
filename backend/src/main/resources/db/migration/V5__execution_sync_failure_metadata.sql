ALTER TABLE test_executions ADD COLUMN sync_failure_message VARCHAR(1000);
ALTER TABLE test_executions ADD COLUMN sync_failure_count INT NOT NULL DEFAULT 0;
