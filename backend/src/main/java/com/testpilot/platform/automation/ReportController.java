package com.testpilot.platform.automation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.testpilot.platform.user.User;

import com.testpilot.platform.common.ApiResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ApiResponse<ReportDtos.CenterResponse> center(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(reportService.center(
                projectId,
                status,
                from,
                to,
                page,
                size,
                user
        ));
    }

    @GetMapping("/executions/{executionId}")
    public ApiResponse<ReportDtos.ExecutionDetail> detail(
            @PathVariable Long executionId,
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.success(reportService.detail(
                executionId,
                user
        ));
    }
}
