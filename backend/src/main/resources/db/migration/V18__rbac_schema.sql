CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    scope_type VARCHAR(16) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_roles_scope_type (scope_type),
    KEY idx_roles_enabled (enabled)
);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    resource VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    KEY idx_permissions_resource (resource),
    KEY idx_permissions_enabled (enabled)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_role_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    scope_type VARCHAR(16) NOT NULL,
    scope_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_user_role_assignments UNIQUE (user_id, role_id, scope_type, scope_id),
    KEY idx_user_role_assignments_user (user_id),
    KEY idx_user_role_assignments_role (role_id),
    KEY idx_user_role_assignments_scope (scope_type, scope_id),
    CONSTRAINT fk_user_role_assignments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_assignments_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

INSERT INTO roles (code, name, scope_type, enabled, created_at, updated_at)
SELECT 'ADMIN', '系统管理员', 'SYSTEM', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ADMIN');

INSERT INTO roles (code, name, scope_type, enabled, created_at, updated_at)
SELECT 'TEST_DEVELOPER', '测试开发', 'SYSTEM', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'TEST_DEVELOPER');

INSERT INTO roles (code, name, scope_type, enabled, created_at, updated_at)
SELECT 'PROJECT_OWNER', '项目负责人', 'PROJECT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PROJECT_OWNER');

INSERT INTO roles (code, name, scope_type, enabled, created_at, updated_at)
SELECT 'PROJECT_MANAGER', '项目经理', 'PROJECT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PROJECT_MANAGER');

INSERT INTO roles (code, name, scope_type, enabled, created_at, updated_at)
SELECT 'PROJECT_TEST_DEVELOPER', '项目测试开发', 'PROJECT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PROJECT_TEST_DEVELOPER');

INSERT INTO roles (code, name, scope_type, enabled, created_at, updated_at)
SELECT 'PROJECT_VIEWER', '项目查看者', 'PROJECT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PROJECT_VIEWER');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'system:user:manage', '用户管理', 'system', 'user:manage', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'system:user:manage');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'system:role:manage', '角色管理', 'system', 'role:manage', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'system:role:manage');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'system:ai-model:manage', 'AI 模型管理', 'system', 'ai-model:manage', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'system:ai-model:manage');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'system:jenkins:manage', 'Jenkins 管理', 'system', 'jenkins:manage', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'system:jenkins:manage');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'project:read', '项目查看', 'project', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'project:read');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'project:update', '项目编辑', 'project', 'update', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'project:update');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'project:member:manage', '项目成员管理', 'project', 'member:manage', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'project:member:manage');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'requirement:read', '需求查看', 'requirement', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'requirement:read');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'requirement:write', '需求编辑', 'requirement', 'write', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'requirement:write');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'testcase:read', '用例查看', 'testcase', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'testcase:read');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'testcase:write', '用例编辑', 'testcase', 'write', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'testcase:write');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'testcase:review', '用例审核', 'testcase', 'review', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'testcase:review');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'api:execute', '接口执行', 'api', 'execute', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'api:execute');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'automation:execute', '自动化执行', 'automation', 'execute', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'automation:execute');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'report:read', '报告查看', 'report', 'read', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'report:read');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'ai:generate', 'AI 生成', 'ai', 'generate', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'ai:generate');

INSERT INTO permissions (code, name, resource, action, enabled)
SELECT 'ai:analyze', 'AI 分析', 'ai', 'analyze', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'ai:analyze');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'system:user:manage',
    'system:role:manage',
    'system:ai-model:manage',
    'system:jenkins:manage',
    'project:read',
    'project:update',
    'project:member:manage',
    'requirement:read',
    'requirement:write',
    'testcase:read',
    'testcase:write',
    'testcase:review',
    'api:execute',
    'automation:execute',
    'report:read',
    'ai:generate',
    'ai:analyze'
)
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'project:read',
    'project:update',
    'project:member:manage',
    'requirement:read',
    'testcase:read',
    'report:read',
    'ai:generate',
    'ai:analyze'
)
WHERE r.code = 'PROJECT_OWNER'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'project:read',
    'project:update',
    'project:member:manage',
    'requirement:read',
    'requirement:write',
    'testcase:read',
    'testcase:write',
    'testcase:review',
    'api:execute',
    'automation:execute',
    'report:read',
    'ai:generate',
    'ai:analyze'
)
WHERE r.code = 'PROJECT_MANAGER'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'project:read',
    'requirement:read',
    'testcase:read',
    'testcase:write',
    'testcase:review',
    'api:execute',
    'automation:execute',
    'report:read',
    'ai:generate',
    'ai:analyze'
)
WHERE r.code = 'PROJECT_TEST_DEVELOPER'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'project:read',
    'requirement:read',
    'testcase:read',
    'report:read'
)
WHERE r.code = 'PROJECT_VIEWER'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO user_role_assignments (user_id, role_id, scope_type, scope_id, created_at)
SELECT u.id, r.id, 'SYSTEM', 0, CURRENT_TIMESTAMP
FROM users u
JOIN roles r ON r.code = 'ADMIN'
WHERE UPPER(u.role) = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM user_role_assignments ura
    WHERE ura.user_id = u.id
      AND ura.role_id = r.id
      AND ura.scope_type = 'SYSTEM'
      AND ura.scope_id = 0
  );

INSERT INTO user_role_assignments (user_id, role_id, scope_type, scope_id, created_at)
SELECT u.id, r.id, 'SYSTEM', 0, CURRENT_TIMESTAMP
FROM users u
JOIN roles r ON r.code = 'TEST_DEVELOPER'
WHERE UPPER(u.role) = 'TEST_DEVELOPER'
  AND NOT EXISTS (
    SELECT 1
    FROM user_role_assignments ura
    WHERE ura.user_id = u.id
      AND ura.role_id = r.id
      AND ura.scope_type = 'SYSTEM'
      AND ura.scope_id = 0
  );

INSERT INTO user_role_assignments (user_id, role_id, scope_type, scope_id, created_at)
SELECT p.owner_id, r.id, 'PROJECT', p.id, CURRENT_TIMESTAMP
FROM projects p
JOIN roles r ON r.code = 'PROJECT_OWNER'
WHERE NOT EXISTS (
    SELECT 1
    FROM user_role_assignments ura
    WHERE ura.user_id = p.owner_id
      AND ura.role_id = r.id
      AND ura.scope_type = 'PROJECT'
      AND ura.scope_id = p.id
);

INSERT INTO user_role_assignments (user_id, role_id, scope_type, scope_id, created_at)
SELECT pm.user_id, r.id, 'PROJECT', pm.project_id, CURRENT_TIMESTAMP
FROM project_members pm
JOIN roles r ON (
    (UPPER(pm.role) = 'MANAGER' AND r.code = 'PROJECT_MANAGER')
    OR (UPPER(pm.role) = 'TEST_DEVELOPER' AND r.code = 'PROJECT_TEST_DEVELOPER')
    OR (UPPER(pm.role) = 'VIEWER' AND r.code = 'PROJECT_VIEWER')
)
WHERE UPPER(pm.role) <> 'OWNER'
  AND NOT EXISTS (
    SELECT 1
    FROM user_role_assignments ura
    WHERE ura.user_id = pm.user_id
      AND ura.role_id = r.id
      AND ura.scope_type = 'PROJECT'
      AND ura.scope_id = pm.project_id
);
