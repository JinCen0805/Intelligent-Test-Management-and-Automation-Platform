package com.testpilot.platform.project;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

import com.testpilot.platform.common.ApiResponse;
import com.testpilot.platform.user.UserDtos;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @GetMapping
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'project:read')")
    public ApiResponse<List<ProjectMemberDtos.Response>> list(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(projectMemberService.list(
                projectId,
                user
        ));
    }

    @GetMapping("/candidates")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'project:member:manage')")
    public ApiResponse<List<UserDtos.Response>> candidates(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(projectMemberService.candidates(
                projectId,
                user
        ));
    }

    @PostMapping
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'project:member:manage')")
    public ApiResponse<ProjectMemberDtos.Response> add(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectMemberDtos.UpsertRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(projectMemberService.add(
                projectId,
                request,
                user
        ));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'project:member:manage')")
    public ApiResponse<ProjectMemberDtos.Response> update(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @Valid @RequestBody ProjectMemberDtos.UpsertRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(projectMemberService.update(
                projectId,
                userId,
                request,
                user
        ));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'project:member:manage')")
    public ApiResponse<Void> remove(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User user
    ) {
        projectMemberService.remove(
                projectId,
                userId,
                user
        );
        return ApiResponse.success();
    }
}
