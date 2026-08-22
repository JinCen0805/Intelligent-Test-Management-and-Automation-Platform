package com.testpilot.platform.project;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.user.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final RbacService rbacService;

    public ProjectAccessService(
            ProjectRepository projectRepository,
            RbacService rbacService
    ) {
        this.projectRepository = projectRepository;
        this.rbacService = rbacService;
    }

    public Project requireProject(Long projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new BusinessException(40401, "项目不存在"));
    }

    public Project requireRead(Long projectId, User user) {
        Project project = requireProject(projectId);
        if (!canRead(project, user)) {
            throw new AccessDeniedException("没有项目访问权限");
        }
        return project;
    }

    public Project requireWrite(Long projectId, User user) {
        return requirePermission(projectId, user, RbacPermissions.PROJECT_UPDATE, "没有项目修改权限");
    }

    public Project requirePermission(Long projectId, User user, String permissionCode) {
        return requirePermission(projectId, user, permissionCode, "没有项目权限");
    }

    public boolean canRead(Project project, User user) {
        return hasProjectPermission(project, user, RbacPermissions.PROJECT_READ);
    }

    public boolean canWrite(Project project, User user) {
        return hasProjectPermission(project, user, RbacPermissions.PROJECT_UPDATE);
    }

    public boolean isAdmin(User user) {
        return user != null && rbacService.hasSystemPermission(user.getId(), RbacPermissions.SYSTEM_USER_MANAGE);
    }

    private Project requirePermission(Long projectId, User user, String permissionCode, String deniedMessage) {
        Project project = requireProject(projectId);
        if (!hasProjectPermission(project, user, permissionCode)) {
            throw new AccessDeniedException(deniedMessage);
        }
        return project;
    }

    private boolean hasProjectPermission(Project project, User user, String permissionCode) {
        if (user == null) {
            return false;
        }
        return isAdmin(user)
                || project.getOwnerId().equals(user.getId())
                || rbacService.hasProjectPermission(user.getId(), project.getId(), permissionCode);
    }
}
