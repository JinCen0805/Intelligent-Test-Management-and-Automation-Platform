package com.testpilot.platform.ai;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResponse<?> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        var user = authService.currentUser(authorization);
        return page == null && size == null
                ? ApiResponse.success(service.list(user))
                : ApiResponse.success(service.page(user, page == null ? 0 : page, size == null ? 50 : size));
    }
}
