package com.testpilot.platform.automation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.common.ApiResponse;
import com.testpilot.platform.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Jenkins 回调入口。
 *
 * <p>签名覆盖 executionId、timestamp、nonce、请求体 SHA-256 与 Job（新格式）；
 * 为兼容旧流水线，{@code callback-require-body-hash=false} 时同时接受旧格式
 * （executionId:timestamp:nonce）。Jenkins 侧升级后把开关切为 true 即可强制新格式。
 */
@RestController
@RequestMapping("/api/integrations/jenkins")
public class JenkinsCallbackController {

    private static final long CLOCK_SKEW_SECONDS = Duration.ofMinutes(5).toSeconds();
    private static final long NONCE_TTL_SECONDS = Duration.ofMinutes(5).toSeconds();

    private final AutomationService automationService;
    private final ObjectMapper objectMapper;
    private final JenkinsCallbackReplayGuard replayGuard;
    private final String callbackToken;
    private final boolean strictSignature;
    private final boolean requireBodyHash;

    public JenkinsCallbackController(
            AutomationService automationService,
            ObjectMapper objectMapper,
            JenkinsCallbackReplayGuard replayGuard,
            @Value("${testpilot.jenkins.callback-token:}") String callbackToken,
            @Value("${testpilot.jenkins.callback-strict-signature:true}") boolean strictSignature,
            @Value("${testpilot.jenkins.callback-require-body-hash:false}") boolean requireBodyHash
    ) {
        this.automationService = automationService;
        this.objectMapper = objectMapper;
        this.replayGuard = replayGuard;
        this.callbackToken = callbackToken;
        this.strictSignature = strictSignature;
        this.requireBodyHash = requireBodyHash;
    }

    @PostMapping("/callback/{executionId}")
    public ApiResponse<AutomationDtos.TestExecutionResponse> callback(
            @PathVariable Long executionId,
            @RequestHeader(value = "X-Jenkins-Token", required = false) String token,
            @RequestHeader(value = "X-Jenkins-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Jenkins-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Jenkins-Signature", required = false) String signature,
            @RequestHeader(value = "X-Jenkins-Job", required = false) String job,
            @RequestHeader(value = "X-Jenkins-Body-SHA256", required = false) String bodyHashHeader,
            @RequestBody(required = false) String rawBody
    ) {
        AutomationDtos.ExecutionResultRequest request = parseBody(rawBody);
        if (!validCredentials(executionId, token, timestamp, nonce, signature, job, bodyHashHeader, rawBody)) {
            throw new BusinessException(40102, "Jenkins 回调签名无效");
        }
        if (strictSignature && (job == null || job.isBlank())) {
            throw new BusinessException(40312, "Jenkins 回调缺少 Job 标识");
        }
        return ApiResponse.success(automationService.updateResultFromCallback(executionId, request, job));
    }

    private AutomationDtos.ExecutionResultRequest parseBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new BusinessException(40001, "Jenkins 回调请求体不能为空");
        }
        try {
            AutomationDtos.ExecutionResultRequest request =
                    objectMapper.readValue(rawBody, AutomationDtos.ExecutionResultRequest.class);
            if (request.status() == null || request.status().isBlank()) {
                throw new BusinessException(40001, "Jenkins 回调缺少执行状态");
            }
            return request;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(40001, "Jenkins 回调请求体不合法");
        }
    }

    private boolean validCredentials(
            Long executionId,
            String token,
            String timestamp,
            String nonce,
            String signature,
            String job,
            String bodyHashHeader,
            String rawBody
    ) {
        if (callbackToken == null || callbackToken.isBlank() || token == null
                || !MessageDigest.isEqual(
                callbackToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        String bodyHash = sha256Hex(rawBody == null ? "" : rawBody);
        if (bodyHashHeader != null && !bodyHashHeader.isBlank()
                && !MessageDigest.isEqual(
                bodyHash.getBytes(StandardCharsets.UTF_8),
                bodyHashHeader.trim().toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        if (requireBodyHash && (bodyHashHeader == null || bodyHashHeader.isBlank())) {
            return false;
        }
        if (!strictSignature) {
            // 旧行为：仅校验回调 token（无签名/防重放），用于兼容旧流水线。
            return true;
        }
        if (timestamp == null || nonce == null || signature == null) {
            return false;
        }
        final long epoch;
        try {
            epoch = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            return false;
        }
        long normalizedEpoch = epoch > 100_000_000_000L ? epoch / 1000 : epoch;
        if (Math.abs(Instant.now().getEpochSecond() - normalizedEpoch) > CLOCK_SKEW_SECONDS) {
            return false;
        }
        String jobPart = job == null ? "" : job;
        String legacyPayload = executionId + ":" + timestamp + ":" + nonce;
        String modernPayload = legacyPayload + ":" + bodyHash + ":" + jobPart;
        boolean verified = hmacMatches(modernPayload, signature);
        if (!verified && !requireBodyHash) {
            verified = hmacMatches(legacyPayload, signature);
        }
        if (!verified) {
            return false;
        }
        return replayGuard.consumeNonce(nonce, NONCE_TTL_SECONDS);
    }

    private boolean hmacMatches(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(callbackToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String supplied = signature.trim().toLowerCase(java.util.Locale.ROOT);
            if (supplied.length() != expected.length * 2) {
                return false;
            }
            byte[] suppliedBytes = HexFormat.of().parseHex(supplied);
            return MessageDigest.isEqual(expected, suppliedBytes);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法计算请求体摘要", exception);
        }
    }
}
