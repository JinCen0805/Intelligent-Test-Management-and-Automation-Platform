ALTER TABLE test_executions ADD COLUMN jenkins_job VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE test_executions ADD COLUMN queue_url VARCHAR(1000);
