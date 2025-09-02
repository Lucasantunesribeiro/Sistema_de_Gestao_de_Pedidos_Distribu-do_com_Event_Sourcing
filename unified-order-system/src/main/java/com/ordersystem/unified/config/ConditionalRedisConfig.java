package com.ordersystem.unified.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * Conditional Redis cache configuration.
 * Only creates Redis cache manager when Redis is enabled and available.
 */
@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class ConditionalRedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalRedisConfig.class);

    @Value("${spring.cache.redis.time-to-live:600000}")
    private long cacheTimeToLive;

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        logger.info("Configuring Redis cache manager with TTL: {} ms", cacheTimeToLive);
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(
                org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMillis(cacheTimeToLive))
                    .disableCachingNullValues()
            )
            .build();
    }
}