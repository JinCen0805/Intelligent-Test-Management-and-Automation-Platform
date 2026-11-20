package com.testpilot.platform.ai;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

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

    public AiCallAuditController(AiCallAuditService service) {
        this.service = service;
    }

    @GetMapping("/audits")
    public ApiResponse<?> list(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return page == null && size == null
                ? ApiResponse.success(service.list(user))
                : ApiResponse.success(service.page(user, page == null ? 0 : page, size == null ? 50 : size));
    }
}
