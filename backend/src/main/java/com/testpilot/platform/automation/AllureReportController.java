package com.testpilot.platform.automation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.common.ApiResponse;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.user.User;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class AllureReportController {

    private final TestExecutionRepository executionRepository;
    private final ProjectAccessService projectAccessService;
    private final AllureReportStorage storage;
    private final AllureAccessTokenService tokenService;

    public AllureReportController(
            TestExecutionRepository executionRepository,
            ProjectAccessService projectAccessService,
            AllureReportStorage storage,
            AllureAccessTokenService tokenService
    ) {
        this.executionRepository = executionRepository;
        this.projectAccessService = projectAccessService;
        this.storage = storage;
        this.tokenService = tokenService;
    }

    @PostMapping("/executions/{executionId}/allure-access")
    public ApiResponse<Map<String, String>> access(
            @PathVariable Long executionId,
            @AuthenticationPrincipal User user
    ) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException(40412, "执行记录不存在"));
        projectAccessService.requirePermission(execution.getProjectId(), user, RbacPermissions.REPORT_READ);
        String token = tokenService.issue(executionId, 600);
        String base = "/api/reports/public/allure/" + token;
        return ApiResponse.success(Map.of(
                "viewUrl", storage.hasReport(executionId) ? base + "/" : "",
                "downloadUrl", storage.hasReport(executionId) ? base + "/download" : "",
                "expiresInSeconds", "600"
        ));
    }

    @GetMapping({"/public/allure/{token}", "/public/allure/{token}/",
            "/public/allure/{token}/{*path}"})
    public ResponseEntity<Resource> view(
            @PathVariable String token,
            @PathVariable(required = false) String path
    ) {
        Long executionId = tokenService.verify(token);
        Resource resource = storage.reportResource(executionId, path);
        String filename = resource.getFilename() == null ? "index.html" : resource.getFilename();
        MediaType type = MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                // 报告是构建产物（可被项目成员控制）：用 sandbox 隔离成不透明源，
                // 避免其中的脚本读取平台同源的 localStorage / Cookie。
                .header("Content-Security-Policy", "sandbox allow-scripts allow-popups allow-forms")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(type)
                .body(resource);
    }

    @GetMapping("/public/allure/{token}/download")
    public ResponseEntity<Resource> download(@PathVariable String token) {
        Long executionId = tokenService.verify(token);
        Resource resource = storage.archiveResource(executionId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("allure-report-" + executionId + ".zip").build().toString())
                .body(resource);
    }
}
