package com.testpilot.platform.audit;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operation-audits")
public class OperationAuditController {

    private final OperationAuditService service;
    private final AuthService authService;

    public OperationAuditController(OperationAuditService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<OperationAuditDtos.OperationAuditResponse>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(service.list(authService.currentUser(authorization)));
    }
}
