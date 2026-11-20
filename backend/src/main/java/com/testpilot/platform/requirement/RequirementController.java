package com.testpilot.platform.requirement;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @GetMapping({"/projects/{projectId}/requirements", "/projects/{projectId}/files"})
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'requirement:read')")
    public ApiResponse<List<RequirementDtos.Response>> list(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(requirementService.list(
                projectId,
                user
        ));
    }

    @GetMapping({"/projects/{projectId}/requirements/{requirementId}", "/projects/{projectId}/files/{requirementId}"})
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'requirement:read')")
    public ApiResponse<RequirementDtos.DetailResponse> get(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(requirementService.get(
                projectId,
                requirementId,
                user
        ));
    }

    @PostMapping("/projects/{projectId}/requirements/upload")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'requirement:write')")
    public ApiResponse<RequirementDtos.Response> upload(
            @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "version", required = false, defaultValue = "V1.0") String version,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(requirementService.upload(
                projectId,
                file,
                version,
                user
        ));
    }

    @PostMapping("/projects/{projectId}/files/upload")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'requirement:write')")
    public ApiResponse<RequirementDtos.Response> uploadFile(
            @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "version", required = false, defaultValue = "V1.0") String version,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(requirementService.upload(
                projectId,
                file,
                version,
                user,
                true
        ));
    }

    @PostMapping({"/projects/{projectId}/requirements/{requirementId}/parse", "/projects/{projectId}/files/{requirementId}/parse"})
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'requirement:write')")
    public ApiResponse<RequirementDtos.Response> parse(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(requirementService.parse(
                projectId,
                requirementId,
                user
        ));
    }

    @DeleteMapping({"/projects/{projectId}/requirements/{requirementId}", "/projects/{projectId}/files/{requirementId}"})
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'requirement:write')")
    public ApiResponse<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @AuthenticationPrincipal User user
    ) {
        requirementService.delete(projectId, requirementId, user);
        return ApiResponse.success(null);
    }
}
