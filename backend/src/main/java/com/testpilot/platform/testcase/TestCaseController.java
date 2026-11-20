package com.testpilot.platform.testcase;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

import com.testpilot.platform.common.ApiResponse;
import com.testpilot.platform.common.PageResponse;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @GetMapping
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'testcase:read')")
    public ApiResponse<PageResponse<TestCaseDtos.Response>> list(
            @PathVariable Long projectId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "caseType", required = false) String caseType,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "suiteType", required = false) String suiteType,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "50") int size,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(PageResponse.from(testCaseService.list(
                projectId,
                status,
                keyword,
                priority,
                caseType,
                source,
                suiteType,
                page,
                size,
                user
        )));
    }

    @GetMapping("/export")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'testcase:read')")
    public ResponseEntity<byte[]> export(
            @PathVariable Long projectId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "caseType", required = false) String caseType,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "suiteType", required = false) String suiteType,
            @AuthenticationPrincipal User user
    ) {
        List<TestCaseDtos.Response> rows = testCaseService.export(
                projectId,
                status,
                keyword,
                priority,
                caseType,
                source,
                suiteType,
                user
        );
        byte[] content = testCaseService.exportToExcel(rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("test-cases-" + projectId + ".xlsx").build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @GetMapping("/{testCaseId}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'testcase:read')")
    public ApiResponse<TestCaseDtos.Response> get(
            @PathVariable Long projectId,
            @PathVariable Long testCaseId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(testCaseService.get(
                projectId,
                testCaseId,
                user
        ));
    }

    @PostMapping
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'testcase:write')")
    public ApiResponse<TestCaseDtos.Response> create(
            @PathVariable Long projectId,
            @Valid @RequestBody TestCaseDtos.UpsertRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(testCaseService.create(
                projectId,
                request,
                user,
                "MANUAL"
        ));
    }

    @PutMapping("/{testCaseId}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'testcase:write')")
    public ApiResponse<TestCaseDtos.Response> update(
            @PathVariable Long projectId,
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseDtos.UpsertRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(testCaseService.update(
                projectId,
                testCaseId,
                request,
                user
        ));
    }

    @PostMapping("/{testCaseId}/status")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'testcase:review')")
    public ApiResponse<TestCaseDtos.Response> changeStatus(
            @PathVariable Long projectId,
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseDtos.StatusRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(testCaseService.changeStatus(
                projectId,
                testCaseId,
                request.status(),
                user
        ));
    }

    @DeleteMapping("/{testCaseId}")
    @PreAuthorize("@rbacAuthorizationService.hasProjectPermission(authentication, #projectId, 'testcase:write')")
    public ApiResponse<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long testCaseId,
            @AuthenticationPrincipal User user
    ) {
        testCaseService.delete(projectId, testCaseId, user);
        return ApiResponse.success();
    }
}
