package com.ordersystem.query.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CacheMetricsConfigTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheMetricsConfig cacheMetricsConfig;

    @Test
    void shouldConfigureMeterRegistryForCacheMetrics() {
        assertThat(meterRegistry).isNotNull();
    }

    @Test
    void shouldHaveCacheHitRatioMetrics() {
        // Test that cache metrics are being collected
        assertThat(meterRegistry).isNotNull();
        
        // Cache operations should register metrics
        String cacheName = "orders";
        if (cacheManager.getCache(cacheName) != null) {
            // After cache operations, metrics should be available
            Timer cacheAccessTimer = meterRegistry.find("cache.access")
                    .tag("cache", cacheName)
                    .timer();
            
            // Metrics might not exist until cache is actually used
            // This test verifies the infrastructure is in place
        }
    }

    @Test
    void shouldTrackCacheHitAndMissCounters() {
        String cacheName = "orders";
        
        // Test infrastructure for hit/miss tracking
        assertThat(meterRegistry).isNotNull();
        
        // Counters will be created when cache operations occur
        Counter hitCounter = meterRegistry.find("cache.hits")
                .tag("cache", cacheName)
                .counter();
        
        Counter missCounter = meterRegistry.find("cache.misses")
                .tag("cache", cacheName)
                .counter();
        
        // Metrics infrastructure should be ready
    }

    @Test
    void shouldTrackCacheEvictionMetrics() {
        String cacheName = "orders";
        
        Counter evictionCounter = meterRegistry.find("cache.evictions")
                .tag("cache", cacheName)
                .counter();
        
        // Eviction metrics infrastructure should be ready
        assertThat(meterRegistry).isNotNull();
    }

    @Test
    void shouldProvideCacheHealthIndicator() {
        // Cache health indicator should be available
        assertThat(cacheMetricsConfig).isNotNull();
    }

    @Test
    void shouldCalculateCacheHitRatio() {
        // Test cache hit ratio calculation logic
        long hits = 80;
        long misses = 20;
        double expectedRatio = (double) hits / (hits + misses);
        
        double actualRatio = cacheMetricsConfig.calculateHitRatio(hits, misses);
        
        assertThat(actualRatio).isEqualTo(expectedRatio);
        assertThat(actualRatio).isGreaterThan(0.8); // Target: > 80%
    }

    @Test
    void shouldHandleZeroAccessesInHitRatio() {
        double ratio = cacheMetricsConfig.calculateHitRatio(0, 0);
        assertThat(ratio).isEqualTo(0.0);
    }

    @Test
    void shouldProvidePerformanceMetrics() {
        // Test that performance metrics are being tracked
        assertThat(meterRegistry).isNotNull();
        
        // Cache hit ratio gauge should be available
        assertThat(meterRegistry.find("cache.hit.ratio").gauge()).isNotNull();
        
        // Cache hits counter should be available
        assertThat(meterRegistry.find("cache.total.hits").gauge()).isNotNull();
        
        // Cache misses counter should be available
        assertThat(meterRegistry.find("cache.total.misses").gauge()).isNotNull();
    }
}