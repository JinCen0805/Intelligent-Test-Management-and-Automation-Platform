package com.testpilot.platform.automation;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class AutomationController {

    private final AutomationService automationService;
    private final AuthService authService;

    public AutomationController(AutomationService automationService, AuthService authService) {
        this.automationService = automationService;
        this.authService = authService;
    }

    @GetMapping("/automation-projects")
    public ApiResponse<List<AutomationDtos.AutomationProjectResponse>> listAutomationProjects(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.listAutomationProjects(
                projectId,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/automation-projects")
    public ApiResponse<AutomationDtos.AutomationProjectResponse> createAutomationProject(
            @PathVariable Long projectId,
            @Valid @RequestBody AutomationDtos.AutomationProjectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.createAutomationProject(
                projectId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @GetMapping("/test-tasks")
    public ApiResponse<List<AutomationDtos.TestTaskResponse>> listTasks(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.listTasks(
                projectId,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/test-tasks")
    public ApiResponse<AutomationDtos.TestTaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody AutomationDtos.TestTaskRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.createTask(
                projectId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @GetMapping("/test-executions")
    public ApiResponse<List<AutomationDtos.TestExecutionResponse>> listExecutions(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.listExecutions(
                projectId,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/test-tasks/{taskId}/execute")
    public ApiResponse<AutomationDtos.TestExecutionResponse> execute(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.execute(
                projectId,
                taskId,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/test-executions/{executionId}/result")
    public ApiResponse<AutomationDtos.TestExecutionResponse> updateResult(
            @PathVariable Long projectId,
            @PathVariable Long executionId,
            @Valid @RequestBody AutomationDtos.ExecutionResultRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.updateResult(
                projectId,
                executionId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/test-executions/{executionId}/sync")
    public ApiResponse<AutomationDtos.TestExecutionResponse> sync(
            @PathVariable Long projectId,
            @PathVariable Long executionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.sync(
                projectId,
                executionId,
                authService.currentUser(authorization)
        ));
    }
}
