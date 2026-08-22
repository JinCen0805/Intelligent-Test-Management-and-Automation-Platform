UPDATE project_members SET role = 'PROJECT_OWNER' WHERE UPPER(role) = 'OWNER';
UPDATE project_members SET role = 'PROJECT_MANAGER' WHERE UPPER(role) = 'MANAGER';
UPDATE project_members SET role = 'PROJECT_TEST_DEVELOPER' WHERE UPPER(role) = 'TEST_DEVELOPER';
UPDATE project_members SET role = 'PROJECT_VIEWER' WHERE UPPER(role) = 'VIEWER';
