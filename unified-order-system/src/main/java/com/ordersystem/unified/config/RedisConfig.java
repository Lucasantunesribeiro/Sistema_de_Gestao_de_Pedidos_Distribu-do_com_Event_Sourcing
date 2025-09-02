package com.ordersystem.unified.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration for the unified order system.
 * Provides caching and optional Redis connectivity with fallback handling.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${app.redis.enabled:true}")
    private boolean redisEnabled;

    @Value("${app.redis.cache-ttl:3600}")
    private long cacheTtl;

    /**
     * Redis connection factory - only created when Redis is enabled.
     */
    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisConnectionFactory redisConnectionFactory() {
        logger.info("Configuring Redis connection factory");
        return new LettuceConnectionFactory();
    }

    /**
     * Redis template for data operations.
     */
    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        logger.info("Configuring Redis template");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Cache manager configuration.
     * Falls back to simple cache manager if Redis is not available.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        if (redisEnabled) {
            logger.info("Configuring Redis cache manager with TTL: {} seconds", cacheTtl);
            
            RedisCacheManager.Builder builder = RedisCacheManager.RedisCacheManagerBuilder
                    .fromConnectionFactory(redisConnectionFactory)
                    .cacheDefaults(
                        org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                            .entryTtl(Duration.ofSeconds(cacheTtl))
                    );
            
            return builder.build();
        } else {
            logger.warn("Redis is disabled, using simple cache manager");
            return new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
        }
    }

    /**
     * Fallback cache manager when Redis is disabled.
     */
    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
    public CacheManager simpleCacheManager() {
        logger.info("Configuring simple in-memory cache manager");
        return new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
    }
}