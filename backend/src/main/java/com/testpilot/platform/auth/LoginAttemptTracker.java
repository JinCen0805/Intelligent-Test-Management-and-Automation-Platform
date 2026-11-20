package com.testpilot.platform.auth;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.config.RedisConfig.RedisKeyPrefix;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * 登录失败限流：按 "用户名 + 客户端 IP" 统计失败次数，超过阈值后锁定一段时间。
 *
 * <p>Redis 可用时计数放 Redis（多实例共享）；Redis 未启用或调用异常时退化为本地计数。
 */
@Component
public class LoginAttemptTracker {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptTracker.class);

    private final RedisConnectionFactory connectionFactory;
    private final RedisKeyPrefix keyPrefix;
    private final boolean redisEnabled;
    private final boolean redisRequired;
    private final int maxAttempts;
    private final long windowSeconds;
    private final long lockSeconds;
    private final Map<String, LocalState> localStates = new ConcurrentHashMap<>();

    public LoginAttemptTracker(
            RedisConnectionFactory connectionFactory,
            RedisKeyPrefix keyPrefix,
            @Value("${testpilot.infrastructure.redis.enabled:true}") boolean redisEnabled,
            @Value("${testpilot.infrastructure.redis.required:false}") boolean redisRequired,
            @Value("${testpilot.security.login-max-attempts:5}") int maxAttempts,
            @Value("${testpilot.security.login-fail-window-minutes:5}") long windowMinutes,
            @Value("${testpilot.security.login-lock-minutes:15}") long lockMinutes
    ) {
        this.connectionFactory = connectionFactory;
        this.keyPrefix = keyPrefix;
        this.redisEnabled = redisEnabled;
        this.redisRequired = redisRequired;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.windowSeconds = Math.max(1L, windowMinutes) * 60L;
        this.lockSeconds = Math.max(1L, lockMinutes) * 60L;
    }

    public boolean isLocked(String username, String clientIp) {
        String lockKey = lockKey(username, clientIp);
        if (redisEnabled) {
            try (RedisConnection connection = connectionFactory.getConnection()) {
                return connection.stringCommands().get(lockKey.getBytes(StandardCharsets.UTF_8)) != null;
            } catch (Exception exception) {
                handleRedisFailure(exception);
            }
        }
        LocalState state = localStates.get(lockKey);
        return state != null && state.lockedUntil > Instant.now().toEpochMilli();
    }

    public void recordFailure(String username, String clientIp) {
        String failKey = failKey(username, clientIp);
        String lockKey = lockKey(username, clientIp);
        if (redisEnabled) {
            try (RedisConnection connection = connectionFactory.getConnection()) {
                Long count = connection.stringCommands().incr(failKey.getBytes(StandardCharsets.UTF_8));
                if (count != null && count == 1L) {
                    connection.keyCommands().expire(failKey.getBytes(StandardCharsets.UTF_8), windowSeconds);
                }
                if (count != null && count >= maxAttempts) {
                    connection.stringCommands().set(
                            lockKey.getBytes(StandardCharsets.UTF_8),
                            "1".getBytes(StandardCharsets.UTF_8)
                    );
                    connection.keyCommands().expire(lockKey.getBytes(StandardCharsets.UTF_8), lockSeconds);
                    connection.keyCommands().del(failKey.getBytes(StandardCharsets.UTF_8));
                }
                return;
            } catch (Exception exception) {
                handleRedisFailure(exception);
            }
        }
        long now = Instant.now().toEpochMilli();
        LocalState current = localStates.get(failKey);
        boolean expired = current == null || current.expiresAt < now;
        int failures = expired ? 0 : current.failures;
        failures++;
        if (failures >= maxAttempts) {
            localStates.remove(failKey);
            localStates.put(lockKey, new LocalState(0, 0L, now + lockSeconds * 1000L));
        } else {
            long expiresAt = expired ? now + windowSeconds * 1000L : current.expiresAt;
            localStates.put(failKey, new LocalState(failures, expiresAt, 0L));
        }
    }

    public void clear(String username, String clientIp) {
        String failKey = failKey(username, clientIp);
        String lockKey = lockKey(username, clientIp);
        localStates.remove(failKey);
        localStates.remove(lockKey);
        if (!redisEnabled) {
            return;
        }
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.keyCommands().del(
                    failKey.getBytes(StandardCharsets.UTF_8),
                    lockKey.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception exception) {
            handleRedisFailure(exception);
        }
    }

    private String failKey(String username, String clientIp) {
        return keyPrefix.apply("auth:login-fail:" + normalize(username) + ":" + normalize(clientIp));
    }

    private String lockKey(String username, String clientIp) {
        return keyPrefix.apply("auth:login-lock:" + normalize(username) + ":" + normalize(clientIp));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private void handleRedisFailure(Exception exception) {
        if (redisRequired) {
            throw new BusinessException(50301, "登录限流存储不可用");
        }
        log.warn("登录限流 Redis 调用失败，退化为本地计数: {}", exception.getMessage());
    }

    private record LocalState(int failures, long expiresAt, long lockedUntil) {
    }
}
