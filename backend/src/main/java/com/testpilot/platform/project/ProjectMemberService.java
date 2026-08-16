package com.testpilot.platform.project;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.user.User;
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

    public ProjectMemberService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            ProjectAccessService projectAccessService
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
    }

    public List<ProjectMemberDtos.Response> list(Long projectId, User currentUser) {
        Project project = projectAccessService.requireRead(projectId, currentUser);
        List<ProjectMemberDtos.Response> result = new ArrayList<>();
        if (projectMemberRepository.findByProjectIdAndUserId(projectId, project.getOwnerId()).isEmpty()) {
            userRepository.findById(project.getOwnerId()).ifPresent(owner ->
                    result.add(toResponse(null, owner, "OWNER")));
        }
        result.addAll(projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(projectId)
                .stream()
                .map(this::toResponse)
                .toList());
        return result;
    }

    @Transactional
    public ProjectMemberDtos.Response add(
            Long projectId,
            ProjectMemberDtos.UpsertRequest request,
            User currentUser
    ) {
        projectAccessService.requireWrite(projectId, currentUser);
        User member = enabledUser(request.userId());
        if (projectMemberRepository.findByProjectIdAndUserId(projectId, member.getId()).isPresent()) {
            throw new BusinessException(40904, "项目成员已存在");
        }
        ProjectMember saved = projectMemberRepository.save(new ProjectMember(
                projectId,
                member.getId(),
                normalizeRole(request.role())
        ));
        return toResponse(saved);
    }

    @Transactional
    public ProjectMemberDtos.Response update(
            Long projectId,
            Long userId,
            ProjectMemberDtos.UpsertRequest request,
            User currentUser
    ) {
        projectAccessService.requireWrite(projectId, currentUser);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(40405, "项目成员不存在"));
        member.setRole(normalizeRole(request.role()));
        return toResponse(projectMemberRepository.save(member));
    }

    @Transactional
    public void remove(Long projectId, Long userId, User currentUser) {
        Project project = projectAccessService.requireWrite(projectId, currentUser);
        if (project.getOwnerId().equals(userId)) {
            throw new BusinessException(40016, "项目负责人不能移除");
        }
        if (projectMemberRepository.findByProjectIdAndUserId(projectId, userId).isEmpty()) {
            throw new BusinessException(40405, "项目成员不存在");
        }
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
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
        if (!List.of("MANAGER", "TEST_DEVELOPER", "VIEWER").contains(normalized)) {
            throw new BusinessException(40017, "项目成员角色不合法");
        }
        return normalized;
    }
}
