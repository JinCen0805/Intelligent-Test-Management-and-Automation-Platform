package com.testpilot.platform.automation;

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
@RequestMapping("/api/admin/jenkins-instances")
public class JenkinsAdminController {

    private final JenkinsAdminService service;

    public JenkinsAdminController(JenkinsAdminService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@rbacAuthorizationService.hasSystemPermission(authentication, 'system:jenkins:manage')")
    public ApiResponse<List<JenkinsAdminDtos.InstanceResponse>> list(
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.listInstances(user));
    }

    @PostMapping
    @PreAuthorize("@rbacAuthorizationService.hasSystemPermission(authentication, 'system:jenkins:manage')")
    public ApiResponse<JenkinsAdminDtos.InstanceResponse> create(
            @Valid @RequestBody JenkinsAdminDtos.InstanceRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.createInstance(request, user));
    }

    @PutMapping("/{instanceId}")
    @PreAuthorize("@rbacAuthorizationService.hasSystemPermission(authentication, 'system:jenkins:manage')")
    public ApiResponse<JenkinsAdminDtos.InstanceResponse> update(
            @PathVariable Long instanceId,
            @Valid @RequestBody JenkinsAdminDtos.InstanceRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.updateInstance(
                instanceId, request, user
        ));
    }

    @PostMapping("/{instanceId}/status")
    @PreAuthorize("@rbacAuthorizationService.hasSystemPermission(authentication, 'system:jenkins:manage')")
    public ApiResponse<JenkinsAdminDtos.InstanceResponse> changeStatus(
            @PathVariable Long instanceId,
            @Valid @RequestBody JenkinsAdminDtos.StatusRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.changeStatus(
                instanceId, request, user
        ));
    }

    @DeleteMapping("/{instanceId}")
    @PreAuthorize("@rbacAuthorizationService.hasSystemPermission(authentication, 'system:jenkins:manage')")
    public ApiResponse<Void> delete(
            @PathVariable Long instanceId,
            @AuthenticationPrincipal User user
    ) {
        service.deleteInstance(instanceId, user);
        return ApiResponse.success(null);
    }

    @PostMapping("/{instanceId}/test-connection")
    @PreAuthorize("@rbacAuthorizationService.hasSystemPermission(authentication, 'system:jenkins:manage')")
    public ApiResponse<JenkinsAdminDtos.InstanceResponse> testConnection(
            @PathVariable Long instanceId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.testConnection(
                instanceId, user
        ));
    }

}
