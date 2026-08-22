package com.testpilot.platform.rbac;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.config.RedisConfig.RedisKeyPrefix;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RbacPermissionCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyPrefix keyPrefix;
    private final ObjectMapper objectMapper;
    private final boolean redisEnabled;
    private final boolean redisRequired;
    private final Duration ttl;
    private final Map<String, CachedPermissions> localCache = new ConcurrentHashMap<>();

    public RbacPermissionCache(
            RedisTemplate<String, Object> redisTemplate,
            RedisKeyPrefix keyPrefix,
            ObjectMapper objectMapper,
            @Value("${testpilot.infrastructure.redis.enabled:true}") boolean redisEnabled,
            @Value("${testpilot.infrastructure.redis.required:false}") boolean redisRequired,
            @Value("${testpilot.security.permission-cache-ttl-seconds:300}") long ttlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.objectMapper = objectMapper;
        this.redisEnabled = redisEnabled;
        this.redisRequired = redisRequired;
        this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
    }

    public Optional<Set<String>> getSystem(Long userId, Long authVersion) {
        return get(keyPrefix.apply("rbac:user:" + userId), authVersion);
    }

    public Optional<Set<String>> getProject(Long userId, Long projectId, Long authVersion) {
        return get(keyPrefix.apply("rbac:project:" + projectId + ":user:" + userId), authVersion);
    }

    public void putSystem(Long userId, Long authVersion, Set<String> permissions) {
        put(keyPrefix.apply("rbac:user:" + userId), authVersion, permissions);
    }

    public void putProject(Long userId, Long projectId, Long authVersion, Set<String> permissions) {
        put(keyPrefix.apply("rbac:project:" + projectId + ":user:" + userId), authVersion, permissions);
    }

    public void invalidateUser(Long userId) {
        delete(keyPrefix.apply("rbac:user:" + userId));
        localCache.keySet().removeIf(key -> key.endsWith(":user:" + userId));
        if (!redisEnabled) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(keyPrefix.apply("rbac:project:*:user:" + userId));
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception exception) {
            if (redisRequired) {
                throw new BusinessException(50302, "权限缓存不可用");
            }
        }
    }

    public void invalidateProject(Long userId, Long projectId) {
        delete(keyPrefix.apply("rbac:project:" + projectId + ":user:" + userId));
    }

    private Optional<Set<String>> get(String key, Long authVersion) {
        CachedPermissions cached = localCache.get(key);
        if (!redisEnabled && cached != null) {
            return cached.isValid(authVersion)
                    ? Optional.of(cached.permissions())
                    : Optional.empty();
        }
        if (!redisEnabled) {
            return Optional.empty();
        }
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (!(value instanceof CachedPermissions decoded)) {
                if (value == null) {
                    return localPermissions(key, authVersion);
                }
                CachedPermissions converted = objectMapper.convertValue(value, CachedPermissions.class);
                return converted.isValid(authVersion)
                        ? Optional.of(converted.permissions())
                        : Optional.empty();
            }
            return decoded.isValid(authVersion)
                    ? Optional.of(decoded.permissions())
                    : Optional.empty();
        } catch (Exception exception) {
            if (redisRequired) {
                throw new BusinessException(50302, "权限缓存不可用");
            }
            return localPermissions(key, authVersion);
        }
    }

    private void put(String key, Long authVersion, Set<String> permissions) {
        CachedPermissions cached = new CachedPermissions(
                authVersion,
                new LinkedHashSet<>(permissions),
                LocalDateTime.now().plus(ttl)
        );
        localCache.put(key, cached);
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, cached, ttl);
        } catch (Exception exception) {
            if (redisRequired) {
                localCache.remove(key);
                throw new BusinessException(50302, "权限缓存不可用");
            }
        }
    }

    private void delete(String key) {
        localCache.remove(key);
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            if (redisRequired) {
                throw new BusinessException(50302, "权限缓存不可用");
            }
        }
    }

    public String systemKey(Long userId) {
        return keyPrefix.apply("rbac:user:" + userId);
    }

    public String projectKey(Long userId, Long projectId) {
        return keyPrefix.apply("rbac:project:" + projectId + ":user:" + userId);
    }

    private Optional<Set<String>> localPermissions(String key, Long authVersion) {
        CachedPermissions cached = localCache.get(key);
        if (cached == null || !cached.isValid(authVersion)) {
            return Optional.empty();
        }
        return Optional.of(cached.permissions());
    }

    public record CachedPermissions(Long authVersion, Set<String> permissions, LocalDateTime expiresAt) {
        boolean isValid(Long expectedAuthVersion) {
            return authVersion != null
                    && authVersion.equals(expectedAuthVersion)
                    && expiresAt != null
                    && expiresAt.isAfter(LocalDateTime.now());
        }
    }
}
