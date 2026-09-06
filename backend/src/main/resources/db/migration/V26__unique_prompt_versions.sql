ALTER TABLE prompt_templates
    ADD CONSTRAINT uk_prompt_template_name_type_version UNIQUE (name, task_type, version);

ALTER TABLE ai_tasks ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
