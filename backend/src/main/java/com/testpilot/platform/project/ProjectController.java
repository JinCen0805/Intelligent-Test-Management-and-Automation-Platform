package com.testpilot.platform.project;

import com.testpilot.platform.auth.AuthService;
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
    private final AuthService authService;

    public ProjectController(ProjectService projectService, AuthService authService) {
        this.projectService = projectService;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ProjectDtos.Response>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(projectService.list(authService.currentUser(authorization)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #id, 'project:read')")
    public ApiResponse<ProjectDtos.Response> get(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(projectService.get(id, authService.currentUser(authorization)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProjectDtos.Response> create(
            @Valid @RequestBody ProjectDtos.CreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(projectService.create(request, authService.currentUser(authorization)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #id, 'project:update')")
    public ApiResponse<ProjectDtos.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectDtos.UpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(projectService.update(
                id,
                request,
                authService.currentUser(authorization)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #id, 'project:update')")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        projectService.delete(id, authService.currentUser(authorization));
        return ApiResponse.success();
    }
}
