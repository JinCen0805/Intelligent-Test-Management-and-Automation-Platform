package com.testpilot.platform.rbac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.config.RedisConfig.RedisKeyPrefix;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RbacPermissionCacheTest {

    @Test
    void fallsBackToLocalPermissionsWhenRedisWriteAndReadFail() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down")).when(valueOperations).set(anyString(), any(), any(Duration.class));
        when(valueOperations.get("testpilot:test:rbac:user:1")).thenReturn(null);

        RbacPermissionCache cache = new RbacPermissionCache(
                redisTemplate,
                new RedisKeyPrefix("testpilot:test:"),
                new ObjectMapper(),
                true,
                false,
                60
        );

        cache.putSystem(1L, 3L, Set.of("project:read", "testcase:read"));

        assertTrue(cache.getSystem(1L, 3L).isPresent());
        assertEquals(Set.of("project:read", "testcase:read"), cache.getSystem(1L, 3L).orElseThrow());
    }

    @Test
    void cachedPermissionsExpireLocally() throws InterruptedException {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        RbacPermissionCache cache = new RbacPermissionCache(
                redisTemplate,
                new RedisKeyPrefix("testpilot:test:"),
                new ObjectMapper(),
                true,
                false,
                1
        );

        cache.putSystem(1L, 1L, Set.of("project:read"));
        Thread.sleep(1100);

        assertTrue(cache.getSystem(1L, 1L).isEmpty());
    }
}
