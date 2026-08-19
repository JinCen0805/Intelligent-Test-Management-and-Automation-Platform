package com.testpilot.platform.automation;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class AutomationController {

    private final AutomationService automationService;
    private final JenkinsAdminService jenkinsAdminService;
    private final AutomationOnboardingService onboardingService;
    private final JenkinsProtocolService protocolService;
    private final AuthService authService;

    public AutomationController(
            AutomationService automationService,
            JenkinsAdminService jenkinsAdminService,
            AutomationOnboardingService onboardingService,
            JenkinsProtocolService protocolService,
            AuthService authService
    ) {
        this.automationService = automationService;
        this.jenkinsAdminService = jenkinsAdminService;
        this.onboardingService = onboardingService;
        this.protocolService = protocolService;
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

    @GetMapping("/jenkins-credential-grants")
    public ApiResponse<List<JenkinsAdminDtos.CredentialGrantResponse>> listCredentialGrants(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(jenkinsAdminService.listCredentialGrants(
                projectId, authService.currentUser(authorization)
        ));
    }

    @PostMapping("/jenkins-credential-grants")
    public ApiResponse<JenkinsAdminDtos.CredentialGrantResponse> createCredentialGrant(
            @PathVariable Long projectId,
            @Valid @RequestBody JenkinsAdminDtos.CredentialGrantRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(jenkinsAdminService.createCredentialGrant(
                projectId, request, authService.currentUser(authorization)
        ));
    }

    @DeleteMapping("/jenkins-credential-grants/{grantId}")
    public ApiResponse<Void> revokeCredentialGrant(
            @PathVariable Long projectId,
            @PathVariable Long grantId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        jenkinsAdminService.revokeCredentialGrant(
                projectId, grantId, authService.currentUser(authorization)
        );
        return ApiResponse.success(null);
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

    @PutMapping("/automation-projects/{automationProjectId}")
    public ApiResponse<AutomationDtos.AutomationProjectResponse> updateAutomationProject(
            @PathVariable Long projectId,
            @PathVariable Long automationProjectId,
            @Valid @RequestBody AutomationDtos.AutomationProjectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.updateAutomationProject(
                projectId,
                automationProjectId,
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

    @PutMapping("/test-tasks/{taskId}")
    public ApiResponse<AutomationDtos.TestTaskResponse> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody AutomationDtos.TestTaskRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.updateTask(
                projectId,
                taskId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @DeleteMapping("/test-tasks/{taskId}")
    public ApiResponse<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        automationService.deleteTask(projectId, taskId, authService.currentUser(authorization));
        return ApiResponse.success(null);
    }

    @PostMapping("/test-tasks/{taskId}/status")
    public ApiResponse<AutomationDtos.TestTaskResponse> updateTaskStatus(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody AutomationDtos.TestTaskStatusRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.updateTaskStatus(
                projectId,
                taskId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/test-tasks/{taskId}/stop")
    public ApiResponse<AutomationDtos.TestExecutionResponse> stopTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.stopTask(
                projectId,
                taskId,
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

    @GetMapping("/automation-projects/{automationProjectId}/pipeline-bindings")
    public ApiResponse<List<JenkinsAdminDtos.BindingResponse>> listBindings(
            @PathVariable Long projectId,
            @PathVariable Long automationProjectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(jenkinsAdminService.listBindings(
                projectId, automationProjectId, authService.currentUser(authorization)
        ));
    }

    @GetMapping("/automation-projects/{automationProjectId}/jenkins-jobs")
    public ApiResponse<List<JenkinsAdminDtos.JobResponse>> listJenkinsJobs(
            @PathVariable Long projectId,
            @PathVariable Long automationProjectId,
            @RequestParam Long jenkinsInstanceId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(jenkinsAdminService.listJobs(
                projectId, automationProjectId, jenkinsInstanceId, authService.currentUser(authorization)
        ));
    }

    @PostMapping("/automation-projects/{automationProjectId}/jenkins-jobs/import")
    public ApiResponse<JenkinsAdminDtos.BindingResponse> importJenkinsJob(
            @PathVariable Long projectId,
            @PathVariable Long automationProjectId,
            @Valid @RequestBody JenkinsAdminDtos.JobImportRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(jenkinsAdminService.importJob(
                projectId, automationProjectId, request, authService.currentUser(authorization)
        ));
    }

    @PostMapping("/automation-projects/{automationProjectId}/jenkins-jobs/create")
    public ApiResponse<JenkinsAdminDtos.BindingResponse> createJenkinsJob(
            @PathVariable Long projectId,
            @PathVariable Long automationProjectId,
            @Valid @RequestBody JenkinsAdminDtos.CreateJobRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(jenkinsAdminService.createJobAndBind(
                projectId, automationProjectId, request, authService.currentUser(authorization)
        ));
    }

    @PostMapping("/automation-projects/{automationProjectId}/pipeline-bindings")
    public ApiResponse<JenkinsAdminDtos.BindingResponse> createBinding(
            @PathVariable Long projectId,
            @PathVariable Long automationProjectId,
            @Valid @RequestBody JenkinsAdminDtos.BindingRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(jenkinsAdminService.createBinding(
                projectId, automationProjectId, request, authService.currentUser(authorization)
        ));
    }

    @PutMapping("/pipeline-bindings/{bindingId}")
    public ApiResponse<JenkinsAdminDtos.BindingResponse> updateBinding(
            @PathVariable Long projectId,
            @PathVariable Long bindingId,
            @Valid @RequestBody JenkinsAdminDtos.BindingRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(jenkinsAdminService.updateBinding(
                projectId, bindingId, request, authService.currentUser(authorization)
        ));
    }

    @PostMapping("/automation-projects/{automationProjectId}/initialize")
    public ApiResponse<OnboardingDtos.StartResponse> initialize(
            @PathVariable Long projectId,
            @PathVariable Long automationProjectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(onboardingService.start(
                projectId, automationProjectId, authService.currentUser(authorization)
        ));
    }

    @GetMapping("/automation-projects/{automationProjectId}/onboarding-status")
    public ApiResponse<OnboardingDtos.StatusResponse> onboardingStatus(
            @PathVariable Long projectId,
            @PathVariable Long automationProjectId,
            @RequestParam String onboardingId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(onboardingService.status(
                projectId, onboardingId, authService.currentUser(authorization)
        ));
    }

    @PostMapping("/automation-projects/{automationProjectId}/onboarding/{onboardingId}/retry")
    public ApiResponse<OnboardingDtos.StatusResponse> retryOnboarding(
            @PathVariable Long projectId,
            @PathVariable String onboardingId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(onboardingService.retry(
                projectId, onboardingId, authService.currentUser(authorization)
        ));
    }

    @PostMapping("/automation-projects/{automationProjectId}/onboarding/{onboardingId}/cancel")
    public ApiResponse<OnboardingDtos.StatusResponse> cancelOnboarding(
            @PathVariable Long projectId,
            @PathVariable String onboardingId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(onboardingService.cancel(
                projectId, onboardingId, authService.currentUser(authorization)
        ));
    }

    @GetMapping("/automation-projects/{automationProjectId}/pipeline/contract")
    public ApiResponse<JenkinsProtocolDtos.ContractResponse> pipelineContract(
            @PathVariable Long projectId,
            @PathVariable Long automationProjectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(protocolService.contract(
                projectId, automationProjectId, authService.currentUser(authorization)
        ));
    }

    @PostMapping("/automation-projects/{automationProjectId}/agent-precheck")
    public ApiResponse<JenkinsProtocolDtos.PrecheckResponse> agentPrecheck(
            @PathVariable Long projectId,
            @PathVariable Long automationProjectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(protocolService.precheck(
                projectId, automationProjectId, authService.currentUser(authorization)
        ));
    }

    @PostMapping("/test-tasks/{taskId}/execute")
    public ApiResponse<AutomationDtos.TestExecutionResponse> execute(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody(required = false) AutomationDtos.ExecuteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.execute(
                projectId,
                taskId,
                request,
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

    @PostMapping("/test-executions/{executionId}/retry")
    public ApiResponse<AutomationDtos.TestExecutionResponse> retry(
            @PathVariable Long projectId,
            @PathVariable Long executionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.retry(
                projectId,
                executionId,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/test-executions/{executionId}/cancel")
    public ApiResponse<AutomationDtos.TestExecutionResponse> cancel(
            @PathVariable Long projectId,
            @PathVariable Long executionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.cancel(
                projectId, executionId, authService.currentUser(authorization)
        ));
    }

    @DeleteMapping("/test-executions/{executionId}")
    public ApiResponse<Void> deleteExecution(
            @PathVariable Long projectId,
            @PathVariable Long executionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        automationService.deleteExecution(projectId, executionId, authService.currentUser(authorization));
        return ApiResponse.success(null);
    }

    @GetMapping("/test-executions/{executionId}/console-log")
    public ApiResponse<String> consoleLog(
            @PathVariable Long projectId,
            @PathVariable Long executionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(automationService.fetchConsoleLog(
                projectId, executionId, authService.currentUser(authorization)
        ));
    }
}
