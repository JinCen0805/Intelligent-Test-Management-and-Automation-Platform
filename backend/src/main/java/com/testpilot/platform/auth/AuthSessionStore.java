package com.testpilot.platform.auth;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.config.RedisConfig.RedisKeyPrefix;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionStore {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyPrefix keyPrefix;
    private final ObjectMapper objectMapper;
    private final boolean redisEnabled;
    private final boolean redisRequired;
    private final Map<String, AuthSession> localSessions = new ConcurrentHashMap<>();

    public AuthSessionStore(
            RedisTemplate<String, Object> redisTemplate,
            RedisKeyPrefix keyPrefix,
            ObjectMapper objectMapper,
            @Value("${testpilot.infrastructure.redis.enabled:true}") boolean redisEnabled,
            @Value("${testpilot.infrastructure.redis.required:false}") boolean redisRequired
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.objectMapper = objectMapper;
        this.redisEnabled = redisEnabled;
        this.redisRequired = redisRequired;
    }

    public void save(String token, AuthSession session) {
        String key = key(token);
        if (!redisEnabled) {
            localSessions.put(key, session);
            sweepLocalSessions();
            return;
        }
        try {
            Duration ttl = Duration.between(LocalDateTime.now(), session.expiresAt());
            if (ttl.isNegative() || ttl.isZero()) {
                throw new BusinessException(40100, "登录已失效");
            }
            redisTemplate.opsForValue().set(key, session, ttl);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            if (redisRequired) {
                throw new BusinessException(50301, "认证 Session 存储不可用");
            }
            // 降级缓存：仅在 Redis 不可用时使用，并随过期清理。
            localSessions.put(key, session);
            sweepLocalSessions();
        }
    }

    public AuthSession get(String token) {
        String key = key(token);
        if (!redisEnabled) {
            return localSession(key);
        }
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                // Redis 中已不存在（过期或主动删除）：本地副本一律不再复活。
                localSessions.remove(key);
                return null;
            }
            if (value instanceof AuthSession session) {
                return session;
            }
            return objectMapper.convertValue(value, AuthSession.class);
        } catch (Exception exception) {
            if (redisRequired) {
                throw new BusinessException(50301, "认证 Session 存储不可用");
            }
            return localSession(key);
        }
    }

    private void sweepLocalSessions() {
        if (localSessions.size() <= 256) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        localSessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public void delete(String token) {
        String key = key(token);
        localSessions.remove(key);
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            if (redisRequired) {
                throw new BusinessException(50301, "认证 Session 存储不可用");
            }
        }
    }

    public String sessionKey(String token) {
        return key(token);
    }

    private String key(String token) {
        return keyPrefix.apply("auth:session:" + token);
    }

    private AuthSession localSession(String key) {
        AuthSession session = localSessions.get(key);
        if (session != null && session.expiresAt().isBefore(LocalDateTime.now())) {
            localSessions.remove(key);
            return null;
        }
        return session;
    }

    public record AuthSession(
            Long userId,
            Long authVersion,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
    }
}
