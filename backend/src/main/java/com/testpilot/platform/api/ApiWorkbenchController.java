package com.testpilot.platform.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'api:execute')")
public class ApiWorkbenchController {

    private final ApiWorkbenchService service;

    public ApiWorkbenchController(ApiWorkbenchService service) {
        this.service = service;
    }

    @GetMapping("/collections")
    public ApiResponse<List<ApiDtos.CollectionResponse>> collections(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.listCollections(projectId, user));
    }

    @PostMapping("/collections")
    public ApiResponse<ApiDtos.CollectionResponse> createCollection(
            @PathVariable Long projectId,
            @Valid @RequestBody ApiDtos.CollectionRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.createCollection(projectId, request, user));
    }

    @DeleteMapping("/collections/{collectionId}")
    public ApiResponse<Void> deleteCollection(
            @PathVariable Long projectId,
            @PathVariable Long collectionId,
            @AuthenticationPrincipal User user
    ) {
        service.deleteCollection(projectId, collectionId, user);
        return ApiResponse.success(null);
    }

    @GetMapping("/collections/{collectionId}/requests")
    public ApiResponse<List<ApiDtos.RequestResponse>> requests(
            @PathVariable Long projectId,
            @PathVariable Long collectionId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.listRequests(projectId, collectionId, user));
    }

    @PostMapping("/collections/{collectionId}/execute")
    public ApiResponse<ApiDtos.CollectionExecutionResponse> executeCollection(
            @PathVariable Long projectId,
            @PathVariable Long collectionId,
            @Valid @RequestBody ApiDtos.CollectionExecuteRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.executeCollection(
                projectId,
                collectionId,
                request,
                user
        ));
    }

    @PostMapping("/requests")
    public ApiResponse<ApiDtos.RequestResponse> createRequest(
            @PathVariable Long projectId,
            @Valid @RequestBody ApiDtos.RequestUpsert request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.saveRequest(projectId, request, user));
    }

    @PutMapping("/requests/{requestId}")
    public ApiResponse<ApiDtos.RequestResponse> updateRequest(
            @PathVariable Long projectId,
            @PathVariable Long requestId,
            @Valid @RequestBody ApiDtos.RequestUpsert request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.updateRequest(projectId, requestId, request, user));
    }

    @DeleteMapping("/requests/{requestId}")
    public ApiResponse<Void> deleteRequest(
            @PathVariable Long projectId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal User user
    ) {
        service.deleteRequest(projectId, requestId, user);
        return ApiResponse.success(null);
    }

    @PostMapping("/requests/{requestId}/execute")
    public ApiResponse<ApiDtos.HistoryResponse> execute(
            @PathVariable Long projectId,
            @PathVariable Long requestId,
            @Valid @RequestBody ApiDtos.ExecuteRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.execute(projectId, requestId, request, user));
    }

    @GetMapping("/variables")
    public ApiResponse<List<ApiDtos.VariableResponse>> variables(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "TEST") String environment,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.listVariables(projectId, environment, user));
    }

    @PostMapping("/variables")
    public ApiResponse<ApiDtos.VariableResponse> saveVariable(
            @PathVariable Long projectId,
            @Valid @RequestBody ApiDtos.VariableUpsert request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(service.saveVariable(projectId, request, user));
    }

    @DeleteMapping("/variables/{variableId}")
    public ApiResponse<Void> deleteVariable(
            @PathVariable Long projectId,
            @PathVariable Long variableId,
            @AuthenticationPrincipal User user
    ) {
        service.deleteVariable(projectId, variableId, user);
        return ApiResponse.success(null);
    }

    @GetMapping("/history")
    public ApiResponse<ApiDtos.HistoryPageResponse> history(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(ApiDtos.HistoryPageResponse.from(service.listHistory(
                projectId, page, size, user
        )));
    }
}
