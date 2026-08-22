package com.testpilot.platform.ai;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("@rbacAuthorizationService.hasSystemPermission(authentication, 'system:audit:read')")
public class AiCallAuditController {

    private final AiCallAuditService service;
    private final AuthService authService;

    public AiCallAuditController(AiCallAuditService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping("/audits")
    public ApiResponse<List<AiDtos.AiCallAuditResponse>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.list(authService.currentUser(authorization)));
    }
}
