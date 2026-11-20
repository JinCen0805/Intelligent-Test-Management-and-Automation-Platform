package com.testpilot.platform.automation;

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
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

/**
 * Jenkins 回调 nonce 防重放。
 *
 * <p>Redis 可用时用 SET NX EX 保证多实例共享；Redis 未启用时退化为本地 Map。
 */
@Component
public class JenkinsCallbackReplayGuard {

    private static final Logger log = LoggerFactory.getLogger(JenkinsCallbackReplayGuard.class);

    private final RedisConnectionFactory connectionFactory;
    private final RedisKeyPrefix keyPrefix;
    private final boolean redisEnabled;
    private final boolean redisRequired;
    private final Map<String, Long> localNonces = new ConcurrentHashMap<>();

    public JenkinsCallbackReplayGuard(
            RedisConnectionFactory connectionFactory,
            RedisKeyPrefix keyPrefix,
            @Value("${testpilot.infrastructure.redis.enabled:true}") boolean redisEnabled,
            @Value("${testpilot.infrastructure.redis.required:false}") boolean redisRequired
    ) {
        this.connectionFactory = connectionFactory;
        this.keyPrefix = keyPrefix;
        this.redisEnabled = redisEnabled;
        this.redisRequired = redisRequired;
    }

    /**
     * @return true 表示 nonce 首次出现（允许处理），false 表示重放。
     */
    public boolean consumeNonce(String nonce, long ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        if (redisEnabled) {
            try (RedisConnection connection = connectionFactory.getConnection()) {
                byte[] key = keyPrefix.apply("jenkins:callback:nonce:" + nonce)
                        .getBytes(StandardCharsets.UTF_8);
                Boolean acquired = connection.stringCommands().set(
                        key,
                        "1".getBytes(StandardCharsets.UTF_8),
                        Expiration.seconds(ttlSeconds),
                        RedisStringCommands.SetOption.ifAbsent()
                );
                return Boolean.TRUE.equals(acquired);
            } catch (Exception exception) {
                if (redisRequired) {
                    throw new BusinessException(50301, "回调防重放存储不可用");
                }
                log.warn("回调 nonce Redis 调用失败，退化为本地去重: {}", exception.getMessage());
            }
        }
        localNonces.entrySet().removeIf(entry -> entry.getValue() < now);
        return localNonces.putIfAbsent(nonce, now + ttlSeconds) == null;
    }
}
