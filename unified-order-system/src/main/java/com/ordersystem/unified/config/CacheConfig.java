package com.ordersystem.unified.config;

import java.time.Duration;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.beans.factory.annotation.Value;

/**
 * Cache configuration for the active runtime.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final List<String> CACHE_NAMES = List.of(
        "orders",
        "orderStatistics",
        "dashboard"
    );

    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory,
                                          @Value("${app.redis.cache-ttl:1h}") Duration cacheTtl,
                                          ObjectMapper objectMapper) {
        // Copy Spring Boot's auto-configured ObjectMapper (which includes JavaTimeModule)
        // and add polymorphic type info so Redis can deserialize cached objects correctly.
        ObjectMapper cacheMapper = objectMapper.copy()
            .activateDefaultTypingAsProperty(
                BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(Object.class)
                    .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                "@class"
            );

        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(cacheTtl)
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(configuration)
            .initialCacheNames(java.util.Set.copyOf(CACHE_NAMES))
            .transactionAware()
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager inMemoryCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(CACHE_NAMES);
        return cacheManager;
    }
}
