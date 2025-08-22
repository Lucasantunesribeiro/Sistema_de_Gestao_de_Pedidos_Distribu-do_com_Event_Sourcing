package com.ordersystem.order.saga;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * High-performance repository for SagaInstance persistence operations.
 * 
 * Optimized for:
 * - Recovery operations (timeout handling)
 * - Status-based queries
 * - Batch operations for performance
 * - Index-optimized queries
 */
@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, String> {
    
    /**
     * Find saga by order ID - most common query
     * Uses index: idx_saga_instances_order_id
     */
    Optional<SagaInstance> findByOrderId(String orderId);
    
    /**
     * Find active saga by order ID (not terminal states)
     * Optimized for checking if order already has active saga
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.orderId = :orderId AND s.sagaStatus IN ('INITIATED', 'IN_PROGRESS', 'COMPENSATING')")
    Optional<SagaInstance> findActiveSagaByOrderId(@Param("orderId") String orderId);
    
    /**
     * Find all sagas by status
     * Uses index: idx_saga_instances_status
     */
    List<SagaInstance> findBySagaStatus(SagaStatus status);
    
    /**
     * Find sagas by status with pagination (for large datasets)
     */
    Page<SagaInstance> findBySagaStatus(SagaStatus status, Pageable pageable);
    
    /**
     * Find sagas by multiple statuses
     * Optimized for recovery operations
     */
    List<SagaInstance> findBySagaStatusIn(List<SagaStatus> statuses);
    
    /**
     * Find timed-out sagas for recovery
     * Uses index: idx_saga_instances_timeout_recovery
     * Critical query for automatic recovery
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.timeoutAt <= :timeout AND s.sagaStatus IN :statuses")
    List<SagaInstance> findTimedOutSagas(@Param("timeout") Instant timeout, 
                                       @Param("statuses") List<SagaStatus> statuses);
    
    /**
     * Find sagas that can be retried (not exceeded max retries)
     * Used for automatic retry logic
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.timeoutAt <= :timeout AND s.sagaStatus IN :statuses AND s.retryCount < s.maxRetries")
    List<SagaInstance> findRetriableSagas(@Param("timeout") Instant timeout, 
                                        @Param("statuses") List<SagaStatus> statuses);
    
    /**
     * Find saga by correlation ID
     * Uses index: idx_saga_instances_correlation_id
     */
    Optional<SagaInstance> findByCorrelationId(String correlationId);
    
    /**
     * Find sagas by customer ID
     * For customer-specific saga management
     */
    List<SagaInstance> findByCustomerId(String customerId);
    
    /**
     * Find sagas created within time range
     * Uses index: idx_saga_instances_created_at
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.createdAt BETWEEN :startTime AND :endTime")
    List<SagaInstance> findSagasInTimeRange(@Param("startTime") Instant startTime, 
                                          @Param("endTime") Instant endTime);
    
    /**
     * Find active sagas older than specified time
     * For monitoring and alerting
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.createdAt <= :cutoffTime AND s.sagaStatus IN ('INITIATED', 'IN_PROGRESS', 'COMPENSATING')")
    List<SagaInstance> findOldActiveSagas(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Count sagas by status
     * For monitoring and metrics
     */
    long countBySagaStatus(SagaStatus status);
    
    /**
     * Count sagas by status and time range
     * For performance metrics and reporting
     */
    @Query("SELECT COUNT(s) FROM SagaInstance s WHERE s.sagaStatus = :status AND s.createdAt BETWEEN :startTime AND :endTime")
    long countBySagaStatusInTimeRange(@Param("status") SagaStatus status,
                                    @Param("startTime") Instant startTime,
                                    @Param("endTime") Instant endTime);
    
    /**
     * Find sagas with high retry count
     * For alerting on problematic sagas
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.retryCount >= :threshold AND s.sagaStatus IN ('IN_PROGRESS', 'COMPENSATING')")
    List<SagaInstance> findHighRetrySagas(@Param("threshold") int threshold);
    
    /**
     * Batch update timeout for multiple sagas
     * Performance optimization for bulk operations
     */
    @Modifying
    @Query("UPDATE SagaInstance s SET s.timeoutAt = :newTimeout, s.updatedAt = :now WHERE s.sagaId IN :sagaIds")
    int updateTimeoutForSagas(@Param("sagaIds") List<String> sagaIds, 
                            @Param("newTimeout") Instant newTimeout,
                            @Param("now") Instant now);
    
    /**
     * Batch update status for multiple sagas
     * Used for bulk status changes
     */
    @Modifying
    @Query("UPDATE SagaInstance s SET s.sagaStatus = :status, s.updatedAt = :now WHERE s.sagaId IN :sagaIds")
    int updateStatusForSagas(@Param("sagaIds") List<String> sagaIds, 
                           @Param("status") SagaStatus status,
                           @Param("now") Instant now);
    
    /**
     * Clean up completed sagas older than retention period
     * For data retention management
     */
    @Modifying
    @Query("DELETE FROM SagaInstance s WHERE s.sagaStatus IN ('COMPLETED', 'COMPENSATED', 'FAILED') AND s.updatedAt <= :cutoffTime")
    int deleteOldCompletedSagas(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Custom query for saga analytics
     * Returns aggregated data for monitoring dashboards
     */
    @Query("SELECT s.sagaStatus, COUNT(s), AVG(s.retryCount) FROM SagaInstance s GROUP BY s.sagaStatus")
    List<Object[]> getSagaStatusStatistics();
    
    /**
     * Find sagas by current step
     * For step-specific recovery operations
     */
    List<SagaInstance> findByCurrentStep(SagaStep currentStep);
    
    /**
     * Find sagas stuck in specific step for too long
     * For detecting and handling stuck sagas
     */
    @Query("SELECT s FROM SagaInstance s WHERE s.currentStep = :step AND s.updatedAt <= :cutoffTime AND s.sagaStatus = 'IN_PROGRESS'")
    List<SagaInstance> findStuckSagasInStep(@Param("step") SagaStep step, 
                                          @Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Performance monitoring queries
     */
    
    /**
     * Get average saga duration for completed sagas in time range
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (s.updatedAt - s.createdAt))) FROM SagaInstance s WHERE s.sagaStatus = 'COMPLETED' AND s.createdAt BETWEEN :startTime AND :endTime")
    Double getAverageSagaDuration(@Param("startTime") Instant startTime, 
                                @Param("endTime") Instant endTime);
    
    /**
     * Get saga success rate (completed vs total) in time range
     */
    @Query("SELECT " +
           "SUM(CASE WHEN s.sagaStatus = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
           "COUNT(s) as total " +
           "FROM SagaInstance s WHERE s.createdAt BETWEEN :startTime AND :endTime")
    Object[] getSagaSuccessRate(@Param("startTime") Instant startTime, 
                              @Param("endTime") Instant endTime);
    
    /**
     * Find sagas requiring immediate attention
     * Combines multiple failure criteria for alerting
     */
    @Query("SELECT s FROM SagaInstance s WHERE " +
           "(s.timeoutAt <= :now AND s.sagaStatus IN ('IN_PROGRESS', 'COMPENSATING')) OR " +
           "(s.retryCount >= s.maxRetries AND s.sagaStatus IN ('IN_PROGRESS', 'COMPENSATING')) OR " +
           "(s.sagaStatus = 'FAILED' AND s.updatedAt >= :recentTime)")
    List<SagaInstance> findSagasRequiringAttention(@Param("now") Instant now, 
                                                  @Param("recentTime") Instant recentTime);
}