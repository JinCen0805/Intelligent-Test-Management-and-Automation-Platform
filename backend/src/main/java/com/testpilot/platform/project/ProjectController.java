package com.testpilot.platform.project;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

import com.testpilot.platform.common.ApiResponse;
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
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ProjectDtos.Response>> list(
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(projectService.list(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #id, 'project:read')")
    public ApiResponse<ProjectDtos.Response> get(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(projectService.get(id, user));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProjectDtos.Response> create(
            @Valid @RequestBody ProjectDtos.CreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(projectService.create(request, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #id, 'project:update')")
    public ApiResponse<ProjectDtos.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectDtos.UpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(projectService.update(
                id,
                request,
                user
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #id, 'project:update')")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        projectService.delete(id, user);
        return ApiResponse.success();
    }
}
