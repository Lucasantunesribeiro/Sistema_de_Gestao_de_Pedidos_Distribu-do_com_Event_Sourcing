package com.ordersystem.query.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Autowired(required = false)
    private ObjectMapper optimizedObjectMapper;

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
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

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        logger.info("🔧 Configuring optimized Redis cache manager");
        
        // Use optimized Jackson serializer if available, fallback to default
        GenericJackson2JsonRedisSerializer jsonSerializer = optimizedObjectMapper != null ?
                new GenericJackson2JsonRedisSerializer(optimizedObjectMapper) :
                new GenericJackson2JsonRedisSerializer();
        
        // Default cache configuration with performance optimizations
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues() // Performance: don't cache nulls
                .computePrefixWith(cacheName -> "order-query:" + cacheName + ":"); // Organized keys

        // Specific cache configurations with different TTLs optimized for use patterns
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Very short-lived cache for high-frequency, changing data
        cacheConfigurations.put("orders", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("orders-paged", defaultConfig.entryTtl(Duration.ofMinutes(3)));
        
        // Short-lived cache for user-specific data
        cacheConfigurations.put("customer-orders", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("status-orders", defaultConfig.entryTtl(Duration.ofMinutes(7)));
        
        // Medium-lived cache for individual entities
        cacheConfigurations.put("single-order", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("order-count", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Longer-lived cache for aggregated data
        cacheConfigurations.put("dashboard-metrics", defaultConfig.entryTtl(Duration.ofMinutes(3)));
        cacheConfigurations.put("order-stats", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // System health cache - short TTL to ensure fresh health data
        cacheConfigurations.put("health", defaultConfig.entryTtl(Duration.ofSeconds(30)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // Support Spring transactions
                .build();

        logger.info("✅ Redis cache manager configured with {} cache types", cacheConfigurations.size());
        return cacheManager;
    }
}