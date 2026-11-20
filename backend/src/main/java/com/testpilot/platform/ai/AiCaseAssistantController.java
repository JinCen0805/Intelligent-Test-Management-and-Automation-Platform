package com.testpilot.platform.ai;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/test-cases")
@PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'ai:generate')")
public class AiCaseAssistantController {

    private final AiCaseAssistantService service;
    private final AiCaseChatSessionService sessionService;

    public AiCaseAssistantController(AiCaseAssistantService service, AiCaseChatSessionService sessionService) {
        this.service = service;
        this.sessionService = sessionService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiDtos.CaseChatResponse> chat(
            @PathVariable Long projectId,
            @Valid @RequestBody AiDtos.CaseChatRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.chat(projectId, request, user));
    }

    @PostMapping(value = "/chat/file", consumes = "multipart/form-data")
    public ApiResponse<AiDtos.CaseChatResponse> chatWithFile(
            @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "message", required = false, defaultValue = "请根据这个文件内容生成测试用例。") String message,
            @RequestParam(value = "suiteType", required = false) String suiteType,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam(value = "modelId", required = false) Long modelId,
            @RequestParam(value = "selectedCaseId", required = false) Long selectedCaseId,
            @AuthenticationPrincipal User user
    ) {
        AiDtos.CaseChatRequest request = new AiDtos.CaseChatRequest(
                sessionId, modelId, null, selectedCaseId, suiteType, message
        );
        return ApiResponse.success(service.chatWithFile(projectId, request, file, user));
    }

    @GetMapping("/chat/history")
    public ApiResponse<List<AiDtos.CaseChatSessionResponse>> history(
            @PathVariable Long projectId,
            @RequestParam(value = "suiteType", required = false) String suiteType,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(sessionService.list(projectId, suiteType, user));
    }

    @DeleteMapping("/chat/history/{sessionId}")
    public ApiResponse<Void> deleteHistory(
            @PathVariable Long projectId,
            @PathVariable Long sessionId,
            @RequestParam(value = "suiteType", required = false) String suiteType,
            @AuthenticationPrincipal User user
    ) {
        sessionService.delete(projectId, suiteType, sessionId, user);
        return ApiResponse.success();
    }

    @DeleteMapping("/chat/history")
    public ApiResponse<Void> deleteAllHistory(
            @PathVariable Long projectId,
            @RequestParam(value = "suiteType", required = false) String suiteType,
            @AuthenticationPrincipal User user
    ) {
        sessionService.deleteAll(projectId, suiteType, user);
        return ApiResponse.success();
    }
}
