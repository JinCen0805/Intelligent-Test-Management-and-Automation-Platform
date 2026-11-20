package com.testpilot.platform.ai;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("@rbacAuthorizationService.hasSystemPermission(authentication, 'system:ai-model:manage')")
public class AiModelController {

    private final AiModelService aiModelService;

    public AiModelController(AiModelService aiModelService) {
        this.aiModelService = aiModelService;
    }

    @GetMapping("/models")
    public ApiResponse<List<AiDtos.ModelResponse>> listModels(
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiModelService.listModels(user));
    }

    @PostMapping("/models")
    public ApiResponse<AiDtos.ModelResponse> createModel(
            @Valid @RequestBody AiDtos.ModelRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiModelService.createModel(
                request,
                user
        ));
    }

    @PutMapping("/models/{id}")
    public ApiResponse<AiDtos.ModelResponse> updateModel(
            @PathVariable Long id,
            @Valid @RequestBody AiDtos.ModelRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiModelService.updateModel(
                id,
                request,
                user
        ));
    }

    @PutMapping("/models/{id}/enabled")
    public ApiResponse<AiDtos.ModelResponse> setModelEnabled(
            @PathVariable Long id,
            @Valid @RequestBody AiDtos.ModelEnabledRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiModelService.setModelEnabled(
                id,
                request.enabled(),
                user
        ));
    }

    @PostMapping("/models/{id}/test")
    public ApiResponse<Void> testConnection(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        aiModelService.testConnection(id, user);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/models/{id}")
    public ApiResponse<Void> deleteModel(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        aiModelService.deleteModel(id, user);
        return ApiResponse.success(null);
    }

    @GetMapping("/prompts")
    public ApiResponse<List<AiDtos.PromptResponse>> listPrompts(
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiModelService.listPrompts(user));
    }

    @PostMapping("/prompts")
    public ApiResponse<AiDtos.PromptResponse> createPrompt(
            @Valid @RequestBody AiDtos.PromptRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiModelService.createPrompt(
                request,
                user
        ));
    }

    @PutMapping("/prompts/{id}")
    public ApiResponse<AiDtos.PromptResponse> updatePrompt(
            @PathVariable Long id,
            @Valid @RequestBody AiDtos.PromptRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiModelService.updatePrompt(
                id,
                request,
                user
        ));
    }

    @PutMapping("/prompts/{id}/enabled")
    public ApiResponse<AiDtos.PromptResponse> setPromptEnabled(
            @PathVariable Long id,
            @Valid @RequestBody AiDtos.PromptEnabledRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiModelService.setPromptEnabled(
                id,
                request.enabled(),
                user
        ));
    }

    @PostMapping("/prompts/{id}/publish")
    public ApiResponse<AiDtos.PromptResponse> publishPrompt(
            @PathVariable Long id,
            @RequestBody(required = false) AiDtos.PromptPublishRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiModelService.publishPrompt(
                id,
                request == null ? null : request.version(),
                user
        ));
    }

    @PostMapping("/prompts/{id}/activate")
    public ApiResponse<AiDtos.PromptResponse> activatePrompt(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(aiModelService.activatePrompt(
                id,
                user
        ));
    }
}
