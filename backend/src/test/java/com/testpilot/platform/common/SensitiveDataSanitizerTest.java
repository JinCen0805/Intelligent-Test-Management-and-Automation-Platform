package com.testpilot.platform.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SensitiveDataSanitizerTest {

    private final SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer();

    @Test
    void redactsPasswordsTokensAndCookies() {
        String out = sanitizer.sanitize("password=supersecret token:abc123 cookie: sessionId=xyz");
        assertFalse(out.contains("supersecret"));
        assertFalse(out.contains("abc123"));
        assertFalse(out.contains("sessionId=xyz"));
        assertTrue(out.contains("***"));
    }

    @Test
    void redactsAuthorizationAndBearerHeaders() {
        String out = sanitizer.sanitize("Authorization: Bearer abc.def.ghi\nX-Api-Key: sk-123");
        assertFalse(out.contains("abc.def.ghi"));
        assertFalse(out.contains("sk-123"));
    }

    @Test
    void redactsUrlUserinfoAndQueryCredentials() {
        String out = sanitizer.sanitize("https://user:pass@example.com/repo?token=abc&signature=xyz");
        assertFalse(out.contains("user:pass"));
        assertFalse(out.contains("token=abc"));
        assertFalse(out.contains("signature=xyz"));
    }

    @Test
    void redactsJsonQuotedKeysAndValues() {
        String out = sanitizer.sanitize(
                "{\"password\":\"p@ssw0rd\",\"token\": \"abc123\",\"Authorization\": \"Bearer secret-token\"}");
        assertFalse(out.contains("p@ssw0rd"));
        assertFalse(out.contains("abc123"));
        assertFalse(out.contains("secret-token"));
        assertTrue(out.contains("***"));
    }

    @Test
    void preservesBenignContent() {
        String input = "hello world, test path tests/regression";
        assertEquals(input, sanitizer.sanitize(input));
    }
}
