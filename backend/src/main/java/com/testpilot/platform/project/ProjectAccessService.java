package com.testpilot.platform.project;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.user.User;
import org.springframework.stereotype.Service;

@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAccessService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public Project requireProject(Long projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new BusinessException(40401, "项目不存在"));
    }

    public Project requireRead(Long projectId, User user) {
        Project project = requireProject(projectId);
        if (!canRead(project, user)) {
            throw new BusinessException(40301, "没有项目访问权限");
        }
        return project;
    }

    public Project requireWrite(Long projectId, User user) {
        Project project = requireProject(projectId);
        if (!canWrite(project, user)) {
            throw new BusinessException(40302, "没有项目修改权限");
        }
        return project;
    }

    public boolean canRead(Project project, User user) {
        return isAdmin(user)
                || project.getOwnerId().equals(user.getId())
                || projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
    }

    public boolean canWrite(Project project, User user) {
        return isAdmin(user) || project.getOwnerId().equals(user.getId());
    }

    public boolean isAdmin(User user) {
        return "ADMIN".equalsIgnoreCase(user.getRole());
    }
}
