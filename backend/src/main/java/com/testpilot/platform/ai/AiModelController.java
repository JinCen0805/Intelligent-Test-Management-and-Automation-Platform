package com.testpilot.platform.ai;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiModelController {

    private final AiModelService aiModelService;
    private final AuthService authService;

    public AiModelController(AiModelService aiModelService, AuthService authService) {
        this.aiModelService = aiModelService;
        this.authService = authService;
    }

    @GetMapping("/models")
    public ApiResponse<List<AiDtos.ModelResponse>> listModels(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiModelService.listModels(authService.currentUser(authorization)));
    }

    @PostMapping("/models")
    public ApiResponse<AiDtos.ModelResponse> createModel(
            @Valid @RequestBody AiDtos.ModelRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiModelService.createModel(
                request,
                authService.currentUser(authorization)
        ));
    }

    @GetMapping("/prompts")
    public ApiResponse<List<AiDtos.PromptResponse>> listPrompts(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiModelService.listPrompts(authService.currentUser(authorization)));
    }

    @PostMapping("/prompts")
    public ApiResponse<AiDtos.PromptResponse> createPrompt(
            @Valid @RequestBody AiDtos.PromptRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiModelService.createPrompt(
                request,
                authService.currentUser(authorization)
        ));
    }
}
