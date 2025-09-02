package com.ordersystem.unified.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Custom health indicators for the unified order system.
 * Provides detailed health information for monitoring and debugging.
 */
@Component
public class HealthConfig implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(HealthConfig.class);

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        // Check database connectivity
        checkDatabase(builder);
        
        // Check Redis connectivity (if enabled)
        checkRedis(builder);
        
        // Add application-specific health information
        builder.withDetail("service", "unified-order-system");
        builder.withDetail("version", "1.0.0");
        
        return builder.build();
    }

    private void checkDatabase(Health.Builder builder) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                builder.withDetail("database", "UP");
                builder.withDetail("database.url", connection.getMetaData().getURL());
            } else {
                builder.down().withDetail("database", "Connection validation failed");
            }
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            builder.down().withDetail("database", "DOWN: " + e.getMessage());
        }
    }

    private void checkRedis(Health.Builder builder) {
        if (redisTemplate != null) {
            try {
                // Try to ping Redis
                String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
                
                if ("PONG".equals(result)) {
                    builder.withDetail("redis", "UP");
                } else {
                    builder.withDetail("redis", "Unexpected ping response: " + result);
                }
            } catch (Exception e) {
                logger.warn("Redis health check failed: {}", e.getMessage());
                builder.withDetail("redis", "DOWN: " + e.getMessage());
            }
        } else {
            builder.withDetail("redis", "DISABLED");
        }
    }
}