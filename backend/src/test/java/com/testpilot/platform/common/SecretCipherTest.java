package com.testpilot.platform.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SecretCipherTest {

    private final SecretCipher cipher = new SecretCipher("test-encryption-key");

    @Test
    void roundTripsPlaintext() {
        String encrypted = cipher.encrypt("sk-secret-value");
        assertNotEquals("sk-secret-value", encrypted);
        assertEquals("sk-secret-value", cipher.decrypt(encrypted));
    }

    @Test
    void nullAndBlankAreNull() {
        assertNull(cipher.encrypt(null));
        assertNull(cipher.encrypt(""));
        assertNull(cipher.decrypt(null));
        assertNull(cipher.decrypt("  "));
    }

    @Test
    void tamperedCiphertextThrows() {
        String encrypted = cipher.encrypt("secret");
        String tampered = "A" + encrypted.substring(1);
        assertThrows(BusinessException.class, () -> cipher.decrypt(tampered));
    }
}
