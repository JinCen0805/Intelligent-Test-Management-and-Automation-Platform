package com.testpilot.platform.project;

import com.testpilot.platform.auth.AuthService;
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
    private final AuthService authService;

    public ProjectMemberController(ProjectMemberService projectMemberService, AuthService authService) {
        this.projectMemberService = projectMemberService;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'project:read')")
    public ApiResponse<List<ProjectMemberDtos.Response>> list(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(projectMemberService.list(
                projectId,
                authService.currentUser(authorization)
        ));
    }

    @GetMapping("/candidates")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'project:member:manage')")
    public ApiResponse<List<UserDtos.Response>> candidates(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(projectMemberService.candidates(
                projectId,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'project:member:manage')")
    public ApiResponse<ProjectMemberDtos.Response> add(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectMemberDtos.UpsertRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(projectMemberService.add(
                projectId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'project:member:manage')")
    public ApiResponse<ProjectMemberDtos.Response> update(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @Valid @RequestBody ProjectMemberDtos.UpsertRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(projectMemberService.update(
                projectId,
                userId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'project:member:manage')")
    public ApiResponse<Void> remove(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        projectMemberService.remove(
                projectId,
                userId,
                authService.currentUser(authorization)
        );
        return ApiResponse.success();
    }
}
