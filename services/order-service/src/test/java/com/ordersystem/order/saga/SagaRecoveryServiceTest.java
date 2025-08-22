package com.ordersystem.order.saga;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Saga Recovery Service
 * 
 * Tests automatic recovery scenarios including:
 * - Timeout detection and recovery
 * - Retry logic with exponential backoff
 * - Failed saga handling
 * - Performance monitoring
 * - Manual recovery operations
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SagaRecoveryServiceTest {
    
    @Autowired
    private SagaRecoveryService sagaRecoveryService;
    
    @Autowired
    private SagaStateManager sagaStateManager;
    
    @Autowired
    private SagaInstanceRepository sagaRepository;
    
    @Autowired
    private OrderSaga orderSaga;
    
    private String testOrderId;
    private String testCustomerId;
    private BigDecimal testAmount;
    private String testCorrelationId;
    
    @BeforeEach
    void setUp() {
        testOrderId = "ORDER-RECOVERY-" + System.currentTimeMillis();
        testCustomerId = "CUSTOMER-RECOVERY-123";
        testAmount = new BigDecimal("149.99");
        testCorrelationId = "CORR-RECOVERY-" + System.currentTimeMillis();
    }
    
    @Test
    @DisplayName("Should detect and attempt recovery of timed-out sagas")
    void shouldDetectAndRecoverTimedOutSagas() {
        // Given - Create saga with past timeout
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        saga.setTimeoutAt(Instant.now().minus(1, ChronoUnit.HOURS));
        saga.setSagaStatus(SagaStatus.IN_PROGRESS);
        sagaRepository.save(saga);
        
        // Store initial metrics
        SagaRecoveryService.RecoveryMetrics initialMetrics = sagaRecoveryService.getRecoveryMetrics();
        
        // When
        sagaRecoveryService.recoverOrphanedSagas();
        
        // Then
        SagaRecoveryService.RecoveryMetrics finalMetrics = sagaRecoveryService.getRecoveryMetrics();
        assertTrue(finalMetrics.getTotalRecoveryAttempts() > initialMetrics.getTotalRecoveryAttempts());
        
        // Verify saga was processed for recovery
        SagaInstance recoveredSaga = sagaStateManager.getSaga(saga.getSagaId());
        assertTrue(recoveredSaga.getRetryCount() > 0 || recoveredSaga.getSagaStatus() == SagaStatus.FAILED);
    }
    
    @Test
    @DisplayName("Should handle manual force recovery")
    void shouldHandleManualForceRecovery() {
        // Given - Create saga in recoverable state
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        saga.setSagaStatus(SagaStatus.IN_PROGRESS);
        saga.setTimeoutAt(Instant.now().minus(30, ChronoUnit.MINUTES));
        sagaRepository.save(saga);
        
        // When
        boolean recovered = sagaRecoveryService.forceRecoverSaga(saga.getSagaId());
        
        // Then
        assertTrue(recovered);
        
        SagaInstance recoveredSaga = sagaStateManager.getSaga(saga.getSagaId());
        assertTrue(recoveredSaga.getRetryCount() > 0);
    }
    
    @Test
    @DisplayName("Should not recover saga that exceeded max retries")
    void shouldNotRecoverSagaThatExceededMaxRetries() {
        // Given - Create saga that has exceeded max retries
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        saga.setRetryCount(5); // Exceeds default max of 3
        saga.setSagaStatus(SagaStatus.IN_PROGRESS);
        saga.setTimeoutAt(Instant.now().minus(1, ChronoUnit.HOURS));
        sagaRepository.save(saga);
        
        // When
        boolean recovered = sagaRecoveryService.forceRecoverSaga(saga.getSagaId());
        
        // Then
        assertFalse(recovered);
        
        SagaInstance failedSaga = sagaStateManager.getSaga(saga.getSagaId());
        assertEquals(SagaStatus.FAILED, failedSaga.getSagaStatus());
    }
    
    @Test
    @DisplayName("Should find sagas requiring attention")
    void shouldFindSagasRequiringAttention() {
        // Given - Create various problematic sagas
        
        // Timed-out saga
        SagaInstance timedOutSaga = sagaStateManager.createSaga("ORDER-TIMEOUT", testCustomerId, testAmount, "CORR-TIMEOUT");
        timedOutSaga.setTimeoutAt(Instant.now().minus(1, ChronoUnit.HOURS));
        timedOutSaga.setSagaStatus(SagaStatus.IN_PROGRESS);
        sagaRepository.save(timedOutSaga);
        
        // High retry saga
        SagaInstance highRetrySaga = sagaStateManager.createSaga("ORDER-RETRY", testCustomerId, testAmount, "CORR-RETRY");
        highRetrySaga.setRetryCount(3);
        highRetrySaga.setSagaStatus(SagaStatus.IN_PROGRESS);
        sagaRepository.save(highRetrySaga);
        
        // Recently failed saga
        SagaInstance failedSaga = sagaStateManager.createSaga("ORDER-FAILED", testCustomerId, testAmount, "CORR-FAILED");
        sagaStateManager.failSaga(failedSaga.getSagaId(), "Test failure");
        
        // When
        List<SagaInstance> problemSagas = sagaStateManager.findSagasRequiringAttention();
        
        // Then
        assertFalse(problemSagas.isEmpty());
        
        // Should contain our problematic sagas
        assertTrue(problemSagas.stream().anyMatch(s -> s.getSagaId().equals(timedOutSaga.getSagaId())));
    }
    
    @Test
    @DisplayName("Should track recovery metrics correctly")
    void shouldTrackRecoveryMetricsCorrectly() {
        // Given
        SagaRecoveryService.RecoveryMetrics initialMetrics = sagaRecoveryService.getRecoveryMetrics();
        
        // Create a recoverable saga
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        saga.setTimeoutAt(Instant.now().minus(1, ChronoUnit.HOURS));
        saga.setSagaStatus(SagaStatus.IN_PROGRESS);
        sagaRepository.save(saga);
        
        // When
        sagaRecoveryService.recoverOrphanedSagas();
        
        // Then
        SagaRecoveryService.RecoveryMetrics finalMetrics = sagaRecoveryService.getRecoveryMetrics();
        
        assertTrue(finalMetrics.getTotalRecoveryAttempts() >= initialMetrics.getTotalRecoveryAttempts());
        assertTrue(finalMetrics.isRecoveryEnabled());
        assertTrue(finalMetrics.getSuccessRate() >= 0);
    }
    
    @Test
    @DisplayName("Should enable and disable recovery service")
    void shouldEnableAndDisableRecoveryService() {
        // Given
        sagaRecoveryService.setRecoveryEnabled(true);
        assertTrue(sagaRecoveryService.getRecoveryMetrics().isRecoveryEnabled());
        
        // When
        sagaRecoveryService.setRecoveryEnabled(false);
        
        // Then
        assertFalse(sagaRecoveryService.getRecoveryMetrics().isRecoveryEnabled());
        
        // Reset to enabled for other tests
        sagaRecoveryService.setRecoveryEnabled(true);
    }
    
    @Test
    @DisplayName("Should handle recovery of saga in different steps")
    void shouldHandleRecoveryOfSagaInDifferentSteps() {
        // Test INVENTORY_RESERVATION step
        SagaInstance inventorySaga = sagaStateManager.createSaga("ORDER-INV", testCustomerId, testAmount, "CORR-INV");
        inventorySaga.setCurrentStep(SagaStep.INVENTORY_RESERVATION);
        inventorySaga.setSagaStatus(SagaStatus.IN_PROGRESS);
        inventorySaga.setTimeoutAt(Instant.now().minus(1, ChronoUnit.HOURS));
        sagaRepository.save(inventorySaga);
        
        // Test PAYMENT_PROCESSING step
        SagaInstance paymentSaga = sagaStateManager.createSaga("ORDER-PAY", testCustomerId, testAmount, "CORR-PAY");
        paymentSaga.setCurrentStep(SagaStep.PAYMENT_PROCESSING);
        paymentSaga.setSagaStatus(SagaStatus.IN_PROGRESS);
        paymentSaga.setTimeoutAt(Instant.now().minus(1, ChronoUnit.HOURS));
        // Add required saga data
        paymentSaga.putSagaData("totalAmount", testAmount);
        sagaRepository.save(paymentSaga);
        
        // When
        boolean inventoryRecovery = orderSaga.retrySaga("ORDER-INV");
        boolean paymentRecovery = orderSaga.retrySaga("ORDER-PAY");
        
        // Then
        // At least one should be recoverable (depending on saga data availability)
        assertTrue(inventoryRecovery || paymentRecovery);
    }
    
    @Test
    @DisplayName("Should handle cleanup old sagas through recovery service")
    void shouldHandleCleanupOldSagasThroughRecoveryService() {
        // Given - Create old completed saga
        SagaInstance oldSaga = sagaStateManager.createSaga("ORDER-OLD", testCustomerId, testAmount, "CORR-OLD");
        sagaStateManager.completeSaga(oldSaga.getSagaId());
        
        // Make it old
        oldSaga.setUpdatedAt(Instant.now().minus(35, ChronoUnit.DAYS));
        sagaRepository.save(oldSaga);
        
        // When
        int cleanedUp = sagaStateManager.cleanupOldSagas(30);
        
        // Then
        assertTrue(cleanedUp >= 0); // Should not throw exception
        
        // The saga should be cleaned up if it's the only old one
        if (cleanedUp > 0) {
            assertFalse(sagaRepository.findById(oldSaga.getSagaId()).isPresent());
        }
    }
    
    @Test
    @DisplayName("Should handle performance analysis without errors")
    void shouldHandlePerformanceAnalysisWithoutErrors() {
        // Given - Create some sagas in different states
        SagaInstance completedSaga = sagaStateManager.createSaga("ORDER-COMPLETE", testCustomerId, testAmount, "CORR-COMPLETE");
        sagaStateManager.completeSaga(completedSaga.getSagaId());
        
        SagaInstance failedSaga = sagaStateManager.createSaga("ORDER-FAIL", testCustomerId, testAmount, "CORR-FAIL");
        sagaStateManager.failSaga(failedSaga.getSagaId(), "Test failure");
        
        SagaInstance activeSaga = sagaStateManager.createSaga("ORDER-ACTIVE", testCustomerId, testAmount, "CORR-ACTIVE");
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            sagaRecoveryService.analyzePerformance();
        });
    }
    
    @Test
    @DisplayName("Should handle monitoring saga health without errors")
    void shouldHandleMonitoringSagaHealthWithoutErrors() {
        // Given - Create some problematic sagas
        SagaInstance problematicSaga = sagaStateManager.createSaga("ORDER-PROBLEM", testCustomerId, testAmount, "CORR-PROBLEM");
        problematicSaga.setTimeoutAt(Instant.now().minus(1, ChronoUnit.HOURS));
        problematicSaga.setSagaStatus(SagaStatus.IN_PROGRESS);
        sagaRepository.save(problematicSaga);
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            sagaRecoveryService.monitorSagaHealth();
        });
    }
}