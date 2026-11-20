package com.testpilot.platform.project;

import com.testpilot.platform.audit.OperationAuditService;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.rbac.RbacPermissionCache;
import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessService projectAccessService;
    private final OperationAuditService operationAuditService;
    private final RbacPermissionCache permissionCache;
    private final RbacService rbacService;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectAccessService projectAccessService,
            OperationAuditService operationAuditService,
            RbacPermissionCache permissionCache,
            RbacService rbacService
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectAccessService = projectAccessService;
        this.operationAuditService = operationAuditService;
        this.permissionCache = permissionCache;
        this.rbacService = rbacService;
    }

    public List<ProjectDtos.Response> list(User currentUser) {
        List<Project> allProjects = projectRepository.findAllByDeletedFalseOrderByCreatedAtDesc();
        List<Project> accessibleProjects = projectAccessService.filterReadable(allProjects, currentUser);
        if (accessibleProjects.isEmpty()) {
            return List.of();
        }
        // 批量取 owner 名称与成员，避免每个项目 3 次额外查询。
        Set<Long> ownerIds = accessibleProjects.stream()
                .map(Project::getOwnerId)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, String> ownerNames = userRepository.findAllById(ownerIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getDisplayName));
        List<Long> projectIds = accessibleProjects.stream().map(Project::getId).toList();
        java.util.Map<Long, List<ProjectMember>> membersByProject = projectMemberRepository
                .findAllByProjectIdIn(projectIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(ProjectMember::getProjectId));
        return accessibleProjects.stream()
                .map(project -> toResponse(
                        project,
                        ownerNames.getOrDefault(project.getOwnerId(), "未知用户"),
                        membersByProject.getOrDefault(project.getId(), List.of())
                ))
                .toList();
    }

    public ProjectDtos.Response get(Long id, User currentUser) {
        return toResponse(projectAccessService.requireRead(id, currentUser));
    }

    @Transactional
    public ProjectDtos.Response create(ProjectDtos.CreateRequest request, User currentUser) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9][A-Z0-9_-]{0,63}")) {
            throw new BusinessException(40026, "项目标识只能包含字母、数字、下划线和连字符，且以字母或数字开头");
        }
        if (projectRepository.existsByCode(code)) {
            throw new BusinessException(40901, "项目标识已存在");
        }
        Project project = new Project(
                request.name().trim(),
                code,
                request.description(),
                currentUser.getId(),
                "TESTING"
        );
        Project saved;
        try {
            saved = projectRepository.save(project);
        } catch (DuplicateKeyException exception) {
            // existsByCode 会被 @TableLogic 过滤掉已删除项目，这里兜底唯一键冲突。
            throw new BusinessException(40901, "项目标识已存在（可能属于已删除项目）");
        }
        projectMemberRepository.save(new ProjectMember(saved.getId(), currentUser.getId(), "PROJECT_OWNER"));
        rbacService.ensureProjectRole(currentUser.getId(), saved.getId(), "PROJECT_OWNER");
        permissionCache.invalidateProject(currentUser.getId(), saved.getId());
        operationAuditService.record(saved.getId(), currentUser, "PROJECT_CREATE", "PROJECT", saved.getId(),
                "创建项目 " + saved.getName() + "（" + saved.getCode() + "）", true);
        return toResponse(saved);
    }

    @Transactional
    public ProjectDtos.Response update(Long id, ProjectDtos.UpdateRequest request, User currentUser) {
        Project project = projectAccessService.requireWrite(id, currentUser);
        project.setName(request.name().trim());
        project.setDescription(request.description());
        if (request.status() != null && !request.status().isBlank()) {
            project.setStatus(normalizeStatus(request.status()));
        }
        Project saved = projectRepository.save(project);
        operationAuditService.record(id, currentUser, "PROJECT_UPDATE", "PROJECT", id,
                "更新项目 " + saved.getName(), true);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Project project = projectAccessService.requireWrite(id, currentUser);
        project.setDeleted(true);
        projectRepository.save(project);
        operationAuditService.record(id, currentUser, "PROJECT_DELETE", "PROJECT", id,
                "删除项目 " + project.getName(), true);
    }

    private ProjectDtos.Response toResponse(Project project) {
        String ownerName = userRepository.findById(project.getOwnerId())
                .map(User::getDisplayName)
                .orElse("未知用户");
        long memberCount = projectMemberRepository.countByProjectId(project.getId());
        if (projectMemberRepository.findByProjectIdAndUserId(project.getId(), project.getOwnerId()).isEmpty()) {
            memberCount++;
        }
        return toResponse(project, ownerName, memberCount);
    }

    private ProjectDtos.Response toResponse(Project project, String ownerName, List<ProjectMember> members) {
        long memberCount = members.size();
        if (members.stream().noneMatch(member -> member.getUserId().equals(project.getOwnerId()))) {
            memberCount++;
        }
        return toResponse(project, ownerName, memberCount);
    }

    private ProjectDtos.Response toResponse(Project project, String ownerName, long memberCount) {
        return new ProjectDtos.Response(
                project.getId(),
                project.getName(),
                project.getCode(),
                project.getDescription(),
                project.getOwnerId(),
                ownerName,
                project.getStatus(),
                memberCount,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("RUNNING", "TESTING").contains(normalized)) {
            throw new BusinessException(40027, "项目状态不合法");
        }
        return normalized;
    }
}
