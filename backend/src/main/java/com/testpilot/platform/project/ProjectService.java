package com.testpilot.platform.project;

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

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectAccessService projectAccessService
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectAccessService = projectAccessService;
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
        if (projectRepository.existsByCodeAndDeletedFalse(code)) {
            throw new BusinessException(40901, "项目标识已存在");
        }
        String environment = request.defaultEnvironment() == null || request.defaultEnvironment().isBlank()
                ? "TEST"
                : request.defaultEnvironment().trim().toUpperCase(Locale.ROOT);
        Project project = new Project(
                request.name().trim(),
                code,
                request.description(),
                currentUser.getId(),
                environment,
                "TESTING"
        );
        Project saved = projectRepository.save(project);
        projectMemberRepository.save(new ProjectMember(saved.getId(), currentUser.getId(), "OWNER"));
        return toResponse(saved);
    }

    @Transactional
    public ProjectDtos.Response update(Long id, ProjectDtos.UpdateRequest request, User currentUser) {
        Project project = projectAccessService.requireWrite(id, currentUser);
        project.setName(request.name().trim());
        project.setDescription(request.description());
        if (request.defaultEnvironment() != null && !request.defaultEnvironment().isBlank()) {
            project.setDefaultEnvironment(request.defaultEnvironment().trim().toUpperCase(Locale.ROOT));
        }
        if (request.status() != null && !request.status().isBlank()) {
            project.setStatus(request.status().trim().toUpperCase(Locale.ROOT));
        }
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Project project = projectAccessService.requireWrite(id, currentUser);
        project.setDeleted(true);
        projectRepository.save(project);
    }

    private ProjectDtos.Response toResponse(Project project) {
        String ownerName = userRepository.findById(project.getOwnerId())
                .map(User::getDisplayName)
                .orElse("未知用户");
        return new ProjectDtos.Response(
                project.getId(),
                project.getName(),
                project.getCode(),
                project.getDescription(),
                project.getOwnerId(),
                ownerName,
                project.getDefaultEnvironment(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
