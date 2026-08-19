package com.testpilot.platform.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AddressSafetyTest {

    private final AddressSafety safety = new AddressSafety("");

    @Test
    void allowsPublicHttpsUrl() {
        assertEquals("https", safety.validate("https://8.8.8.8/health").getScheme());
    }

    @Test
    void allowsWhitelistedHost() {
        assertEquals("internal.test",
                safety.validate("http://internal.test/repo", Set.of("internal.test")).getHost());
    }

    @Test
    void rejectsNonHttpScheme() {
        assertThrows(BusinessException.class, () -> safety.validate("ftp://example.com/file"));
    }

    @Test
    void rejectsUrlWithEmbeddedUserinfo() {
        assertThrows(BusinessException.class, () -> safety.validate("https://user:pass@example.com"));
    }

    @Test
    void rejectsLoopbackLocalhostPrivateAndMetadata() {
        assertThrows(BusinessException.class, () -> safety.validate("http://127.0.0.1"));
        assertThrows(BusinessException.class, () -> safety.validate("http://localhost"));
        assertThrows(BusinessException.class, () -> safety.validate("http://10.0.0.1"));
        assertThrows(BusinessException.class, () -> safety.validate("http://192.168.1.1"));
        assertThrows(BusinessException.class, () -> safety.validate("http://169.254.10.10"));
        assertThrows(BusinessException.class, () -> safety.validate("http://169.254.169.254"));
        assertThrows(BusinessException.class, () -> safety.validate("http://metadata.google.internal"));
    }
}
