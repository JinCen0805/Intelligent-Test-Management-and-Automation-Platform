package com.testpilot.platform.ai;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/automation-tasks")
@PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'ai:generate')")
public class AiAutomationTaskController {

    private final AiAutomationTaskService service;
    private final AuthService authService;

    public AiAutomationTaskController(AiAutomationTaskService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PostMapping("/ai-generate")
    public ApiResponse<AiAutomationDtos.DraftResponse> generate(
            @PathVariable Long projectId,
            @Valid @RequestBody AiAutomationDtos.GenerateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.generate(
                projectId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @GetMapping("/ai-drafts/{draftId}")
    public ApiResponse<AiAutomationDtos.DraftResponse> get(
            @PathVariable Long projectId,
            @PathVariable Long draftId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.get(
                projectId,
                draftId,
                authService.currentUser(authorization)
        ));
    }

    @PutMapping("/ai-drafts/{draftId}")
    public ApiResponse<AiAutomationDtos.DraftResponse> update(
            @PathVariable Long projectId,
            @PathVariable Long draftId,
            @Valid @RequestBody AiAutomationDtos.DraftUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.update(
                projectId,
                draftId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/ai-drafts/{draftId}/confirm")
    public ApiResponse<AiAutomationDtos.ConfirmResponse> confirm(
            @PathVariable Long projectId,
            @PathVariable Long draftId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.confirm(
                projectId,
                draftId,
                authService.currentUser(authorization)
        ));
    }
}
