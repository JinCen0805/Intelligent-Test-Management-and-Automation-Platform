ALTER TABLE api_requests ADD COLUMN pre_request_script TEXT;
ALTER TABLE api_requests ADD COLUMN post_request_script TEXT;
ALTER TABLE api_execution_history ADD COLUMN collection_run_id VARCHAR(64);
