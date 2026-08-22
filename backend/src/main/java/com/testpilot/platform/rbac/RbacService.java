package com.testpilot.platform.rbac;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.audit.OperationAuditService;
import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.Project;
import com.testpilot.platform.project.ProjectMember;
import com.testpilot.platform.project.ProjectMemberRepository;
import com.testpilot.platform.project.ProjectRepository;
import com.testpilot.platform.rbac.entity.RbacPermission;
import com.testpilot.platform.rbac.entity.RbacRole;
import com.testpilot.platform.rbac.entity.RbacRolePermission;
import com.testpilot.platform.rbac.entity.RbacUserRoleAssignment;
import com.testpilot.platform.rbac.mapper.RbacPermissionMapper;
import com.testpilot.platform.rbac.mapper.RbacRoleMapper;
import com.testpilot.platform.rbac.mapper.RbacRolePermissionMapper;
import com.testpilot.platform.rbac.mapper.RbacUserRoleAssignmentMapper;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RbacService {

    public static final String SYSTEM_SCOPE_TYPE = "SYSTEM";
    public static final String PROJECT_SCOPE_TYPE = "PROJECT";
    public static final long SYSTEM_SCOPE_ID = 0L;
    public static final String ACTION_ASSIGN_ROLE = "RBAC_ASSIGN_ROLE";
    public static final String ACTION_REMOVE_ROLE = "RBAC_REMOVE_ROLE";
    private static final List<RoleSeed> ROLE_SEEDS = List.of(
            new RoleSeed("ADMIN", "系统管理员", SYSTEM_SCOPE_TYPE, true),
            new RoleSeed("TEST_DEVELOPER", "测试开发", SYSTEM_SCOPE_TYPE, true),
            new RoleSeed("PROJECT_OWNER", "项目负责人", PROJECT_SCOPE_TYPE, true),
            new RoleSeed("PROJECT_MANAGER", "项目经理", PROJECT_SCOPE_TYPE, true),
            new RoleSeed("PROJECT_TEST_DEVELOPER", "项目测试开发", PROJECT_SCOPE_TYPE, true),
            new RoleSeed("PROJECT_VIEWER", "项目查看者", PROJECT_SCOPE_TYPE, true)
    );
    private static final List<PermissionSeed> PERMISSION_SEEDS = List.of(
            new PermissionSeed(RbacPermissions.SYSTEM_USER_MANAGE, "用户管理", "system", "user:manage"),
            new PermissionSeed(RbacPermissions.SYSTEM_ROLE_MANAGE, "角色管理", "system", "role:manage"),
            new PermissionSeed(RbacPermissions.SYSTEM_AI_MODEL_MANAGE, "AI 模型管理", "system", "ai-model:manage"),
            new PermissionSeed(RbacPermissions.SYSTEM_JENKINS_MANAGE, "Jenkins 管理", "system", "jenkins:manage"),
            new PermissionSeed(RbacPermissions.SYSTEM_AUDIT_READ, "审计查看", "system", "audit:read"),
            new PermissionSeed(RbacPermissions.PROJECT_READ, "项目查看", "project", "read"),
            new PermissionSeed(RbacPermissions.PROJECT_UPDATE, "项目编辑", "project", "update"),
            new PermissionSeed(RbacPermissions.PROJECT_MEMBER_MANAGE, "项目成员管理", "project", "member:manage"),
            new PermissionSeed(RbacPermissions.REQUIREMENT_READ, "需求查看", "requirement", "read"),
            new PermissionSeed(RbacPermissions.REQUIREMENT_WRITE, "需求编辑", "requirement", "write"),
            new PermissionSeed(RbacPermissions.TESTCASE_READ, "用例查看", "testcase", "read"),
            new PermissionSeed(RbacPermissions.TESTCASE_WRITE, "用例编辑", "testcase", "write"),
            new PermissionSeed(RbacPermissions.TESTCASE_REVIEW, "用例审核", "testcase", "review"),
            new PermissionSeed(RbacPermissions.API_EXECUTE, "接口执行", "api", "execute"),
            new PermissionSeed(RbacPermissions.AUTOMATION_EXECUTE, "自动化执行", "automation", "execute"),
            new PermissionSeed(RbacPermissions.REPORT_READ, "报告查看", "report", "read"),
            new PermissionSeed(RbacPermissions.AI_GENERATE, "AI 生成", "ai", "generate"),
            new PermissionSeed(RbacPermissions.AI_ANALYZE, "AI 分析", "ai", "analyze")
    );

    private static final Map<String, List<String>> ROLE_PERMISSION_CODES = Map.of(
            "ADMIN", PERMISSION_SEEDS.stream().map(PermissionSeed::code).toList(),
            "TEST_DEVELOPER", List.of(),
            "PROJECT_OWNER", List.of(
                    RbacPermissions.PROJECT_READ,
                    RbacPermissions.PROJECT_UPDATE,
                    RbacPermissions.PROJECT_MEMBER_MANAGE,
                    RbacPermissions.REQUIREMENT_READ,
                    RbacPermissions.REQUIREMENT_WRITE,
                    RbacPermissions.TESTCASE_READ,
                    RbacPermissions.TESTCASE_WRITE,
                    RbacPermissions.TESTCASE_REVIEW,
                    RbacPermissions.API_EXECUTE,
                    RbacPermissions.AUTOMATION_EXECUTE,
                    RbacPermissions.REPORT_READ,
                    RbacPermissions.AI_GENERATE,
                    RbacPermissions.AI_ANALYZE
            ),
            "PROJECT_MANAGER", List.of(
                    RbacPermissions.PROJECT_READ,
                    RbacPermissions.PROJECT_UPDATE,
                    RbacPermissions.PROJECT_MEMBER_MANAGE,
                    RbacPermissions.REQUIREMENT_READ,
                    RbacPermissions.REQUIREMENT_WRITE,
                    RbacPermissions.TESTCASE_READ,
                    RbacPermissions.TESTCASE_WRITE,
                    RbacPermissions.TESTCASE_REVIEW,
                    RbacPermissions.API_EXECUTE,
                    RbacPermissions.AUTOMATION_EXECUTE,
                    RbacPermissions.REPORT_READ,
                    RbacPermissions.AI_GENERATE,
                    RbacPermissions.AI_ANALYZE
            ),
            "PROJECT_TEST_DEVELOPER", List.of(
                    RbacPermissions.PROJECT_READ,
                    RbacPermissions.REQUIREMENT_READ,
                    RbacPermissions.TESTCASE_READ,
                    RbacPermissions.TESTCASE_WRITE,
                    RbacPermissions.TESTCASE_REVIEW,
                    RbacPermissions.API_EXECUTE,
                    RbacPermissions.AUTOMATION_EXECUTE,
                    RbacPermissions.REPORT_READ,
                    RbacPermissions.AI_GENERATE,
                    RbacPermissions.AI_ANALYZE
            ),
            "PROJECT_VIEWER", List.of(
                    RbacPermissions.PROJECT_READ,
                    RbacPermissions.REQUIREMENT_READ,
                    RbacPermissions.TESTCASE_READ,
                    RbacPermissions.REPORT_READ
            )
    );

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final OperationAuditService operationAuditService;
    private final AuthService authService;
    private final RbacPermissionCache permissionCache;
    private final RbacRoleMapper roleMapper;
    private final RbacPermissionMapper permissionMapper;
    private final RbacRolePermissionMapper rolePermissionMapper;
    private final RbacUserRoleAssignmentMapper assignmentMapper;

    public RbacService(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            OperationAuditService operationAuditService,
            AuthService authService,
            RbacPermissionCache permissionCache,
            RbacRoleMapper roleMapper,
            RbacPermissionMapper permissionMapper,
            RbacRolePermissionMapper rolePermissionMapper,
            RbacUserRoleAssignmentMapper assignmentMapper
    ) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.operationAuditService = operationAuditService;
        this.authService = authService;
        this.permissionCache = permissionCache;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.assignmentMapper = assignmentMapper;
    }

    @Transactional
    public void syncLegacyAssignments() {
        seedCatalog();
        syncSystemAssignments();
        syncProjectAssignments();
    }

    @Transactional(readOnly = true)
    public Set<String> systemPermissions(Long userId) {
        Long authVersion = currentAuthVersion(userId);
        Optional<Set<String>> cached = permissionCache.getSystem(userId, authVersion);
        if (cached.isPresent()) {
            return cached.get();
        }
        Set<String> permissions = resolvePermissions(userId, SYSTEM_SCOPE_TYPE, SYSTEM_SCOPE_ID, null);
        permissionCache.putSystem(userId, authVersion, permissions);
        return permissions;
    }

    @Transactional(readOnly = true)
    public Set<String> projectPermissions(Long userId, Long projectId) {
        Long authVersion = currentAuthVersion(userId);
        Optional<Set<String>> cached = permissionCache.getProject(userId, projectId, authVersion);
        if (cached.isPresent()) {
            return cached.get();
        }
        Set<String> permissions = resolvePermissions(userId, PROJECT_SCOPE_TYPE, projectId, projectId);
        permissionCache.putProject(userId, projectId, authVersion, permissions);
        return permissions;
    }

    @Transactional(readOnly = true)
    public boolean hasSystemPermission(Long userId, String permissionCode) {
        return systemPermissions(userId).contains(permissionCode);
    }

    @Transactional(readOnly = true)
    public boolean hasProjectPermission(Long userId, Long projectId, String permissionCode) {
        return projectPermissions(userId, projectId).contains(permissionCode);
    }

    @Transactional
    public RbacUserRoleAssignment assignSystemRole(User operator, Long userId, String roleCode) {
        return assignRole(operator, userId, roleCode, SYSTEM_SCOPE_TYPE, SYSTEM_SCOPE_ID);
    }

    @Transactional
    public RbacUserRoleAssignment assignProjectRole(User operator, Long userId, Long projectId, String roleCode) {
        return assignRole(operator, userId, roleCode, PROJECT_SCOPE_TYPE, projectId);
    }

    @Transactional
    public void revokeRole(User operator, Long userId, String roleCode, String scopeType, Long scopeId) {
        RbacRole role = requireRole(roleCode, scopeType);
        RbacUserRoleAssignment assignment = findAssignment(userId, role.getId(), scopeType, scopeId)
                .orElseThrow(() -> new BusinessException(40451, "角色授权不存在"));
        assignmentMapper.deleteById(assignment.getId());
        authService.invalidateUserSessions(userId);
        permissionCache.invalidateUser(userId);
        operationAuditService.record(scopeTypeEquals(scopeType, PROJECT_SCOPE_TYPE) ? scopeId : null,
                operator,
                ACTION_REMOVE_ROLE,
                "RBAC_ROLE_ASSIGNMENT",
                assignment.getId(),
                "移除用户 " + userId + " 的角色 " + roleCode + "（" + scopeType + ":" + scopeId + "）",
                true);
    }

    @Transactional
    public void ensureProjectRole(Long userId, Long projectId, String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return;
        }
        RbacRole role = requireRole(roleCode, PROJECT_SCOPE_TYPE);
        ensureAssignment(userId, role.getId(), PROJECT_SCOPE_TYPE, projectId);
        permissionCache.invalidateProject(userId, projectId);
    }

    @Transactional
    public void replaceProjectRole(Long userId, Long projectId, String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return;
        }
        List<RbacRole> projectRoles = roleMapper.selectList(Wrappers.<RbacRole>lambdaQuery()
                .eq(RbacRole::getScopeType, PROJECT_SCOPE_TYPE));
        if (!projectRoles.isEmpty()) {
            assignmentMapper.delete(Wrappers.<RbacUserRoleAssignment>lambdaQuery()
                    .eq(RbacUserRoleAssignment::getUserId, userId)
                    .eq(RbacUserRoleAssignment::getScopeType, PROJECT_SCOPE_TYPE)
                    .eq(RbacUserRoleAssignment::getScopeId, projectId)
                    .in(RbacUserRoleAssignment::getRoleId,
                            projectRoles.stream().map(RbacRole::getId).toList()));
        }
        RbacRole role = requireRole(roleCode, PROJECT_SCOPE_TYPE);
        ensureAssignment(userId, role.getId(), PROJECT_SCOPE_TYPE, projectId);
        permissionCache.invalidateProject(userId, projectId);
    }

    @Transactional
    public void removeProjectRoles(Long userId, Long projectId) {
        List<RbacRole> projectRoles = roleMapper.selectList(Wrappers.<RbacRole>lambdaQuery()
                .eq(RbacRole::getScopeType, PROJECT_SCOPE_TYPE));
        if (projectRoles.isEmpty()) {
            return;
        }
        assignmentMapper.delete(Wrappers.<RbacUserRoleAssignment>lambdaQuery()
                .eq(RbacUserRoleAssignment::getUserId, userId)
                .eq(RbacUserRoleAssignment::getScopeType, PROJECT_SCOPE_TYPE)
                .eq(RbacUserRoleAssignment::getScopeId, projectId)
                .in(RbacUserRoleAssignment::getRoleId, projectRoles.stream().map(RbacRole::getId).toList()));
        permissionCache.invalidateProject(userId, projectId);
    }

    private RbacUserRoleAssignment assignRole(User operator, Long userId, String roleCode, String scopeType, Long scopeId) {
        requireEnabledUser(userId);
        RbacRole role = requireRole(roleCode, scopeType);
        if (findAssignment(userId, role.getId(), scopeType, scopeId).isPresent()) {
            return findAssignment(userId, role.getId(), scopeType, scopeId).orElseThrow();
        }
        RbacUserRoleAssignment saved = assignmentMapper.insert(new RbacUserRoleAssignment(userId, role.getId(), scopeType, scopeId))
                > 0 ? findAssignment(userId, role.getId(), scopeType, scopeId).orElseThrow() : null;
        authService.invalidateUserSessions(userId);
        permissionCache.invalidateUser(userId);
        operationAuditService.record(
                scopeTypeEquals(scopeType, PROJECT_SCOPE_TYPE) ? scopeId : null,
                operator,
                ACTION_ASSIGN_ROLE,
                "RBAC_ROLE_ASSIGNMENT",
                saved.getId(),
                "为用户 " + userId + " 分配角色 " + roleCode + "（" + scopeType + ":" + scopeId + "）",
                true
        );
        return saved;
    }

    private Set<String> resolvePermissions(Long userId, String scopeType, Long scopeId, Long projectId) {
        List<RbacUserRoleAssignment> assignments = assignmentMapper.selectList(Wrappers.<RbacUserRoleAssignment>lambdaQuery()
                .eq(RbacUserRoleAssignment::getUserId, userId)
                .eq(RbacUserRoleAssignment::getScopeType, scopeType)
                .eq(RbacUserRoleAssignment::getScopeId, scopeId));
        if (projectId != null) {
            assignments = new ArrayList<>(assignments);
            assignments.addAll(assignmentMapper.selectList(Wrappers.<RbacUserRoleAssignment>lambdaQuery()
                    .eq(RbacUserRoleAssignment::getUserId, userId)
                    .eq(RbacUserRoleAssignment::getScopeType, SYSTEM_SCOPE_TYPE)
                    .eq(RbacUserRoleAssignment::getScopeId, SYSTEM_SCOPE_ID)));
        }
        if (assignments.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds = assignments.stream().map(RbacUserRoleAssignment::getRoleId).distinct().toList();
        List<RbacRolePermission> mappings = rolePermissionMapper.selectList(Wrappers.<RbacRolePermission>lambdaQuery()
                .in(RbacRolePermission::getRoleId, roleIds));
        if (mappings.isEmpty()) {
            return Set.of();
        }
        Set<Long> permissionIds = mappings.stream().map(RbacRolePermission::getPermissionId).collect(Collectors.toSet());
        if (permissionIds.isEmpty()) {
            return Set.of();
        }
        return permissionMapper.selectList(Wrappers.<RbacPermission>lambdaQuery()
                        .in(RbacPermission::getId, permissionIds)
                        .eq(RbacPermission::getEnabled, true))
                .stream()
                .map(RbacPermission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void seedCatalog() {
        Map<String, RbacRole> rolesByCode = roleMapper.selectList(Wrappers.<RbacRole>lambdaQuery()
                        .in(RbacRole::getCode, ROLE_SEEDS.stream().map(RoleSeed::code).toList()))
                .stream()
                .collect(Collectors.toMap(RbacRole::getCode, Function.identity(), (left, right) -> left));
        for (RoleSeed seed : ROLE_SEEDS) {
            rolesByCode.computeIfAbsent(seed.code(), code -> {
                roleMapper.insert(new RbacRole(seed.code(), seed.name(), seed.scopeType(), seed.enabled()));
                return requireRole(seed.code(), seed.scopeType());
            });
        }

        Map<String, RbacPermission> permissionsByCode = permissionMapper.selectList(Wrappers.<RbacPermission>lambdaQuery()
                        .in(RbacPermission::getCode, PERMISSION_SEEDS.stream().map(PermissionSeed::code).toList()))
                .stream()
                .collect(Collectors.toMap(RbacPermission::getCode, Function.identity(), (left, right) -> left));
        for (PermissionSeed seed : PERMISSION_SEEDS) {
            permissionsByCode.computeIfAbsent(seed.code(), code -> {
                permissionMapper.insert(new RbacPermission(seed.code(), seed.name(), seed.resource(), seed.action(), true));
                return requirePermission(seed.code());
            });
        }

        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSION_CODES.entrySet()) {
            RbacRole role = requireRole(entry.getKey(), roleScopeType(entry.getKey()));
            for (String permissionCode : entry.getValue()) {
                RbacPermission permission = requirePermission(permissionCode);
                boolean exists = rolePermissionMapper.selectCount(Wrappers.<RbacRolePermission>lambdaQuery()
                        .eq(RbacRolePermission::getRoleId, role.getId())
                        .eq(RbacRolePermission::getPermissionId, permission.getId())) > 0;
                if (!exists) {
                    rolePermissionMapper.insert(new RbacRolePermission(role.getId(), permission.getId()));
                }
            }
        }
    }

    private void syncSystemAssignments() {
        RbacRole adminRole = requireRole("ADMIN", SYSTEM_SCOPE_TYPE);
        RbacRole developerRole = requireRole("TEST_DEVELOPER", SYSTEM_SCOPE_TYPE);
        for (User user : userRepository.findAll()) {
            if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
                continue;
            }
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                ensureAssignment(user.getId(), adminRole.getId(), SYSTEM_SCOPE_TYPE, SYSTEM_SCOPE_ID);
            } else if ("TEST_DEVELOPER".equalsIgnoreCase(user.getRole())) {
                ensureAssignment(user.getId(), developerRole.getId(), SYSTEM_SCOPE_TYPE, SYSTEM_SCOPE_ID);
            }
        }
    }

    private void syncProjectAssignments() {
        Map<String, RbacRole> rolesByCode = roleMapper.selectList(Wrappers.<RbacRole>lambdaQuery()
                        .in(RbacRole::getCode, List.of("PROJECT_OWNER", "PROJECT_MANAGER", "PROJECT_TEST_DEVELOPER", "PROJECT_VIEWER")))
                .stream()
                .collect(Collectors.toMap(RbacRole::getCode, Function.identity()));
        projectRepository.findAllByDeletedFalseOrderByCreatedAtDesc().forEach(project -> {
            ensureAssignment(project.getOwnerId(), rolesByCode.get("PROJECT_OWNER").getId(), PROJECT_SCOPE_TYPE, project.getId());
        });
        for (ProjectMember member : projectMemberRepository.findAll()) {
            String roleCode = normalizeProjectRoleCode(member.getRole());
            if (roleCode != null && !"PROJECT_OWNER".equalsIgnoreCase(member.getRole())) {
                ensureAssignment(member.getUserId(), rolesByCode.get(roleCode).getId(), PROJECT_SCOPE_TYPE, member.getProjectId());
            }
        }
    }

    private String normalizeProjectRoleCode(String role) {
        if (role == null) {
            return null;
        }
        return switch (role.toUpperCase()) {
            case "PROJECT_OWNER", "PROJECT_MANAGER", "PROJECT_TEST_DEVELOPER", "PROJECT_VIEWER" -> role.toUpperCase();
            default -> null;
        };
    }

    private void ensureAssignment(Long userId, Long roleId, String scopeType, Long scopeId) {
        if (findAssignment(userId, roleId, scopeType, scopeId).isPresent()) {
            return;
        }
        assignmentMapper.insert(new RbacUserRoleAssignment(userId, roleId, scopeType, scopeId));
    }

    private Optional<RbacUserRoleAssignment> findAssignment(Long userId, Long roleId, String scopeType, Long scopeId) {
        return Optional.ofNullable(assignmentMapper.selectOne(Wrappers.<RbacUserRoleAssignment>lambdaQuery()
                .eq(RbacUserRoleAssignment::getUserId, userId)
                .eq(RbacUserRoleAssignment::getRoleId, roleId)
                .eq(RbacUserRoleAssignment::getScopeType, scopeType)
                .eq(RbacUserRoleAssignment::getScopeId, scopeId)));
    }

    private RbacRole requireRole(String roleCode, String scopeType) {
        RbacRole role = roleMapper.selectOne(Wrappers.<RbacRole>lambdaQuery()
                        .eq(RbacRole::getCode, roleCode)
                        .eq(RbacRole::getScopeType, scopeType)
                        .eq(RbacRole::getEnabled, true));
        if (role == null) {
            throw new BusinessException(40450, "角色不存在或已禁用");
        }
        return role;
    }

    private RbacPermission requirePermission(String permissionCode) {
        RbacPermission permission = permissionMapper.selectOne(Wrappers.<RbacPermission>lambdaQuery()
                .eq(RbacPermission::getCode, permissionCode)
                .eq(RbacPermission::getEnabled, true));
        if (permission == null) {
            throw new BusinessException(40452, "权限不存在或已禁用");
        }
        return permission;
    }

    private String roleScopeType(String roleCode) {
        return "ADMIN".equals(roleCode) || "TEST_DEVELOPER".equals(roleCode)
                ? SYSTEM_SCOPE_TYPE
                : PROJECT_SCOPE_TYPE;
    }

    private void requireEnabledUser(Long userId) {
        userRepository.findByIdAndStatus(userId, "ENABLED")
                .orElseThrow(() -> new BusinessException(40404, "可用用户不存在"));
    }

    private Long currentAuthVersion(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getAuthVersion() == null ? 1L : user.getAuthVersion())
                .orElse(1L);
    }

    private boolean scopeTypeEquals(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private record RoleSeed(String code, String name, String scopeType, boolean enabled) {
    }

    private record PermissionSeed(String code, String name, String resource, String action) {
    }
}
