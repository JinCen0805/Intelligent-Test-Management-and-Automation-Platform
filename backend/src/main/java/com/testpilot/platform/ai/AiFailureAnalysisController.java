package com.testpilot.platform.ai;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

import com.testpilot.platform.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/test-executions/{executionId}/analyze")
@PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'ai:analyze')")
public class AiFailureAnalysisController {

    private final AiTaskService aiTaskService;

    public AiFailureAnalysisController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    @PostMapping
    public ApiResponse<AiTaskDtos.SubmitResponse> analyze(
            @PathVariable Long projectId,
            @PathVariable Long executionId,
            @RequestBody(required = false) AiAnalysisDtos.AnalyzeRequest request,
            @AuthenticationPrincipal User user
    ) {
        AiAnalysisDtos.AnalyzeRequest effectiveRequest = request == null
                ? new AiAnalysisDtos.AnalyzeRequest(null, null)
                : request;
        return ApiResponse.success(aiTaskService.submitAnalysis(
                projectId,
                executionId,
                effectiveRequest,
                user
        ));
    }
}
