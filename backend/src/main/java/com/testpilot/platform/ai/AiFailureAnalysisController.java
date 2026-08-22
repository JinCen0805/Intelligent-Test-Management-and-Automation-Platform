package com.testpilot.platform.ai;

import com.testpilot.platform.auth.AuthService;
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

    private final AiFailureAnalysisService service;
    private final AuthService authService;

    public AiFailureAnalysisController(AiFailureAnalysisService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<AiAnalysisDtos.AnalysisResponse> analyze(
            @PathVariable Long projectId,
            @PathVariable Long executionId,
            @RequestBody(required = false) AiAnalysisDtos.AnalyzeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.analyze(
                projectId,
                executionId,
                request,
                authService.currentUser(authorization)
        ));
    }
}
