package com.testpilot.platform.ai;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/requirements/{requirementId}/test-cases")
public class AiGenerationController {

    private final AiGenerationService aiGenerationService;
    private final AuthService authService;

    public AiGenerationController(AiGenerationService aiGenerationService, AuthService authService) {
        this.aiGenerationService = aiGenerationService;
        this.authService = authService;
    }

    @PostMapping("/generate")
    public ApiResponse<AiDtos.GenerateResponse> generate(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @Valid @RequestBody(required = false) AiDtos.GenerateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiGenerationService.generate(
                projectId,
                requirementId,
                request == null ? new AiDtos.GenerateRequest(null, null, null) : request,
                authService.currentUser(authorization)
        ));
    }
}
