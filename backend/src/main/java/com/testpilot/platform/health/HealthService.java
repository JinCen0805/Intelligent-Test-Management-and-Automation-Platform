package com.testpilot.platform.health;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final boolean redisEnabled;
    private final boolean redisRequired;

    public HealthService(
            DataSource dataSource,
            RedisConnectionFactory redisConnectionFactory,
            @Value("${testpilot.infrastructure.redis.enabled:true}") boolean redisEnabled,
            @Value("${testpilot.infrastructure.redis.required:false}") boolean redisRequired
    ) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisEnabled = redisEnabled;
        this.redisRequired = redisRequired;
    }

    public Map<String, Object> snapshot() {
        Instant startedAt = Instant.now();
        Map<String, Object> checks = new LinkedHashMap<>();
        Map<String, Object> database = checkDatabase();
        Map<String, Object> redis = checkRedis();
        checks.put("database", database);
        checks.put("redis", redis);

        boolean databaseUp = "UP".equals(database.get("status"));
        boolean redisAcceptable = !redisRequired || "UP".equals(redis.get("status"));
        String status = databaseUp && redisAcceptable ? "UP" : "DOWN";
        return Map.of(
                "service", "itmap-platform",
                "status", status,
                "timestamp", Instant.now().toString(),
                "durationMs", Duration.between(startedAt, Instant.now()).toMillis(),
                "checks", checks
        );
    }

    private Map<String, Object> checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return Map.of("status", "UP", "product", connection.getMetaData().getDatabaseProductName());
        } catch (SQLException exception) {
            return Map.of("status", "DOWN", "message", "数据库连接不可用");
        }
    }

    private Map<String, Object> checkRedis() {
        if (!redisEnabled) {
            return Map.of("status", "DISABLED", "required", redisRequired);
        }
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return Map.of("status", "UP", "ping", pong == null ? "UNKNOWN" : pong, "required", redisRequired);
        } catch (Exception exception) {
            return Map.of("status", "DOWN", "required", redisRequired, "message", "Redis 连接不可用");
        }
    }
}
