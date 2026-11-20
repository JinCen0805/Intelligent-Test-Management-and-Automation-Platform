package com.testpilot.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.audit.OperationAudit;
import com.testpilot.platform.audit.OperationAuditRepository;
import com.testpilot.platform.project.Project;
import com.testpilot.platform.project.ProjectRepository;
import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.rbac.entity.RbacPermission;
import com.testpilot.platform.rbac.entity.RbacRole;
import com.testpilot.platform.rbac.mapper.RbacRolePermissionMapper;
import com.testpilot.platform.rbac.entity.RbacUserRoleAssignment;
import com.testpilot.platform.rbac.mapper.RbacPermissionMapper;
import com.testpilot.platform.rbac.mapper.RbacRoleMapper;
import com.testpilot.platform.rbac.mapper.RbacUserRoleAssignmentMapper;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserDtos;
import com.testpilot.platform.user.UserRepository;
import com.testpilot.platform.user.UserService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class Stage2RbacTest {

    @Autowired
    private RbacService rbacService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private RbacRoleMapper roleMapper;

    @Autowired
    private RbacPermissionMapper permissionMapper;

    @Autowired
    private RbacUserRoleAssignmentMapper assignmentMapper;

    @Autowired
    private RbacRolePermissionMapper rolePermissionMapper;

    @Autowired
    private OperationAuditRepository operationAuditRepository;

    @Autowired
    private UserService userService;

    @Test
    void legacyUsersAndProjectOwnersAreMappedToDatabaseRbac() {
        User admin = userRepository.findByUsername("chen.yu").orElseThrow();
        User developer = userRepository.findByUsername("lin.xiao").orElseThrow();
        Project project = projectRepository.findByCodeAndDeletedFalse("XH-MALL").orElseThrow();

        assertTrue(rbacService.hasSystemPermission(admin.getId(), "system:user:manage"));
        assertTrue(rbacService.hasSystemPermission(admin.getId(), "system:jenkins:manage"));
        assertTrue(rbacService.hasProjectPermission(admin.getId(), project.getId(), "project:update"));

        Set<String> projectPermissions = rbacService.projectPermissions(developer.getId(), project.getId());
        assertTrue(projectPermissions.contains("project:update"));
        assertTrue(projectPermissions.contains("project:member:manage"));
        assertTrue(projectPermissions.contains("testcase:write"));
    }

    @Test
    void batchProjectPermissionCodesMatchSingleProjectChecks() {
        User admin = userRepository.findByUsername("chen.yu").orElseThrow();
        User developer = userRepository.findByUsername("lin.xiao").orElseThrow();
        Project first = projectRepository.findByCodeAndDeletedFalse("XH-MALL").orElseThrow();
        Project second = projectRepository.findByCodeAndDeletedFalse("SC-CORE").orElseThrow();

        var batch = rbacService.projectPermissionCodes(
                developer.getId(), developer.getAuthVersion(), List.of(first.getId(), second.getId()));
        assertEquals(rbacService.projectPermissions(developer.getId(), first.getId()), batch.get(first.getId()));
        assertEquals(rbacService.projectPermissions(developer.getId(), second.getId()), batch.get(second.getId()));

        var adminBatch = rbacService.projectPermissionCodes(
                admin.getId(), admin.getAuthVersion(), List.of(first.getId()));
        assertTrue(adminBatch.get(first.getId()).contains("project:update"));
    }

    @Test
    void sameUserCanHoldDifferentProjectRoles() {
        User developer = userRepository.save(new User(
                "rbac." + UUID.randomUUID(),
                "test",
                "RBAC 测试用户",
                null,
                "TEST_DEVELOPER",
                "ENABLED"
        ));
        Project firstProject = projectRepository.findByCodeAndDeletedFalse("XH-MALL").orElseThrow();
        Project secondProject = projectRepository.findByCodeAndDeletedFalse("SC-CORE").orElseThrow();

        User admin = userRepository.findByUsername("chen.yu").orElseThrow();
        rbacService.assignProjectRole(admin, developer.getId(), firstProject.getId(), "PROJECT_MANAGER");
        rbacService.assignProjectRole(admin, developer.getId(), secondProject.getId(), "PROJECT_VIEWER");

        Set<String> firstPermissions = rbacService.projectPermissions(developer.getId(), firstProject.getId());
        Set<String> secondPermissions = rbacService.projectPermissions(developer.getId(), secondProject.getId());

        assertTrue(firstPermissions.contains("project:update"));
        assertTrue(secondPermissions.contains("project:read"));
        assertFalse(secondPermissions.contains("project:update"));
    }

    @Test
    void rolePermissionTraceAndAssignmentChangeAreAudited() {
        User admin = userRepository.findByUsername("chen.yu").orElseThrow();
        Project project = projectRepository.findByCodeAndDeletedFalse("XH-MALL").orElseThrow();
        User developer = userRepository.save(new User(
                "audit." + UUID.randomUUID(),
                "test",
                "审计测试用户",
                null,
                "TEST_DEVELOPER",
                "ENABLED"
        ));

        RbacRole viewerRole = roleMapper.selectOne(Wrappers.<RbacRole>lambdaQuery()
                .eq(RbacRole::getCode, "PROJECT_VIEWER"));
        RbacPermission readPermission = permissionMapper.selectOne(Wrappers.<RbacPermission>lambdaQuery()
                .eq(RbacPermission::getCode, "project:read"));

        assertNotNull(viewerRole);
        assertNotNull(readPermission);
        assertTrue(rolePermissionMapper.selectCount(Wrappers.<com.testpilot.platform.rbac.entity.RbacRolePermission>lambdaQuery()
                .eq(com.testpilot.platform.rbac.entity.RbacRolePermission::getRoleId, viewerRole.getId())
                .eq(com.testpilot.platform.rbac.entity.RbacRolePermission::getPermissionId, readPermission.getId())) > 0);

        int before = operationAuditRepository.findAllByOrderByCreatedAtDesc().size();
        rbacService.assignProjectRole(admin, developer.getId(), project.getId(), "PROJECT_VIEWER");
        assertTrue(assignmentMapper.selectCount(Wrappers.<RbacUserRoleAssignment>lambdaQuery()
                .eq(RbacUserRoleAssignment::getUserId, developer.getId())
                .eq(RbacUserRoleAssignment::getRoleId, viewerRole.getId())
                .eq(RbacUserRoleAssignment::getScopeId, project.getId())) > 0);
        int after = operationAuditRepository.findAllByOrderByCreatedAtDesc().size();

        assertEquals(before + 1, after);
        OperationAudit latest = operationAuditRepository.findAllByOrderByCreatedAtDesc().get(0);
        assertEquals(RbacService.ACTION_ASSIGN_ROLE, latest.getAction());
        assertEquals(project.getId(), latest.getProjectId());
    }

    @Test
    void userRoleChangesSyncSystemRbacAssignments() {
        User admin = userRepository.findByUsername("chen.yu").orElseThrow();
        String username = "rbac-user." + UUID.randomUUID();

        UserDtos.Response created = userService.create(new com.testpilot.platform.user.UserDtos.CreateRequest(
                username,
                "demo123",
                "系统角色用户",
                null,
                "ADMIN"
        ), admin);
        RbacRole adminRole = roleMapper.selectOne(Wrappers.<RbacRole>lambdaQuery()
                .eq(RbacRole::getCode, "ADMIN")
                .eq(RbacRole::getScopeType, RbacService.SYSTEM_SCOPE_TYPE));
        RbacRole developerRole = roleMapper.selectOne(Wrappers.<RbacRole>lambdaQuery()
                .eq(RbacRole::getCode, "TEST_DEVELOPER")
                .eq(RbacRole::getScopeType, RbacService.SYSTEM_SCOPE_TYPE));
        assertNotNull(adminRole);
        assertNotNull(developerRole);
        assertTrue(assignmentMapper.selectCount(Wrappers.<RbacUserRoleAssignment>lambdaQuery()
                .eq(RbacUserRoleAssignment::getUserId, created.id())
                .eq(RbacUserRoleAssignment::getRoleId, adminRole.getId())
                .eq(RbacUserRoleAssignment::getScopeType, RbacService.SYSTEM_SCOPE_TYPE)
                .eq(RbacUserRoleAssignment::getScopeId, RbacService.SYSTEM_SCOPE_ID)) > 0);
        assertTrue(rbacService.hasSystemPermission(created.id(), "system:user:manage"));

        userService.update(created.id(), new com.testpilot.platform.user.UserDtos.UpdateRequest(
                username,
                "系统角色用户-改",
                null,
                "TEST_DEVELOPER"
        ), admin);

        assertTrue(assignmentMapper.selectCount(Wrappers.<RbacUserRoleAssignment>lambdaQuery()
                .eq(RbacUserRoleAssignment::getUserId, created.id())
                .eq(RbacUserRoleAssignment::getRoleId, developerRole.getId())
                .eq(RbacUserRoleAssignment::getScopeType, RbacService.SYSTEM_SCOPE_TYPE)
                .eq(RbacUserRoleAssignment::getScopeId, RbacService.SYSTEM_SCOPE_ID)) > 0);
        assertFalse(rbacService.hasSystemPermission(created.id(), "system:user:manage"));
    }
}
