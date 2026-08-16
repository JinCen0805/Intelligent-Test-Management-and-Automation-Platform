package com.testpilot.platform.testcase;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final AuthService authService;

    public TestCaseController(TestCaseService testCaseService, AuthService authService) {
        this.testCaseService = testCaseService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<Page<TestCaseDtos.Response>> list(
            @PathVariable Long projectId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "caseType", required = false) String caseType,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "50") int size,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(testCaseService.list(
                projectId,
                status,
                keyword,
                priority,
                caseType,
                page,
                size,
                authService.currentUser(authorization)
        ));
    }

    @GetMapping("/{testCaseId}")
    public ApiResponse<TestCaseDtos.Response> get(
            @PathVariable Long projectId,
            @PathVariable Long testCaseId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(testCaseService.get(
                projectId,
                testCaseId,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping
    public ApiResponse<TestCaseDtos.Response> create(
            @PathVariable Long projectId,
            @Valid @RequestBody TestCaseDtos.UpsertRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(testCaseService.create(
                projectId,
                request,
                authService.currentUser(authorization),
                "MANUAL"
        ));
    }

    @PutMapping("/{testCaseId}")
    public ApiResponse<TestCaseDtos.Response> update(
            @PathVariable Long projectId,
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseDtos.UpsertRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(testCaseService.update(
                projectId,
                testCaseId,
                request,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/{testCaseId}/status")
    public ApiResponse<TestCaseDtos.Response> changeStatus(
            @PathVariable Long projectId,
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseDtos.StatusRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(testCaseService.changeStatus(
                projectId,
                testCaseId,
                request.status(),
                authService.currentUser(authorization)
        ));
    }

    @DeleteMapping("/{testCaseId}")
    public ApiResponse<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long testCaseId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        testCaseService.delete(projectId, testCaseId, authService.currentUser(authorization));
        return ApiResponse.success();
    }
}
