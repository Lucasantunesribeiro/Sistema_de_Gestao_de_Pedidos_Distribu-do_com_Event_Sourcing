package com.ordersystem.order.saga;

import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderItem;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Saga State Persistence
 * 
 * Tests the complete saga persistence flow including:
 * - Saga creation and storage
 * - State transitions
 * - Recovery scenarios
 * - Timeout handling
 * - Compensation flows
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SagaPersistenceIntegrationTest {
    
    @Autowired
    private SagaStateManager sagaStateManager;
    
    @Autowired
    private SagaInstanceRepository sagaRepository;
    
    @Autowired
    private OrderSaga orderSaga;
    
    @Autowired
    private SagaRecoveryService sagaRecoveryService;
    
    private String testOrderId;
    private String testCustomerId;
    private BigDecimal testAmount;
    private String testCorrelationId;
    private List<OrderItem> testOrderItems;
    
    @BeforeEach
    void setUp() {
        testOrderId = "ORDER-" + System.currentTimeMillis();
        testCustomerId = "CUSTOMER-123";
        testAmount = new BigDecimal("99.99");
        testCorrelationId = "CORR-" + System.currentTimeMillis();
        
        testOrderItems = Arrays.asList(
            new OrderItem("ITEM-1", "Test Item 1", 2, new BigDecimal("29.99")),
            new OrderItem("ITEM-2", "Test Item 2", 1, new BigDecimal("39.99"))
        );
    }
    
    @Test
    @DisplayName("Should create and persist saga successfully")
    void shouldCreateAndPersistSaga() {
        // When
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        
        // Then
        assertNotNull(saga);
        assertNotNull(saga.getSagaId());
        assertEquals(testOrderId, saga.getOrderId());
        assertEquals(testCustomerId, saga.getCustomerId());
        assertEquals(testAmount, saga.getTotalAmount());
        assertEquals(testCorrelationId, saga.getCorrelationId());
        assertEquals(SagaStatus.INITIATED, saga.getSagaStatus());
        assertEquals(SagaStep.INVENTORY_RESERVATION, saga.getCurrentStep());
        assertEquals(0, saga.getRetryCount());
        assertNotNull(saga.getCreatedAt());
        assertNotNull(saga.getUpdatedAt());
        assertNotNull(saga.getTimeoutAt());
        
        // Verify persistence
        Optional<SagaInstance> retrieved = sagaRepository.findByOrderId(testOrderId);
        assertTrue(retrieved.isPresent());
        assertEquals(saga.getSagaId(), retrieved.get().getSagaId());
    }
    
    @Test
    @DisplayName("Should advance saga to next step")
    void shouldAdvanceSagaToNextStep() {
        // Given
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        
        // When
        SagaInstance advanced = sagaStateManager.advanceSaga(saga.getSagaId());
        
        // Then
        assertEquals(SagaStep.PAYMENT_PROCESSING, advanced.getCurrentStep());
        assertEquals(SagaStatus.IN_PROGRESS, advanced.getSagaStatus());
        assertTrue(advanced.getUpdatedAt().isAfter(saga.getUpdatedAt()));
    }
    
    @Test
    @DisplayName("Should complete saga successfully")
    void shouldCompleteSagaSuccessfully() {
        // Given
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        
        // When
        SagaInstance completed = sagaStateManager.completeSaga(saga.getSagaId());
        
        // Then
        assertEquals(SagaStep.COMPLETED, completed.getCurrentStep());
        assertEquals(SagaStatus.COMPLETED, completed.getSagaStatus());
        assertNull(completed.getTimeoutAt()); // Timeout cleared on completion
    }
    
    @Test
    @DisplayName("Should handle saga compensation")
    void shouldHandleSagaCompensation() {
        // Given
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        String errorMessage = "Payment processing failed";
        
        // When
        SagaInstance compensated = sagaStateManager.startCompensation(saga.getSagaId(), errorMessage);
        
        // Then
        assertEquals(SagaStep.COMPENSATING, compensated.getCurrentStep());
        assertEquals(SagaStatus.COMPENSATING, compensated.getSagaStatus());
        assertEquals(errorMessage, compensated.getLastErrorMessage());
    }
    
    @Test
    @DisplayName("Should handle saga retry logic")
    void shouldHandleSagaRetryLogic() {
        // Given
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        
        // When - First retry
        SagaInstance retried1 = sagaStateManager.retrySaga(saga.getSagaId());
        
        // Then
        assertEquals(1, retried1.getRetryCount());
        assertTrue(retried1.canRetry());
        assertNotNull(retried1.getTimeoutAt());
        
        // When - Second retry
        SagaInstance retried2 = sagaStateManager.retrySaga(retried1.getSagaId());
        
        // Then
        assertEquals(2, retried2.getRetryCount());
        assertTrue(retried2.canRetry());
        
        // When - Third retry (should hit max)
        SagaInstance retried3 = sagaStateManager.retrySaga(retried2.getSagaId());
        
        // Then
        assertEquals(3, retried3.getRetryCount());
        assertFalse(retried3.canRetry());
        
        // When - Fourth retry attempt (should fail)
        SagaInstance retried4 = sagaStateManager.retrySaga(retried3.getSagaId());
        
        // Then
        assertEquals(SagaStatus.FAILED, retried4.getSagaStatus());
        assertEquals("Maximum retry attempts exceeded", retried4.getLastErrorMessage());
    }
    
    @Test
    @DisplayName("Should find timed-out sagas")
    void shouldFindTimedOutSagas() {
        // Given - Create saga with past timeout
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        saga.setTimeoutAt(Instant.now().minus(1, ChronoUnit.HOURS)); // Set timeout in the past
        sagaRepository.save(saga);
        
        // When
        List<SagaInstance> timedOut = sagaStateManager.findTimedOutSagas();
        
        // Then
        assertFalse(timedOut.isEmpty());
        assertTrue(timedOut.stream().anyMatch(s -> s.getSagaId().equals(saga.getSagaId())));
    }
    
    @Test
    @DisplayName("Should handle saga data persistence")
    void shouldHandleSagaDataPersistence() {
        // Given
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        
        // When
        sagaStateManager.updateSagaData(saga.getSagaId(), "orderItems", testOrderItems);
        sagaStateManager.updateSagaData(saga.getSagaId(), "customData", "test-value");
        
        // Then
        SagaInstance retrieved = sagaStateManager.getSaga(saga.getSagaId());
        assertNotNull(retrieved.getSagaData("orderItems"));
        assertEquals("test-value", retrieved.getSagaData("customData"));
    }
    
    @Test
    @DisplayName("Should handle compensation data persistence")
    void shouldHandleCompensationDataPersistence() {
        // Given
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        
        // When
        sagaStateManager.updateCompensationData(saga.getSagaId(), "inventoryReserved", true);
        sagaStateManager.updateCompensationData(saga.getSagaId(), "paymentId", "PAY-123");
        
        // Then
        SagaInstance retrieved = sagaStateManager.getSaga(saga.getSagaId());
        assertEquals(true, retrieved.getCompensationData("inventoryReserved"));
        assertEquals("PAY-123", retrieved.getCompensationData("paymentId"));
    }
    
    @Test
    @DisplayName("Should prevent duplicate saga creation for same order")
    void shouldPreventDuplicateSagaCreation() {
        // Given
        sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        
        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            sagaStateManager.createSaga(testOrderId, "OTHER-CUSTOMER", testAmount, "OTHER-CORR");
        });
    }
    
    @Test
    @DisplayName("Should find active saga by order ID")
    void shouldFindActiveSagaByOrderId() {
        // Given
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        
        // When
        Optional<SagaInstance> found = sagaStateManager.getActiveSagaByOrderId(testOrderId);
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(saga.getSagaId(), found.get().getSagaId());
        
        // When saga is completed
        sagaStateManager.completeSaga(saga.getSagaId());
        Optional<SagaInstance> foundAfterCompletion = sagaStateManager.getActiveSagaByOrderId(testOrderId);
        
        // Then
        assertFalse(foundAfterCompletion.isPresent());
    }
    
    @Test
    @DisplayName("Should handle OrderCreated event with saga persistence")
    void shouldHandleOrderCreatedEventWithSagaPersistence() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            testOrderId, testCustomerId, testOrderItems, testAmount, 
            Instant.now(), "PENDING"
        );
        
        // When
        orderSaga.handleOrderCreated(event);
        
        // Then
        Optional<SagaInstance> saga = sagaStateManager.getSagaByOrderId(testOrderId);
        assertTrue(saga.isPresent());
        assertEquals(testOrderId, saga.get().getOrderId());
        assertEquals(testCustomerId, saga.get().getCustomerId());
        assertEquals(testAmount, saga.get().getTotalAmount());
        assertEquals(SagaStatus.INITIATED, saga.get().getSagaStatus());
        assertEquals(SagaStep.INVENTORY_RESERVATION, saga.get().getCurrentStep());
    }
    
    @Test
    @DisplayName("Should cleanup old completed sagas")
    void shouldCleanupOldCompletedSagas() {
        // Given - Create and complete a saga
        SagaInstance saga = sagaStateManager.createSaga(testOrderId, testCustomerId, testAmount, testCorrelationId);
        sagaStateManager.completeSaga(saga.getSagaId());
        
        // Make it old by setting updated_at in the past
        saga.setUpdatedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        sagaRepository.save(saga);
        
        // When
        int cleaned = sagaStateManager.cleanupOldSagas(30);
        
        // Then
        assertTrue(cleaned > 0);
        assertFalse(sagaRepository.findById(saga.getSagaId()).isPresent());
    }
    
    @Test
    @DisplayName("Should handle saga statistics correctly")
    void shouldHandleSagaStatisticsCorrectly() {
        // Given - Create sagas in different states
        SagaInstance saga1 = sagaStateManager.createSaga("ORDER-1", testCustomerId, testAmount, "CORR-1");
        SagaInstance saga2 = sagaStateManager.createSaga("ORDER-2", testCustomerId, testAmount, "CORR-2");
        SagaInstance saga3 = sagaStateManager.createSaga("ORDER-3", testCustomerId, testAmount, "CORR-3");
        
        sagaStateManager.completeSaga(saga1.getSagaId());
        sagaStateManager.failSaga(saga2.getSagaId(), "Test failure");
        // saga3 remains in INITIATED state
        
        // When
        SagaStateManager.SagaStatistics stats = sagaStateManager.getSagaStatistics();
        
        // Then
        assertTrue(stats.getCountByStatus(SagaStatus.COMPLETED) >= 1);
        assertTrue(stats.getCountByStatus(SagaStatus.FAILED) >= 1);
        assertTrue(stats.getCountByStatus(SagaStatus.INITIATED) >= 1);
    }
}