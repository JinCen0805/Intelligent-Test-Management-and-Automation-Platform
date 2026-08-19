package com.testpilot.platform.api;

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
@RequestMapping("/api/projects/{projectId}/api-workbench")
public class ApiWorkbenchController {

    private final ApiWorkbenchService service;
    private final AuthService authService;

    public ApiWorkbenchController(ApiWorkbenchService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping("/collections")
    public ApiResponse<List<ApiDtos.CollectionResponse>> collections(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.listCollections(projectId, authService.currentUser(authorization)));
    }

    @PostMapping("/collections")
    public ApiResponse<ApiDtos.CollectionResponse> createCollection(
            @PathVariable Long projectId,
            @Valid @RequestBody ApiDtos.CollectionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.createCollection(projectId, request, authService.currentUser(authorization)));
    }

    @DeleteMapping("/collections/{collectionId}")
    public ApiResponse<Void> deleteCollection(
            @PathVariable Long projectId,
            @PathVariable Long collectionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        service.deleteCollection(projectId, collectionId, authService.currentUser(authorization));
        return ApiResponse.success(null);
    }

    @GetMapping("/collections/{collectionId}/requests")
    public ApiResponse<List<ApiDtos.RequestResponse>> requests(
            @PathVariable Long projectId,
            @PathVariable Long collectionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.listRequests(projectId, collectionId, authService.currentUser(authorization)));
    }

    @PostMapping("/collections/{collectionId}/execute")
    public ApiResponse<ApiDtos.CollectionExecutionResponse> executeCollection(
            @PathVariable Long projectId,
            @PathVariable Long collectionId,
            @Valid @RequestBody ApiDtos.CollectionExecuteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.executeCollection(
                projectId,
                collectionId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/requests")
    public ApiResponse<ApiDtos.RequestResponse> createRequest(
            @PathVariable Long projectId,
            @Valid @RequestBody ApiDtos.RequestUpsert request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.saveRequest(projectId, request, authService.currentUser(authorization)));
    }

    @PutMapping("/requests/{requestId}")
    public ApiResponse<ApiDtos.RequestResponse> updateRequest(
            @PathVariable Long projectId,
            @PathVariable Long requestId,
            @Valid @RequestBody ApiDtos.RequestUpsert request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.updateRequest(projectId, requestId, request, authService.currentUser(authorization)));
    }

    @DeleteMapping("/requests/{requestId}")
    public ApiResponse<Void> deleteRequest(
            @PathVariable Long projectId,
            @PathVariable Long requestId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        service.deleteRequest(projectId, requestId, authService.currentUser(authorization));
        return ApiResponse.success(null);
    }

    @PostMapping("/requests/{requestId}/execute")
    public ApiResponse<ApiDtos.HistoryResponse> execute(
            @PathVariable Long projectId,
            @PathVariable Long requestId,
            @Valid @RequestBody ApiDtos.ExecuteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.execute(projectId, requestId, request, authService.currentUser(authorization)));
    }

    @GetMapping("/variables")
    public ApiResponse<List<ApiDtos.VariableResponse>> variables(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "TEST") String environment,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.listVariables(projectId, environment, authService.currentUser(authorization)));
    }

    @PostMapping("/variables")
    public ApiResponse<ApiDtos.VariableResponse> saveVariable(
            @PathVariable Long projectId,
            @Valid @RequestBody ApiDtos.VariableUpsert request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.saveVariable(projectId, request, authService.currentUser(authorization)));
    }

    @DeleteMapping("/variables/{variableId}")
    public ApiResponse<Void> deleteVariable(
            @PathVariable Long projectId,
            @PathVariable Long variableId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        service.deleteVariable(projectId, variableId, authService.currentUser(authorization));
        return ApiResponse.success(null);
    }

    @GetMapping("/history")
    public ApiResponse<ApiDtos.HistoryPageResponse> history(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(ApiDtos.HistoryPageResponse.from(service.listHistory(
                projectId, page, size, authService.currentUser(authorization)
        )));
    }
}
