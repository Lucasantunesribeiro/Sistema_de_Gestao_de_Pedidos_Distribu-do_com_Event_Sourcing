package com.ordersystem.order.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing saga state persistence operations.
 * 
 * Provides high-level abstractions for:
 * - Saga lifecycle management
 * - Transaction coordination
 * - Error handling and logging
 * - Performance optimization
 * 
 * All operations are transactional to ensure consistency.
 */
@Service
@Transactional
public class SagaStateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaStateManager.class);
    
    @Autowired
    private SagaInstanceRepository sagaRepository;
    
    /**
     * Create and start a new saga instance
     * 
     * @param orderId Order ID for the saga
     * @param customerId Customer ID
     * @param totalAmount Total order amount
     * @param correlationId Correlation ID for tracing
     * @return Created saga instance
     */
    public SagaInstance createSaga(String orderId, String customerId, BigDecimal totalAmount, String correlationId) {
        logger.info("Creating new saga for order: {} with correlation: {}", orderId, correlationId);
        
        // Check if saga already exists for this order
        Optional<SagaInstance> existingSaga = sagaRepository.findActiveSagaByOrderId(orderId);
        if (existingSaga.isPresent()) {
            logger.warn("Active saga already exists for order: {} - saga: {}", orderId, existingSaga.get().getSagaId());
            throw new IllegalStateException("Active saga already exists for order: " + orderId);
        }
        
        SagaInstance saga = SagaInstance.builder()
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(totalAmount)
                .correlationId(correlationId)
                .build();
        
        saga = sagaRepository.save(saga);
        logger.info("Created saga: {} for order: {}", saga.getSagaId(), orderId);
        
        return saga;
    }
    
    /**
     * Advance saga to next step
     * 
     * @param sagaId Saga ID
     * @return Updated saga instance
     */
    public SagaInstance advanceSaga(String sagaId) {
        logger.debug("Advancing saga: {}", sagaId);
        
        SagaInstance saga = getSaga(sagaId);
        saga.advanceToNextStep();
        
        saga = sagaRepository.save(saga);
        logger.info("Advanced saga: {} to step: {} with status: {}", 
                   sagaId, saga.getCurrentStep(), saga.getSagaStatus());
        
        return saga;
    }
    
    /**
     * Start compensation for a saga
     * 
     * @param sagaId Saga ID
     * @param errorMessage Error message that triggered compensation
     * @return Updated saga instance
     */
    public SagaInstance startCompensation(String sagaId, String errorMessage) {
        logger.warn("Starting compensation for saga: {} - reason: {}", sagaId, errorMessage);
        
        SagaInstance saga = getSaga(sagaId);
        saga.startCompensation(errorMessage);
        
        saga = sagaRepository.save(saga);
        logger.info("Started compensation for saga: {}", sagaId);
        
        return saga;
    }
    
    /**
     * Complete saga successfully
     * 
     * @param sagaId Saga ID
     * @return Updated saga instance
     */
    public SagaInstance completeSaga(String sagaId) {
        logger.info("Completing saga: {}", sagaId);
        
        SagaInstance saga = getSaga(sagaId);
        saga.complete();
        
        saga = sagaRepository.save(saga);
        logger.info("Completed saga: {} successfully", sagaId);
        
        return saga;
    }
    
    /**
     * Fail saga permanently
     * 
     * @param sagaId Saga ID
     * @param errorMessage Final error message
     * @return Updated saga instance
     */
    public SagaInstance failSaga(String sagaId, String errorMessage) {
        logger.error("Failing saga: {} - reason: {}", sagaId, errorMessage);
        
        SagaInstance saga = getSaga(sagaId);
        saga.fail(errorMessage);
        
        saga = sagaRepository.save(saga);
        logger.error("Failed saga: {} permanently", sagaId);
        
        return saga;
    }
    
    /**
     * Retry saga operation
     * 
     * @param sagaId Saga ID
     * @return Updated saga instance, or null if max retries exceeded
     */
    public SagaInstance retrySaga(String sagaId) {
        logger.info("Retrying saga: {}", sagaId);
        
        SagaInstance saga = getSaga(sagaId);
        
        if (!saga.canRetry()) {
            logger.warn("Saga: {} cannot be retried - max retries: {}, current: {}", 
                       sagaId, saga.getMaxRetries(), saga.getRetryCount());
            return failSaga(sagaId, "Maximum retry attempts exceeded");
        }
        
        boolean canContinue = saga.incrementRetryCount();
        if (canContinue) {
            saga.resetTimeout();
            saga.setSagaStatus(SagaStatus.IN_PROGRESS);
        } else {
            saga.fail("Maximum retry attempts exceeded");
        }
        
        saga = sagaRepository.save(saga);
        logger.info("Retried saga: {} - attempt: {}/{}", 
                   sagaId, saga.getRetryCount(), saga.getMaxRetries());
        
        return saga;
    }
    
    /**
     * Get saga by ID
     * 
     * @param sagaId Saga ID
     * @return Saga instance
     * @throws IllegalArgumentException if saga not found
     */
    @Transactional(readOnly = true)
    public SagaInstance getSaga(String sagaId) {
        return sagaRepository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga not found: " + sagaId));
    }
    
    /**
     * Get saga by order ID
     * 
     * @param orderId Order ID
     * @return Optional saga instance
     */
    @Transactional(readOnly = true)
    public Optional<SagaInstance> getSagaByOrderId(String orderId) {
        return sagaRepository.findByOrderId(orderId);
    }
    
    /**
     * Get active saga by order ID
     * 
     * @param orderId Order ID
     * @return Optional active saga instance
     */
    @Transactional(readOnly = true)
    public Optional<SagaInstance> getActiveSagaByOrderId(String orderId) {
        return sagaRepository.findActiveSagaByOrderId(orderId);
    }
    
    /**
     * Find timed-out sagas for recovery
     * 
     * @return List of timed-out sagas
     */
    @Transactional(readOnly = true)
    public List<SagaInstance> findTimedOutSagas() {
        List<SagaStatus> recoveryStatuses = List.of(
            SagaStatus.INITIATED, 
            SagaStatus.IN_PROGRESS, 
            SagaStatus.COMPENSATING
        );
        
        return sagaRepository.findTimedOutSagas(Instant.now(), recoveryStatuses);
    }
    
    /**
     * Find retriable sagas (not exceeded max retries)
     * 
     * @return List of retriable sagas
     */
    @Transactional(readOnly = true)
    public List<SagaInstance> findRetriableSagas() {
        List<SagaStatus> recoveryStatuses = List.of(
            SagaStatus.INITIATED, 
            SagaStatus.IN_PROGRESS, 
            SagaStatus.COMPENSATING
        );
        
        return sagaRepository.findRetriableSagas(Instant.now(), recoveryStatuses);
    }
    
    /**
     * Find sagas requiring immediate attention
     * 
     * @return List of sagas requiring attention
     */
    @Transactional(readOnly = true)
    public List<SagaInstance> findSagasRequiringAttention() {
        Instant now = Instant.now();
        Instant recentTime = now.minus(1, ChronoUnit.HOURS); // Recent failures within 1 hour
        
        return sagaRepository.findSagasRequiringAttention(now, recentTime);
    }
    
    /**
     * Find sagas by status
     * 
     * @param status Saga status
     * @return List of sagas with specified status
     */
    @Transactional(readOnly = true)
    public List<SagaInstance> findSagasByStatus(SagaStatus status) {
        return sagaRepository.findBySagaStatus(status);
    }
    
    /**
     * Get saga statistics for monitoring
     * 
     * @return Saga statistics
     */
    @Transactional(readOnly = true)
    public SagaStatistics getSagaStatistics() {
        List<Object[]> stats = sagaRepository.getSagaStatusStatistics();
        return new SagaStatistics(stats);
    }
    
    /**
     * Update saga data
     * 
     * @param sagaId Saga ID
     * @param key Data key
     * @param value Data value
     * @return Updated saga instance
     */
    public SagaInstance updateSagaData(String sagaId, String key, Object value) {
        logger.debug("Updating saga data for saga: {} - key: {}", sagaId, key);
        
        SagaInstance saga = getSaga(sagaId);
        saga.putSagaData(key, value);
        
        saga = sagaRepository.save(saga);
        logger.debug("Updated saga data for saga: {}", sagaId);
        
        return saga;
    }
    
    /**
     * Update compensation data
     * 
     * @param sagaId Saga ID
     * @param key Data key
     * @param value Data value
     * @return Updated saga instance
     */
    public SagaInstance updateCompensationData(String sagaId, String key, Object value) {
        logger.debug("Updating compensation data for saga: {} - key: {}", sagaId, key);
        
        SagaInstance saga = getSaga(sagaId);
        saga.putCompensationData(key, value);
        
        saga = sagaRepository.save(saga);
        logger.debug("Updated compensation data for saga: {}", sagaId);
        
        return saga;
    }
    
    /**
     * Batch operations for performance
     */
    
    /**
     * Update timeout for multiple sagas
     * 
     * @param sagaIds List of saga IDs
     * @param newTimeout New timeout
     * @return Number of updated sagas
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int updateTimeoutForSagas(List<String> sagaIds, Instant newTimeout) {
        if (sagaIds.isEmpty()) {
            return 0;
        }
        
        logger.info("Updating timeout for {} sagas", sagaIds.size());
        int updated = sagaRepository.updateTimeoutForSagas(sagaIds, newTimeout, Instant.now());
        logger.info("Updated timeout for {} sagas", updated);
        
        return updated;
    }
    
    /**
     * Clean up old completed sagas
     * 
     * @param retentionDays Number of days to retain completed sagas
     * @return Number of deleted sagas
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int cleanupOldSagas(int retentionDays) {
        Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        
        logger.info("Cleaning up completed sagas older than {} days", retentionDays);
        int deleted = sagaRepository.deleteOldCompletedSagas(cutoffTime);
        logger.info("Deleted {} old completed sagas", deleted);
        
        return deleted;
    }
    
    /**
     * Inner class for saga statistics
     */
    public static class SagaStatistics {
        private final List<Object[]> rawStats;
        
        public SagaStatistics(List<Object[]> rawStats) {
            this.rawStats = rawStats;
        }
        
        public long getCountByStatus(SagaStatus status) {
            return rawStats.stream()
                    .filter(stat -> stat[0].equals(status))
                    .mapToLong(stat -> ((Number) stat[1]).longValue())
                    .findFirst()
                    .orElse(0L);
        }
        
        public double getAverageRetryByStatus(SagaStatus status) {
            return rawStats.stream()
                    .filter(stat -> stat[0].equals(status))
                    .mapToDouble(stat -> ((Number) stat[2]).doubleValue())
                    .findFirst()
                    .orElse(0.0);
        }
        
        public List<Object[]> getRawStats() {
            return rawStats;
        }
    }
}