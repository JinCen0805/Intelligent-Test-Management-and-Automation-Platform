package com.testpilot.platform.ai;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
        "/api/projects/{projectId}/requirements/{requirementId}/test-cases",
        "/api/projects/{projectId}/files/{requirementId}/test-cases"
})
@PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'ai:generate')")
public class AiGenerationController {

    private final AiTaskService aiTaskService;

    public AiGenerationController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    @PostMapping("/generate")
    public ApiResponse<AiTaskDtos.SubmitResponse> generate(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @Valid @RequestBody(required = false) AiDtos.GenerateRequest request,
            @AuthenticationPrincipal User user
    ) {
        AiDtos.GenerateRequest effectiveRequest = request == null
                ? new AiDtos.GenerateRequest(null, null, AiGenerationService.SUITE_FUNCTIONAL, null)
                : request;
        return ApiResponse.success(aiTaskService.submitTestCases(
                projectId,
                requirementId,
                effectiveRequest,
                user
        ));
    }
}
