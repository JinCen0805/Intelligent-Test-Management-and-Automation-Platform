package com.testpilot.platform.automation;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
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
    private final AuthService authService;

    public JenkinsAdminController(JenkinsAdminService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<JenkinsAdminDtos.InstanceResponse>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.listInstances(authService.currentUser(authorization)));
    }

    @PostMapping
    public ApiResponse<JenkinsAdminDtos.InstanceResponse> create(
            @Valid @RequestBody JenkinsAdminDtos.InstanceRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.createInstance(request, authService.currentUser(authorization)));
    }

    @PutMapping("/{instanceId}")
    public ApiResponse<JenkinsAdminDtos.InstanceResponse> update(
            @PathVariable Long instanceId,
            @Valid @RequestBody JenkinsAdminDtos.InstanceRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.updateInstance(
                instanceId, request, authService.currentUser(authorization)
        ));
    }

    @PostMapping("/{instanceId}/status")
    public ApiResponse<JenkinsAdminDtos.InstanceResponse> changeStatus(
            @PathVariable Long instanceId,
            @Valid @RequestBody JenkinsAdminDtos.StatusRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.changeStatus(
                instanceId, request, authService.currentUser(authorization)
        ));
    }

    @DeleteMapping("/{instanceId}")
    public ApiResponse<Void> delete(
            @PathVariable Long instanceId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        service.deleteInstance(instanceId, authService.currentUser(authorization));
        return ApiResponse.success(null);
    }

    @PostMapping("/{instanceId}/test-connection")
    public ApiResponse<JenkinsAdminDtos.InstanceResponse> testConnection(
            @PathVariable Long instanceId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.testConnection(
                instanceId, authService.currentUser(authorization)
        ));
    }

}
