package com.ordersystem.order.saga;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive metrics collection for saga operations
 * 
 * Tracks:
 * - Saga lifecycle events (created, completed, failed)
 * - Performance metrics (duration, throughput)
 * - Recovery operations
 * - Compensation flows
 * - Success/failure rates
 * 
 * Integrates with Micrometer for Prometheus export
 */
@Component
public class SagaMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaMetrics.class);
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private SagaStateManager sagaStateManager;
    
    @Autowired
    private SagaRecoveryService sagaRecoveryService;
    
    // Counters
    private Counter sagaCreatedCounter;
    private Counter sagaCompletedCounter;
    private Counter sagaFailedCounter;
    private Counter sagaCompensatedCounter;
    private Counter sagaRetryCounter;
    private Counter sagaRecoveryCounter;
    private Counter sagaTimeoutCounter;
    
    // Timers
    private Timer sagaDurationTimer;
    private Timer sagaStepDurationTimer;
    private Timer sagaRecoveryTimer;
    
    // Gauges
    private final AtomicLong activeSagasCount = new AtomicLong(0);
    private final AtomicLong pendingSagasCount = new AtomicLong(0);
    private final AtomicLong compensatingSagasCount = new AtomicLong(0);
    
    @PostConstruct
    public void initializeMetrics() {
        logger.info("Initializing saga metrics...");
        
        // Initialize counters
        sagaCreatedCounter = Counter.builder("saga.created.total")
                .description("Total number of sagas created")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        sagaCompletedCounter = Counter.builder("saga.completed.total")
                .description("Total number of sagas completed successfully")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        sagaFailedCounter = Counter.builder("saga.failed.total")
                .description("Total number of sagas failed")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        sagaCompensatedCounter = Counter.builder("saga.compensated.total")
                .description("Total number of sagas compensated")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        sagaRetryCounter = Counter.builder("saga.retry.total")
                .description("Total number of saga retry attempts")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        sagaRecoveryCounter = Counter.builder("saga.recovery.total")
                .description("Total number of saga recovery attempts")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        sagaTimeoutCounter = Counter.builder("saga.timeout.total")
                .description("Total number of saga timeouts")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        // Initialize timers
        sagaDurationTimer = Timer.builder("saga.duration")
                .description("Duration of saga execution from start to completion")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        sagaStepDurationTimer = Timer.builder("saga.step.duration")
                .description("Duration of individual saga steps")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        sagaRecoveryTimer = Timer.builder("saga.recovery.duration")
                .description("Duration of saga recovery operations")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("saga.active.count")
                .description("Current number of active sagas")
                .tag("service", "order-service")
                .register(meterRegistry, this, SagaMetrics::getActiveSagasCount);
        
        Gauge.builder("saga.pending.count")
                .description("Current number of pending sagas")
                .tag("service", "order-service")
                .register(meterRegistry, this, SagaMetrics::getPendingSagasCount);
        
        Gauge.builder("saga.compensating.count")
                .description("Current number of compensating sagas")
                .tag("service", "order-service")
                .register(meterRegistry, this, SagaMetrics::getCompensatingSagasCount);
        
        Gauge.builder("saga.success.rate")
                .description("Saga success rate (completed / total processed)")
                .tag("service", "order-service")
                .register(meterRegistry, this, SagaMetrics::getSagaSuccessRate);
        
        Gauge.builder("saga.recovery.success.rate")
                .description("Saga recovery success rate")
                .tag("service", "order-service")
                .register(meterRegistry, this, SagaMetrics::getRecoverySuccessRate);
        
        logger.info("Saga metrics initialized successfully");
    }
    
    /**
     * Event listeners for automatic metrics collection
     */
    
    public void recordSagaCreated(String sagaId, String orderId) {
        sagaCreatedCounter.increment();
        logger.debug("Saga created metric recorded: {} for order: {}", sagaId, orderId);
    }
    
    public void recordSagaCompleted(String sagaId, String orderId, Instant startTime) {
        sagaCompletedCounter.increment();
        
        if (startTime != null) {
            Duration duration = Duration.between(startTime, Instant.now());
            sagaDurationTimer.record(duration);
            logger.debug("Saga completed metric recorded: {} for order: {} duration: {}ms", 
                        sagaId, orderId, duration.toMillis());
        }
    }
    
    public void recordSagaFailed(String sagaId, String orderId, String reason, Instant startTime) {
        sagaFailedCounter.increment();
        
        if (startTime != null) {
            Duration duration = Duration.between(startTime, Instant.now());
            sagaDurationTimer.record(duration);
        }
        
        logger.debug("Saga failed metric recorded: {} for order: {} reason: {}", sagaId, orderId, reason);
    }
    
    public void recordSagaCompensated(String sagaId, String orderId) {
        sagaCompensatedCounter.increment();
        logger.debug("Saga compensated metric recorded: {} for order: {}", sagaId, orderId);
    }
    
    public void recordSagaRetry(String sagaId, String orderId, int retryCount) {
        sagaRetryCounter.increment();
        logger.debug("Saga retry metric recorded: {} for order: {} retry: {}", sagaId, orderId, retryCount);
    }
    
    public void recordSagaRecovery(String sagaId, String orderId, boolean successful, Duration duration) {
        sagaRecoveryCounter.increment();
        
        if (duration != null) {
            sagaRecoveryTimer.record(duration);
        }
        
        logger.debug("Saga recovery metric recorded: {} for order: {} successful: {} duration: {}ms", 
                    sagaId, orderId, successful, duration != null ? duration.toMillis() : 0);
    }
    
    public void recordSagaTimeout(String sagaId, String orderId) {
        sagaTimeoutCounter.increment();
        logger.debug("Saga timeout metric recorded: {} for order: {}", sagaId, orderId);
    }
    
    public void recordSagaStepDuration(String sagaId, SagaStep step, Duration duration) {
        sagaStepDurationTimer.record(duration, 
            "saga_id", sagaId,
            "step", step.name());
        
        logger.debug("Saga step duration recorded: {} step: {} duration: {}ms", 
                    sagaId, step, duration.toMillis());
    }
    
    /**
     * Real-time gauge calculations
     */
    
    private double getActiveSagasCount() {
        try {
            SagaStateManager.SagaStatistics stats = sagaStateManager.getSagaStatistics();
            long count = stats.getCountByStatus(SagaStatus.IN_PROGRESS) + 
                        stats.getCountByStatus(SagaStatus.INITIATED);
            activeSagasCount.set(count);
            return count;
        } catch (Exception e) {
            logger.error("Error calculating active sagas count: {}", e.getMessage());
            return activeSagasCount.get();
        }
    }
    
    private double getPendingSagasCount() {
        try {
            SagaStateManager.SagaStatistics stats = sagaStateManager.getSagaStatistics();
            long count = stats.getCountByStatus(SagaStatus.INITIATED);
            pendingSagasCount.set(count);
            return count;
        } catch (Exception e) {
            logger.error("Error calculating pending sagas count: {}", e.getMessage());
            return pendingSagasCount.get();
        }
    }
    
    private double getCompensatingSagasCount() {
        try {
            SagaStateManager.SagaStatistics stats = sagaStateManager.getSagaStatistics();
            long count = stats.getCountByStatus(SagaStatus.COMPENSATING);
            compensatingSagasCount.set(count);
            return count;
        } catch (Exception e) {
            logger.error("Error calculating compensating sagas count: {}", e.getMessage());
            return compensatingSagasCount.get();
        }
    }
    
    private double getSagaSuccessRate() {
        try {
            SagaStateManager.SagaStatistics stats = sagaStateManager.getSagaStatistics();
            
            long completed = stats.getCountByStatus(SagaStatus.COMPLETED);
            long failed = stats.getCountByStatus(SagaStatus.FAILED);
            long compensated = stats.getCountByStatus(SagaStatus.COMPENSATED);
            
            long total = completed + failed + compensated;
            
            if (total == 0) {
                return 1.0; // 100% if no sagas processed yet
            }
            
            return (double) completed / total;
            
        } catch (Exception e) {
            logger.error("Error calculating saga success rate: {}", e.getMessage());
            return 0.0;
        }
    }
    
    private double getRecoverySuccessRate() {
        try {
            SagaRecoveryService.RecoveryMetrics metrics = sagaRecoveryService.getRecoveryMetrics();
            
            if (metrics.getTotalRecoveryAttempts() == 0) {
                return 1.0; // 100% if no recovery attempts yet
            }
            
            return metrics.getSuccessRate() / 100.0; // Convert percentage to ratio
            
        } catch (Exception e) {
            logger.error("Error calculating recovery success rate: {}", e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Custom metrics for specific scenarios
     */
    
    public void recordCustomMetric(String metricName, String description, double value, String... tags) {
        try {
            Gauge.builder(metricName)
                    .description(description)
                    .tags(tags)
                    .register(meterRegistry, () -> value);
            
            logger.debug("Custom metric recorded: {} = {}", metricName, value);
            
        } catch (Exception e) {
            logger.error("Error recording custom metric {}: {}", metricName, e.getMessage());
        }
    }
    
    /**
     * Batch metrics update for performance
     */
    public void updateAllGauges() {
        try {
            getActiveSagasCount();
            getPendingSagasCount();
            getCompensatingSagasCount();
            
            logger.debug("All saga gauges updated");
            
        } catch (Exception e) {
            logger.error("Error updating saga gauges: {}", e.getMessage());
        }
    }
    
    /**
     * Get current metrics summary for health checks
     */
    public MetricsSummary getMetricsSummary() {
        return new MetricsSummary(
            activeSagasCount.get(),
            pendingSagasCount.get(),
            compensatingSagasCount.get(),
            getSagaSuccessRate(),
            getRecoverySuccessRate()
        );
    }
    
    /**
     * Metrics summary class for monitoring endpoints
     */
    public static class MetricsSummary {
        private final long activeSagas;
        private final long pendingSagas;
        private final long compensatingSagas;
        private final double successRate;
        private final double recoverySuccessRate;
        
        public MetricsSummary(long activeSagas, long pendingSagas, long compensatingSagas,
                            double successRate, double recoverySuccessRate) {
            this.activeSagas = activeSagas;
            this.pendingSagas = pendingSagas;
            this.compensatingSagas = compensatingSagas;
            this.successRate = successRate;
            this.recoverySuccessRate = recoverySuccessRate;
        }
        
        public long getActiveSagas() { return activeSagas; }
        public long getPendingSagas() { return pendingSagas; }
        public long getCompensatingSagas() { return compensatingSagas; }
        public double getSuccessRate() { return successRate; }
        public double getRecoverySuccessRate() { return recoverySuccessRate; }
        
        public boolean isHealthy() {
            return successRate >= 0.95 && recoverySuccessRate >= 0.90;
        }
    }
}