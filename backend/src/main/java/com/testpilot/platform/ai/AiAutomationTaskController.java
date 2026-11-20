package com.testpilot.platform.ai;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

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
    private final AiTaskService aiTaskService;

    public AiAutomationTaskController(
            AiAutomationTaskService service,
            AiTaskService aiTaskService
    ) {
        this.service = service;
        this.aiTaskService = aiTaskService;
    }

    @PostMapping("/ai-generate")
    public ApiResponse<AiTaskDtos.SubmitResponse> generate(
            @PathVariable Long projectId,
            @Valid @RequestBody AiAutomationDtos.GenerateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiTaskService.submitAutomationTask(
                projectId,
                request,
                user
        ));
    }

    @GetMapping("/ai-drafts/{draftId}")
    public ApiResponse<AiAutomationDtos.DraftResponse> get(
            @PathVariable Long projectId,
            @PathVariable Long draftId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.get(
                projectId,
                draftId,
                user
        ));
    }

    @PutMapping("/ai-drafts/{draftId}")
    public ApiResponse<AiAutomationDtos.DraftResponse> update(
            @PathVariable Long projectId,
            @PathVariable Long draftId,
            @Valid @RequestBody AiAutomationDtos.DraftUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.update(
                projectId,
                draftId,
                request,
                user
        ));
    }

    @PostMapping("/ai-drafts/{draftId}/confirm")
    public ApiResponse<AiAutomationDtos.ConfirmResponse> confirm(
            @PathVariable Long projectId,
            @PathVariable Long draftId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.confirm(
                projectId,
                draftId,
                user
        ));
    }
}
