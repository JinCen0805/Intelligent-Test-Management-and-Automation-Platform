package com.testpilot.platform.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.config.RedisConfig.RedisKeyPrefix;
import java.time.LocalDateTime;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthSessionStoreTest {

    @Test
    void fallsBackToLocalSessionWhenRedisWriteAndReadFail() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down")).when(valueOperations).set(anyString(), any(), any(Duration.class));
        when(valueOperations.get("testpilot:test:auth:session:token-1")).thenReturn(null);

        AuthSessionStore store = new AuthSessionStore(
                redisTemplate,
                new RedisKeyPrefix("testpilot:test:"),
                new ObjectMapper(),
                true,
                false
        );
        AuthSessionStore.AuthSession session = new AuthSessionStore.AuthSession(
                1L,
                2L,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(30)
        );

        store.save("token-1", session);

        AuthSessionStore.AuthSession restored = store.get("token-1");
        assertNotNull(restored);
        assertEquals(1L, restored.userId());
        assertEquals(2L, restored.authVersion());
    }

    @Test
    void expiredLocalSessionIsIgnored() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthSessionStore store = new AuthSessionStore(
                redisTemplate,
                new RedisKeyPrefix("testpilot:test:"),
                new ObjectMapper(),
                false,
                false
        );
        store.save("token-2", new AuthSessionStore.AuthSession(
                1L,
                1L,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusMinutes(1)
        ));

        assertNull(store.get("token-2"));
    }
}
