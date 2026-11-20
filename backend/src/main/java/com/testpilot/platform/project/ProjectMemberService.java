package com.testpilot.platform.project;

import com.testpilot.platform.audit.OperationAuditService;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.rbac.RbacPermissionCache;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserDtos;
import com.testpilot.platform.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;
    private final OperationAuditService operationAuditService;
    private final RbacPermissionCache permissionCache;
    private final RbacService rbacService;

    public ProjectMemberService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            ProjectAccessService projectAccessService,
            OperationAuditService operationAuditService,
            RbacPermissionCache permissionCache,
            RbacService rbacService
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
        this.operationAuditService = operationAuditService;
        this.permissionCache = permissionCache;
        this.rbacService = rbacService;
    }

    public List<ProjectMemberDtos.Response> list(Long projectId, User currentUser) {
        Project project = projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.PROJECT_READ);
        List<ProjectMember> members = projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(projectId);
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        userIds.add(project.getOwnerId());
        members.forEach(member -> userIds.add(member.getUserId()));
        java.util.Map<Long, User> users = userRepository.findAllById(userIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, user -> user));

        List<ProjectMemberDtos.Response> result = new ArrayList<>();
        boolean ownerIsMember = members.stream()
                .anyMatch(member -> member.getUserId().equals(project.getOwnerId()));
        if (!ownerIsMember) {
            User owner = users.get(project.getOwnerId());
            if (owner != null) {
                result.add(toResponse(null, owner, "PROJECT_OWNER"));
            }
        }
        members.forEach(member -> {
            User user = users.get(member.getUserId());
            if (user != null) {
                result.add(toResponse(member.getId(), user, member.getRole(), member.getJoinedAt()));
            }
        });
        return result;
    }

    public List<UserDtos.Response> candidates(Long projectId, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.PROJECT_MEMBER_MANAGE);
        return userRepository.findAllByStatusOrderByDisplayNameAsc("ENABLED").stream()
                .map(UserDtos.Response::from)
                .toList();
    }

    @Transactional
    public ProjectMemberDtos.Response add(
            Long projectId,
            ProjectMemberDtos.UpsertRequest request,
            User currentUser
    ) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.PROJECT_MEMBER_MANAGE);
        User member = enabledUser(request.userId());
        if (projectMemberRepository.findByProjectIdAndUserId(projectId, member.getId()).isPresent()) {
            throw new BusinessException(40904, "项目成员已存在");
        }
        ProjectMember saved = projectMemberRepository.save(new ProjectMember(
                projectId,
                member.getId(),
                normalizeRole(request.role())
        ));
        rbacService.ensureProjectRole(member.getId(), projectId, saved.getRole());
        permissionCache.invalidateProject(member.getId(), projectId);
        operationAuditService.record(projectId, currentUser, "PROJECT_MEMBER_ADD", "PROJECT_MEMBER", member.getId(),
                "添加成员 " + member.getUsername() + " 到项目 " + projectId, true);
        return toResponse(saved);
    }

    @Transactional
    public ProjectMemberDtos.Response update(
            Long projectId,
            Long userId,
            ProjectMemberDtos.UpsertRequest request,
            User currentUser
    ) {
        Project project = projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.PROJECT_MEMBER_MANAGE);
        if (project.getOwnerId().equals(userId)) {
            throw new BusinessException(40016, "项目负责人角色不能修改");
        }
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(40405, "项目成员不存在"));
        member.setRole(normalizeRole(request.role()));
        ProjectMember saved = projectMemberRepository.save(member);
        rbacService.replaceProjectRole(userId, projectId, saved.getRole());
        permissionCache.invalidateProject(userId, projectId);
        operationAuditService.record(projectId, currentUser, "PROJECT_MEMBER_UPDATE", "PROJECT_MEMBER", userId,
                "变更成员 " + member.getUserId() + " 在项目 " + projectId + " 的角色为 " + saved.getRole(), true);
        return toResponse(saved);
    }

    @Transactional
    public void remove(Long projectId, Long userId, User currentUser) {
        Project project = projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.PROJECT_MEMBER_MANAGE);
        if (project.getOwnerId().equals(userId)) {
            throw new BusinessException(40016, "项目负责人不能移除");
        }
        if (projectMemberRepository.findByProjectIdAndUserId(projectId, userId).isEmpty()) {
            throw new BusinessException(40405, "项目成员不存在");
        }
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
        rbacService.removeProjectRoles(userId, projectId);
        permissionCache.invalidateProject(userId, projectId);
        operationAuditService.record(projectId, currentUser, "PROJECT_MEMBER_REMOVE", "PROJECT_MEMBER", userId,
                "从项目 " + projectId + " 移除成员 " + userId, true);
    }

    private ProjectMemberDtos.Response toResponse(ProjectMember member) {
        User user = userRepository.findById(member.getUserId())
                .orElseThrow(() -> new BusinessException(40404, "成员用户不存在"));
        return toResponse(member.getId(), user, member.getRole(), member.getJoinedAt());
    }

    private ProjectMemberDtos.Response toResponse(Long membershipId, User user, String role) {
        return toResponse(membershipId, user, role, null);
    }

    private ProjectMemberDtos.Response toResponse(
            Long membershipId,
            User user,
            String role,
            java.time.LocalDateTime joinedAt
    ) {
        return new ProjectMemberDtos.Response(
                membershipId,
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                role,
                joinedAt
        );
    }

    private User enabledUser(Long userId) {
        return userRepository.findByIdAndStatus(userId, "ENABLED")
                .orElseThrow(() -> new BusinessException(40404, "可用用户不存在"));
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PROJECT_MANAGER", "PROJECT_TEST_DEVELOPER", "PROJECT_VIEWER").contains(normalized)) {
            throw new BusinessException(40017, "项目成员角色不合法");
        }
        return normalized;
    }
}
