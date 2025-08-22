package com.ordersystem.order.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Automated Saga Recovery Service
 * 
 * Responsibilities:
 * - Detect and recover timed-out sagas
 * - Retry failed sagas with exponential backoff
 * - Monitor saga health and performance
 * - Cleanup old completed sagas
 * - Ensure 99.9% saga completion rate
 * 
 * Recovery Strategies:
 * 1. Timeout Recovery: Detect sagas that exceeded timeout
 * 2. Retry Logic: Automatic retry with max attempts
 * 3. Dead Letter Handling: Move failed sagas to failed state
 * 4. Performance Monitoring: Track recovery metrics
 */
@Service
public class SagaRecoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaRecoveryService.class);
    
    @Autowired
    private SagaStateManager sagaStateManager;
    
    @Autowired
    private OrderSaga orderSaga;
    
    // Configuration properties
    @Value("${app.saga.recovery.enabled:true}")
    private boolean recoveryEnabled;
    
    @Value("${app.saga.recovery.timeout-minutes:5}")
    private int sagaTimeoutMinutes;
    
    @Value("${app.saga.recovery.max-retries:3}")
    private int maxRetries;
    
    @Value("${app.saga.recovery.cleanup-days:30}")
    private int cleanupRetentionDays;
    
    @Value("${app.saga.recovery.batch-size:100}")
    private int recoveryBatchSize;
    
    // Metrics
    private final AtomicLong totalRecoveryAttempts = new AtomicLong(0);
    private final AtomicLong successfulRecoveries = new AtomicLong(0);
    private final AtomicLong failedRecoveries = new AtomicLong(0);
    private final AtomicLong cleanedUpSagas = new AtomicLong(0);
    
    /**
     * Main recovery process - runs every 30 seconds
     * Detects and recovers orphaned/timed-out sagas
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void recoverOrphanedSagas() {
        if (!recoveryEnabled) {
            return;
        }
        
        try {
            logger.debug("Starting saga recovery process...");
            
            List<SagaInstance> timedOutSagas = sagaStateManager.findTimedOutSagas();
            if (timedOutSagas.isEmpty()) {
                logger.debug("No timed-out sagas found");
                return;
            }
            
            logger.info("Found {} timed-out sagas for recovery", timedOutSagas.size());
            
            int recovered = 0;
            int failed = 0;
            
            for (SagaInstance saga : timedOutSagas) {
                boolean success = recoverSaga(saga);
                if (success) {
                    recovered++;
                    successfulRecoveries.incrementAndGet();
                } else {
                    failed++;
                    failedRecoveries.incrementAndGet();
                }
                
                totalRecoveryAttempts.incrementAndGet();
            }
            
            logger.info("Saga recovery completed - Recovered: {}, Failed: {}", recovered, failed);
            
        } catch (Exception e) {
            logger.error("Error during saga recovery process: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Recover individual saga
     * 
     * @param saga Saga to recover
     * @return true if recovery was successful, false otherwise
     */
    private boolean recoverSaga(SagaInstance saga) {
        try {
            logger.info("Attempting to recover saga: {} Order: {} Step: {} Retries: {}/{}", 
                       saga.getSagaId(), saga.getOrderId(), saga.getCurrentStep(), 
                       saga.getRetryCount(), saga.getMaxRetries());
            
            // Check if saga can be retried
            if (!saga.canRetry()) {
                logger.warn("Saga cannot be retried: {} - Moving to failed state", saga.getSagaId());
                sagaStateManager.failSaga(saga.getSagaId(), "Maximum retry attempts exceeded during recovery");
                return false;
            }
            
            // Attempt to retry the saga
            boolean retryInitiated = orderSaga.retrySaga(saga.getOrderId());
            
            if (retryInitiated) {
                logger.info("Successfully initiated retry for saga: {} Order: {}", 
                           saga.getSagaId(), saga.getOrderId());
                return true;
            } else {
                logger.warn("Failed to initiate retry for saga: {} Order: {}", 
                           saga.getSagaId(), saga.getOrderId());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error recovering saga: {} Order: {} - {}", 
                        saga.getSagaId(), saga.getOrderId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Health monitoring - runs every 2 minutes
     * Checks for sagas requiring immediate attention
     */
    @Scheduled(fixedRate = 120000) // Every 2 minutes
    public void monitorSagaHealth() {
        if (!recoveryEnabled) {
            return;
        }
        
        try {
            List<SagaInstance> problemSagas = sagaStateManager.findSagasRequiringAttention();
            
            if (!problemSagas.isEmpty()) {
                logger.warn("Found {} sagas requiring immediate attention", problemSagas.size());
                
                for (SagaInstance saga : problemSagas) {
                    logSagaIssue(saga);
                }
            }
            
            // Log recovery statistics
            if (totalRecoveryAttempts.get() > 0) {
                double successRate = (double) successfulRecoveries.get() / totalRecoveryAttempts.get() * 100;
                logger.info("Saga Recovery Statistics - Total: {}, Success: {}, Failed: {}, Success Rate: {:.2f}%",
                           totalRecoveryAttempts.get(), successfulRecoveries.get(), 
                           failedRecoveries.get(), successRate);
            }
            
        } catch (Exception e) {
            logger.error("Error during saga health monitoring: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Cleanup old completed sagas - runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldSagas() {
        if (!recoveryEnabled) {
            return;
        }
        
        try {
            logger.info("Starting cleanup of old completed sagas (retention: {} days)", cleanupRetentionDays);
            
            int deleted = sagaStateManager.cleanupOldSagas(cleanupRetentionDays);
            cleanedUpSagas.addAndGet(deleted);
            
            logger.info("Cleaned up {} old completed sagas", deleted);
            
        } catch (Exception e) {
            logger.error("Error during saga cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Performance analysis - runs every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // Every 15 minutes
    public void analyzePerformance() {
        if (!recoveryEnabled) {
            return;
        }
        
        try {
            SagaStateManager.SagaStatistics stats = sagaStateManager.getSagaStatistics();
            
            long activeCount = stats.getCountByStatus(SagaStatus.IN_PROGRESS) + 
                              stats.getCountByStatus(SagaStatus.INITIATED) +
                              stats.getCountByStatus(SagaStatus.COMPENSATING);
            
            long completedCount = stats.getCountByStatus(SagaStatus.COMPLETED);
            long failedCount = stats.getCountByStatus(SagaStatus.FAILED);
            long compensatedCount = stats.getCountByStatus(SagaStatus.COMPENSATED);
            
            double totalProcessed = completedCount + failedCount + compensatedCount;
            double successRate = totalProcessed > 0 ? (completedCount / totalProcessed * 100) : 0;
            
            logger.info("Saga Performance Analysis - Active: {}, Completed: {}, Failed: {}, Compensated: {}, Success Rate: {:.2f}%",
                       activeCount, completedCount, failedCount, compensatedCount, successRate);
            
            // Alert if success rate is below threshold
            if (successRate < 95.0 && totalProcessed > 10) {
                logger.error("🚨 ALERT: Saga success rate ({:.2f}%) is below 95% threshold!", successRate);
            }
            
        } catch (Exception e) {
            logger.error("Error during performance analysis: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Force recovery for specific saga (manual intervention)
     */
    public boolean forceRecoverSaga(String sagaId) {
        try {
            logger.info("Forcing recovery for saga: {}", sagaId);
            
            SagaInstance saga = sagaStateManager.getSaga(sagaId);
            boolean success = recoverSaga(saga);
            
            if (success) {
                logger.info("Successfully forced recovery for saga: {}", sagaId);
            } else {
                logger.warn("Failed to force recovery for saga: {}", sagaId);
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error forcing recovery for saga: {} - {}", sagaId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get recovery metrics for monitoring endpoints
     */
    public RecoveryMetrics getRecoveryMetrics() {
        return new RecoveryMetrics(
            totalRecoveryAttempts.get(),
            successfulRecoveries.get(),
            failedRecoveries.get(),
            cleanedUpSagas.get(),
            recoveryEnabled
        );
    }
    
    /**
     * Enable/disable recovery service
     */
    public void setRecoveryEnabled(boolean enabled) {
        this.recoveryEnabled = enabled;
        logger.info("Saga recovery service {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Private helper methods
     */
    
    private void logSagaIssue(SagaInstance saga) {
        if (saga.hasTimedOut()) {
            logger.warn("⏰ Saga timeout detected: {} Order: {} Age: {} minutes", 
                       saga.getSagaId(), saga.getOrderId(), 
                       ChronoUnit.MINUTES.between(saga.getCreatedAt(), Instant.now()));
        }
        
        if (saga.getRetryCount() >= saga.getMaxRetries()) {
            logger.error("🔄 Saga exceeded max retries: {} Order: {} Retries: {}", 
                        saga.getSagaId(), saga.getOrderId(), saga.getRetryCount());
        }
        
        if (saga.getSagaStatus() == SagaStatus.FAILED) {
            logger.error("❌ Recent saga failure: {} Order: {} Error: {}", 
                        saga.getSagaId(), saga.getOrderId(), saga.getLastErrorMessage());
        }
    }
    
    /**
     * Recovery metrics class for monitoring
     */
    public static class RecoveryMetrics {
        private final long totalRecoveryAttempts;
        private final long successfulRecoveries;
        private final long failedRecoveries;
        private final long cleanedUpSagas;
        private final boolean recoveryEnabled;
        
        public RecoveryMetrics(long totalRecoveryAttempts, long successfulRecoveries, 
                             long failedRecoveries, long cleanedUpSagas, boolean recoveryEnabled) {
            this.totalRecoveryAttempts = totalRecoveryAttempts;
            this.successfulRecoveries = successfulRecoveries;
            this.failedRecoveries = failedRecoveries;
            this.cleanedUpSagas = cleanedUpSagas;
            this.recoveryEnabled = recoveryEnabled;
        }
        
        public long getTotalRecoveryAttempts() { return totalRecoveryAttempts; }
        public long getSuccessfulRecoveries() { return successfulRecoveries; }
        public long getFailedRecoveries() { return failedRecoveries; }
        public long getCleanedUpSagas() { return cleanedUpSagas; }
        public boolean isRecoveryEnabled() { return recoveryEnabled; }
        
        public double getSuccessRate() {
            return totalRecoveryAttempts > 0 ? 
                (double) successfulRecoveries / totalRecoveryAttempts * 100 : 0;
        }
    }
}