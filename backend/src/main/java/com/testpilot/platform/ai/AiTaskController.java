package com.testpilot.platform.ai;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-tasks")
public class AiTaskController {

    private final AiTaskService aiTaskService;
    private final AuthService authService;

    public AiTaskController(AiTaskService aiTaskService, AuthService authService) {
        this.aiTaskService = aiTaskService;
        this.authService = authService;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<AiTaskDtos.StatusResponse> status(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiTaskService.status(taskId, authService.currentUser(authorization)));
    }
}
