package com.testpilot.platform.ai;

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

    @PutMapping("/models/{id}")
    public ApiResponse<AiDtos.ModelResponse> updateModel(
            @PathVariable Long id,
            @Valid @RequestBody AiDtos.ModelRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiModelService.updateModel(
                id,
                request,
                authService.currentUser(authorization)
        ));
    }

    @PutMapping("/models/{id}/enabled")
    public ApiResponse<AiDtos.ModelResponse> setModelEnabled(
            @PathVariable Long id,
            @Valid @RequestBody AiDtos.ModelEnabledRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiModelService.setModelEnabled(
                id,
                request.enabled(),
                authService.currentUser(authorization)
        ));
    }

    @DeleteMapping("/models/{id}")
    public ApiResponse<Void> deleteModel(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        aiModelService.deleteModel(id, authService.currentUser(authorization));
        return ApiResponse.success(null);
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

    @PutMapping("/prompts/{id}")
    public ApiResponse<AiDtos.PromptResponse> updatePrompt(
            @PathVariable Long id,
            @Valid @RequestBody AiDtos.PromptRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiModelService.updatePrompt(
                id,
                request,
                authService.currentUser(authorization)
        ));
    }

    @PutMapping("/prompts/{id}/enabled")
    public ApiResponse<AiDtos.PromptResponse> setPromptEnabled(
            @PathVariable Long id,
            @Valid @RequestBody AiDtos.PromptEnabledRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiModelService.setPromptEnabled(
                id,
                request.enabled(),
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/prompts/{id}/publish")
    public ApiResponse<AiDtos.PromptResponse> publishPrompt(
            @PathVariable Long id,
            @RequestBody(required = false) AiDtos.PromptPublishRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiModelService.publishPrompt(
                id,
                request == null ? null : request.version(),
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/prompts/{id}/activate")
    public ApiResponse<AiDtos.PromptResponse> activatePrompt(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiModelService.activatePrompt(
                id,
                authService.currentUser(authorization)
        ));
    }
}
