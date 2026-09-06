package com.testpilot.platform.automation;

import com.testpilot.platform.common.ApiResponse;
import com.testpilot.platform.common.BusinessException;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final boolean strictSignature;
    private final Map<String, Long> consumedNonces = new ConcurrentHashMap<>();

    public JenkinsCallbackController(
            AutomationService automationService,
            @Value("${testpilot.jenkins.callback-token:}") String callbackToken,
            @Value("${testpilot.jenkins.callback-strict-signature:true}") boolean strictSignature
    ) {
        this.automationService = automationService;
        this.callbackToken = callbackToken;
        this.strictSignature = strictSignature;
    }

    @PostMapping("/callback/{executionId}")
    public ApiResponse<AutomationDtos.TestExecutionResponse> callback(
            @PathVariable Long executionId,
            @RequestHeader(value = "X-Jenkins-Token", required = false) String token,
            @RequestHeader(value = "X-Jenkins-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Jenkins-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Jenkins-Signature", required = false) String signature,
            @RequestHeader(value = "X-Jenkins-Job", required = false) String job,
            @Valid @RequestBody AutomationDtos.ExecutionResultRequest request
    ) {
        if (!validCredentials(executionId, token, timestamp, nonce, signature)) {
            throw new BusinessException(40102, "Jenkins 回调签名无效");
        }
        if (strictSignature && (job == null || job.isBlank())) {
            throw new BusinessException(40312, "Jenkins 回调缺少 Job 标识");
        }
        return ApiResponse.success(automationService.updateResultFromCallback(executionId, request, job));
    }

    private boolean validCredentials(Long executionId, String token, String timestamp,
                                     String nonce, String signature) {
        if (callbackToken == null || callbackToken.isBlank()) {
            return false;
        }
        if (!strictSignature && token != null && MessageDigest.isEqual(
                callbackToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }
        if (timestamp == null || nonce == null || signature == null || token == null
                || !MessageDigest.isEqual(callbackToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        final long epoch;
        try {
            epoch = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            return false;
        }
        long normalizedEpoch = epoch > 100_000_000_000L ? epoch / 1000 : epoch;
        if (Math.abs(Instant.now().getEpochSecond() - normalizedEpoch) > Duration.ofMinutes(5).toSeconds()) {
            return false;
        }
        String payload = executionId + ":" + timestamp + ":" + nonce;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    callbackToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = hex(signature);
            if (!MessageDigest.isEqual(expected, supplied)) {
                return false;
            }
            long now = Instant.now().getEpochSecond();
            consumedNonces.entrySet().removeIf(entry -> entry.getValue() < now);
            return consumedNonces.putIfAbsent(nonce, normalizedEpoch + Duration.ofMinutes(5).toSeconds()) == null;
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] hex(String value) {
        if (value.length() % 2 != 0) {
            throw new IllegalArgumentException("invalid signature");
        }
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}
