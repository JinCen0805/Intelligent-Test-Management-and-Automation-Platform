package com.testpilot.platform.automation;

import com.testpilot.platform.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AllureAccessTokenService {

    private final byte[] secret;

    public AllureAccessTokenService(
            @Value("${testpilot.security.encryption-key:change-me-in-production}") String secret
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(Long executionId, long ttlSeconds) {
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = executionId + "." + expiresAt;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                payload.getBytes(StandardCharsets.UTF_8)
        ) + "." + sign(payload);
    }

    public Long verify(String token) {
        try {
            String[] parts = token.split("\\.", 2);
            String payload = new String(
                    Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8
            );
            if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8),
                    parts[1].getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException();
            }
            String[] values = payload.split("\\.");
            if (values.length != 2 || Instant.now().getEpochSecond() >= Long.parseLong(values[1])) {
                throw new IllegalArgumentException();
            }
            return Long.valueOf(values[0]);
        } catch (RuntimeException exception) {
            throw new BusinessException(40103, "Allure 报告访问链接已失效");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成 Allure 访问签名", exception);
        }
    }
}
