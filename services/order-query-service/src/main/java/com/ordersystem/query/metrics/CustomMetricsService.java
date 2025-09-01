package com.ordersystem.query.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ordersystem.query.service.OrderQueryService;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;

/**
 * Custom Metrics Service for Order Query System
 * Provides comprehensive monitoring and observability
 */
@Service
public class CustomMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomMetricsService.class);

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private OrderQueryService orderQueryService;

    // Performance Metrics
    private Timer queryExecutionTimer;
    private Timer cacheHitTimer;
    private Timer cacheMissTimer;

    // Business Metrics
    private Counter ordersQueriedCounter;
    private Counter cacheHitCounter;
    private Counter cacheMissCounter;
    private Counter errorCounter;
    private Counter rateLimitExceededCounter;

    // System Health Metrics
    private AtomicLong activeConnections = new AtomicLong(0);
    private AtomicLong memoryUsage = new AtomicLong(0);
    private AtomicLong lastQueryTime = new AtomicLong(0);

    @PostConstruct
    public void initializeMetrics() {
        logger.info("🔧 Initializing custom metrics for Order Query Service");

        // Performance Timers
        queryExecutionTimer = Timer.builder("order.query.execution.time")
                .description("Time taken to execute order queries")
                .tag("service", "order-query")
                .register(meterRegistry);

        cacheHitTimer = Timer.builder("cache.hit.time")
                .description("Time taken for cache hits")
                .tag("service", "order-query")
                .register(meterRegistry);

        cacheMissTimer = Timer.builder("cache.miss.time")
                .description("Time taken for cache misses")
                .tag("service", "order-query")
                .register(meterRegistry);

        // Business Counters
        ordersQueriedCounter = Counter.builder("orders.queried.total")
                .description("Total number of orders queried")
                .tag("service", "order-query")
                .register(meterRegistry);

        cacheHitCounter = Counter.builder("cache.hits.total")
                .description("Total cache hits")
                .tag("service", "order-query")
                .register(meterRegistry);

        cacheMissCounter = Counter.builder("cache.misses.total")
                .description("Total cache misses")
                .tag("service", "order-query")
                .register(meterRegistry);

        errorCounter = Counter.builder("errors.total")
                .description("Total errors")
                .tag("service", "order-query")
                .register(meterRegistry);

        rateLimitExceededCounter = Counter.builder("rate.limit.exceeded.total")
                .description("Total rate limit exceeded events")
                .tag("service", "order-query")
                .register(meterRegistry);

        // System Health Gauges
        Gauge.builder("system.active.connections", this, CustomMetricsService::getActiveConnections)
                .description("Number of active database connections")
                .tag("service", "order-query")
                .register(meterRegistry);

        Gauge.builder("system.memory.usage.percent", this, CustomMetricsService::getMemoryUsagePercent)
                .description("Memory usage percentage")
                .tag("service", "order-query")
                .register(meterRegistry);

        Gauge.builder("orders.total.count", this, CustomMetricsService::getTotalOrderCount)
                .description("Total number of orders in the system")
                .tag("service", "order-query")
                .register(meterRegistry);

        Gauge.builder("query.last.execution.time", this, CustomMetricsService::getLastQueryTime)
                .description("Last query execution time in milliseconds")
                .tag("service", "order-query")
                .register(meterRegistry);

        logger.info("✅ Custom metrics initialized successfully");
    }

    // Performance Tracking Methods

    public Timer.Sample startQueryTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordQueryExecution(Timer.Sample sample, String queryType, boolean success) {
        sample.stop(Timer.builder("order.query.execution.time")
                .tag("service", "order-query")
                .tag("query.type", queryType)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));

        Counter queryCounter = meterRegistry.counter("orders.queried.total",
                Tags.of("query.type", queryType, "success", String.valueOf(success)));
        queryCounter.increment();

        if (!success) {
            Counter errorByType = meterRegistry.counter("errors.total",
                    Tags.of("type", "query_execution", "service", "order-query"));
            errorByType.increment();
        }
    }

    public void recordCacheHit(String cacheType) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(cacheHitTimer);
        Counter cacheHitByType = meterRegistry.counter("cache.hits.total",
                Tags.of("cache.type", cacheType, "service", "order-query"));
        cacheHitByType.increment();
    }

    public void recordCacheMiss(String cacheType) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(cacheMissTimer);
        Counter cacheMissByType = meterRegistry.counter("cache.misses.total",
                Tags.of("cache.type", cacheType, "service", "order-query"));
        cacheMissByType.increment();
    }

    public void recordError(String errorType, String operation) {
        Counter errorByOperation = meterRegistry.counter("errors.total",
                Tags.of("error.type", errorType, "operation", operation, "service", "order-query"));
        errorByOperation.increment();
    }

    public void recordRateLimitExceeded(String clientType) {
        Counter rateLimitByClient = meterRegistry.counter("rate.limit.exceeded.total",
                Tags.of("client.type", clientType, "service", "order-query"));
        rateLimitByClient.increment();
    }

    public void updateLastQueryTime(long executionTime) {
        lastQueryTime.set(executionTime);
    }

    // System Health Methods

    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

    public void updateMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        memoryUsage.set((long) memoryUsagePercent);
    }

    // Gauge Value Providers

    private double getActiveConnections() {
        return activeConnections.get();
    }

    private double getMemoryUsagePercent() {
        updateMemoryUsage();
        return memoryUsage.get();
    }

    private double getTotalOrderCount() {
        try {
            return orderQueryService.getOrderCount();
        } catch (Exception e) {
            logger.warn("Failed to get order count for metrics: {}", e.getMessage());
            return -1;
        }
    }

    private double getLastQueryTime() {
        return lastQueryTime.get();
    }

    // Cache Metrics Methods

    public double getCacheHitRate() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        
        return total > 0 ? (hits / total) * 100 : 0;
    }

    public Map<String, Double> getPerformanceSummary() {
        return Map.of(
            "totalQueries", ordersQueriedCounter.count(),
            "cacheHitRate", getCacheHitRate(),
            "totalErrors", errorCounter.count(),
            "averageQueryTime", queryExecutionTimer.mean(TimeUnit.MILLISECONDS),
            "memoryUsagePercent", getMemoryUsagePercent(),
            "totalOrders", getTotalOrderCount()
        );
    }

    // Health Check Integration

    public boolean isSystemHealthy() {
        double memoryPercent = getMemoryUsagePercent();
        double errorRate = errorCounter.count() / Math.max(1, ordersQueriedCounter.count());
        
        // System is healthy if:
        // - Memory usage < 85%
        // - Error rate < 5%
        // - Can get order count (database connectivity)
        return memoryPercent < 85 && errorRate < 0.05 && getTotalOrderCount() >= 0;
    }

    public Map<String, Object> getHealthMetrics() {
        return Map.of(
            "memoryUsagePercent", getMemoryUsagePercent(),
            "activeConnections", getActiveConnections(),
            "cacheHitRate", getCacheHitRate(),
            "totalErrors", errorCounter.count(),
            "rateLimitExceeded", rateLimitExceededCounter.count(),
            "systemHealthy", isSystemHealthy(),
            "lastQueryTime", getLastQueryTime()
        );
    }
}