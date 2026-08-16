package com.testpilot.platform.requirement;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.ApiResponse;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class RequirementController {

    private final RequirementService requirementService;
    private final AuthService authService;

    public RequirementController(RequirementService requirementService, AuthService authService) {
        this.requirementService = requirementService;
        this.authService = authService;
    }

    @GetMapping("/projects/{projectId}/requirements")
    public ApiResponse<List<RequirementDtos.Response>> list(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(requirementService.list(
                projectId,
                authService.currentUser(authorization)
        ));
    }

    @GetMapping("/projects/{projectId}/requirements/{requirementId}")
    public ApiResponse<RequirementDtos.DetailResponse> get(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(requirementService.get(
                projectId,
                requirementId,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/projects/{projectId}/requirements/upload")
    public ApiResponse<RequirementDtos.Response> upload(
            @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "version", required = false, defaultValue = "V1.0") String version,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(requirementService.upload(
                projectId,
                file,
                version,
                authService.currentUser(authorization)
        ));
    }

    @PostMapping("/projects/{projectId}/requirements/{requirementId}/parse")
    public ApiResponse<RequirementDtos.Response> parse(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(requirementService.parse(
                projectId,
                requirementId,
                authService.currentUser(authorization)
        ));
    }
}
