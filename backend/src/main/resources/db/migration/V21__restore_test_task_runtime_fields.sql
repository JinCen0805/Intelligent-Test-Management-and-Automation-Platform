ALTER TABLE test_tasks ADD COLUMN environment VARCHAR(32) NULL;
ALTER TABLE test_tasks ADD COLUMN branch VARCHAR(128) NULL;

UPDATE test_tasks
SET environment = COALESCE(
        (SELECT ap.environment
         FROM automation_projects ap
         WHERE ap.id = test_tasks.automation_project_id),
        'TEST'
    ),
    branch = COALESCE(
        (SELECT ap.branch
         FROM automation_projects ap
         WHERE ap.id = test_tasks.automation_project_id),
        'main'
    );
