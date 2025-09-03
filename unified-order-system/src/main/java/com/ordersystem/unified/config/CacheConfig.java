package com.ordersystem.unified.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Cache configuration for performance optimization
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Profile("!redis")
    public CacheManager cacheManager() {
        // Use in-memory cache for development/testing
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(
            "orders",
            "customers", 
            "products",
            "inventory",
            "payments",
            "health-checks",
            "statistics"
        );
        return cacheManager;
    }
}