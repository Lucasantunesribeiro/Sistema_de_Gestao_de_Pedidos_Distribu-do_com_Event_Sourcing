package com.ordersystem.query.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
@EnableScheduling
public class CacheMetricsConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheMetricsConfig.class);

    @Bean
    public CacheMetricsCollector cacheMetricsCollector(CacheManager cacheManager, 
                                                      RedisConnectionFactory redisConnectionFactory) {
        return new CacheMetricsCollector(cacheManager, redisConnectionFactory);
    }

    /**
     * Calculate cache hit ratio
     */
    public double calculateHitRatio(long hits, long misses) {
        long total = hits + misses;
        if (total == 0) {
            return 0.0;
        }
        return (double) hits / total;
    }

    @Component
    public static class CacheMetricsCollector {

        private final CacheManager cacheManager;
        private final RedisConnectionFactory redisConnectionFactory;
        private MeterRegistry meterRegistry;
        
        // Metrics tracking
        private final AtomicLong totalHits = new AtomicLong(0);
        private final AtomicLong totalMisses = new AtomicLong(0);
        private final AtomicLong totalEvictions = new AtomicLong(0);

        public CacheMetricsCollector(CacheManager cacheManager, 
                                    RedisConnectionFactory redisConnectionFactory) {
            this.cacheManager = cacheManager;
            this.redisConnectionFactory = redisConnectionFactory;
        }

        @Autowired
        public void setMeterRegistry(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            logger.info("Cache metrics registry configured");
        }

        public double getHitRatio() {
            long hits = totalHits.get();
            long misses = totalMisses.get();
            long total = hits + misses;
            
            if (total == 0) {
                return 0.0;
            }
            
            return (double) hits / total;
        }

        /**
         * Record cache hit
         */
        public void recordHit() {
            totalHits.incrementAndGet();
        }

        /**
         * Record cache miss
         */
        public void recordMiss() {
            totalMisses.incrementAndGet();
        }

        /**
         * Record cache eviction
         */
        public void recordEviction() {
            totalEvictions.incrementAndGet();
        }

        /**
         * Periodic logging of cache statistics
         */
        @Scheduled(fixedRate = 60000) // Every minute
        public void logCacheStatistics() {
            long hits = totalHits.get();
            long misses = totalMisses.get();
            double hitRatio = getHitRatio();
            
            if (hits + misses > 0) {
                logger.info("Cache Statistics - Hit Ratio: {:.2f}%, Hits: {}, Misses: {}, Evictions: {}", 
                           hitRatio * 100, hits, misses, totalEvictions.get());
                
                // Alert if hit ratio is below target
                if (hitRatio < 0.80 && (hits + misses) > 100) {
                    logger.warn("Cache hit ratio ({:.2f}%) is below target (80%)", hitRatio * 100);
                }
            }
        }

        /**
         * Reset metrics (for testing)
         */
        public void resetMetrics() {
            totalHits.set(0);
            totalMisses.set(0);
            totalEvictions.set(0);
        }
    }
}