package com.testpilot.platform.automation;

import com.testpilot.platform.common.ApiResponse;
import com.testpilot.platform.common.BusinessException;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/jenkins")
public class JenkinsCallbackController {

    private final AutomationService automationService;
    private final String callbackToken;

    public JenkinsCallbackController(
            AutomationService automationService,
            @Value("${testpilot.jenkins.callback-token:}") String callbackToken
    ) {
        this.automationService = automationService;
        this.callbackToken = callbackToken;
    }

    @PostMapping("/callback/{executionId}")
    public ApiResponse<AutomationDtos.TestExecutionResponse> callback(
            @PathVariable Long executionId,
            @RequestHeader(value = "X-Jenkins-Token", required = false) String token,
            @Valid @RequestBody AutomationDtos.ExecutionResultRequest request
    ) {
        if (callbackToken == null || callbackToken.isBlank()
                || token == null
                || !MessageDigest.isEqual(
                        callbackToken.getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8)
                )) {
            throw new BusinessException(40102, "Jenkins 回调签名无效");
        }
        return ApiResponse.success(automationService.updateResultFromCallback(executionId, request));
    }
}
