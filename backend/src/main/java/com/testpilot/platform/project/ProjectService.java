package com.testpilot.platform.project;

import com.testpilot.platform.audit.OperationAuditService;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessService projectAccessService;
    private final OperationAuditService operationAuditService;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectAccessService projectAccessService,
            OperationAuditService operationAuditService
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectAccessService = projectAccessService;
        this.operationAuditService = operationAuditService;
    }

    public List<ProjectDtos.Response> list(User currentUser) {
        return projectRepository.findAllByDeletedFalseOrderByCreatedAtDesc().stream()
                .filter(project -> projectAccessService.canRead(project, currentUser))
                .map(this::toResponse)
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
        if (projectRepository.existsByCodeAndDeletedFalse(code)) {
            throw new BusinessException(40901, "项目标识已存在");
        }
        Project project = new Project(
                request.name().trim(),
                code,
                request.description(),
                currentUser.getId(),
                "TESTING"
        );
        Project saved = projectRepository.save(project);
        projectMemberRepository.save(new ProjectMember(saved.getId(), currentUser.getId(), "OWNER"));
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
            project.setStatus(request.status().trim().toUpperCase(Locale.ROOT));
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
}
