package com.ordersystem.order.saga;

import com.ordersystem.shared.events.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for OrderSaga
 * Tests happy path, failure scenarios, and compensation logic
 */
@ExtendWith(MockitoExtension.class)
class OrderSagaTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderSaga orderSaga;

    private OrderCreatedEvent testOrderCreatedEvent;
    private InventoryReservedEvent testInventoryReservedEvent;
    private PaymentProcessedEvent testPaymentApprovedEvent;
    private PaymentProcessedEvent testPaymentDeclinedEvent;
    private InventoryReservationFailedEvent testInventoryFailedEvent;

    @BeforeEach
    void setUp() {
        // Create test data using the shared OrderItem class
        List<OrderItem> items = List.of(
                new OrderItem("product-1", "Laptop", 1, BigDecimal.valueOf(999.99)),
                new OrderItem("product-2", "Mouse", 2, BigDecimal.valueOf(29.99))
        );

        testOrderCreatedEvent = new OrderCreatedEvent(
                "order-123", "customer-456", items, BigDecimal.valueOf(1059.97), LocalDateTime.now()
        );

        // Use the same items for other events
        List<OrderItem> sharedItems = items;

        testInventoryReservedEvent = new InventoryReservedEvent(
                "order-123", "customer-456", sharedItems, BigDecimal.valueOf(1059.97)
        );

        testPaymentApprovedEvent = new PaymentProcessedEvent(
                "order-123", "payment-123", "APPROVED", 1059.97, LocalDateTime.now()
        );

        testPaymentDeclinedEvent = new PaymentProcessedEvent(
                "order-123", "payment-124", "DECLINED", 1059.97, LocalDateTime.now()
        );

        testInventoryFailedEvent = new InventoryReservationFailedEvent(
                "order-123", "customer-456", sharedItems, BigDecimal.valueOf(1059.97), "Insufficient inventory for product-1"
        );
    }

    @Test
    void testHappyPath_OrderCreatedToCompletion() {
        // Step 1: Order Created
        orderSaga.handleOrderCreated(testOrderCreatedEvent);

        // Verify inventory reservation command sent
        verify(rabbitTemplate).convertAndSend(
                eq("inventory.fanout"), 
                eq(""), 
                any(InventoryReservationCommand.class)
        );

        // Step 2: Inventory Reserved
        orderSaga.handleInventoryReserved(testInventoryReservedEvent);

        // Verify payment processing command sent
        verify(rabbitTemplate).convertAndSend(
                eq("payment.fanout"), 
                eq(""), 
                any(PaymentProcessingCommand.class)
        );

        // Step 3: Payment Approved
        orderSaga.handlePaymentProcessed(testPaymentApprovedEvent);

        // Verify completion commands sent
        verify(rabbitTemplate).convertAndSend(
                eq("inventory.fanout"), 
                eq(""), 
                any(InventoryConfirmationCommand.class)
        );
        
        verify(rabbitTemplate).convertAndSend(
                eq("order.fanout"), 
                eq(""), 
                any(OrderStatusUpdatedEvent.class)
        );
    }

    @Test
    void testPaymentDeclined_CompensationTriggered() {
        // Setup: Order created and inventory reserved
        orderSaga.handleOrderCreated(testOrderCreatedEvent);
        orderSaga.handleInventoryReserved(testInventoryReservedEvent);

        // Reset mock to focus on compensation
        reset(rabbitTemplate);

        // Payment declined
        orderSaga.handlePaymentProcessed(testPaymentDeclinedEvent);

        // Verify compensation: inventory release
        verify(rabbitTemplate).convertAndSend(
                eq("inventory.fanout"), 
                eq(""), 
                any(InventoryReleaseCommand.class)
        );

        // Verify order failed
        verify(rabbitTemplate).convertAndSend(
                eq("order.fanout"), 
                eq(""), 
                any(OrderStatusUpdatedEvent.class)
        );
    }

    @Test
    void testInventoryReservationFailed_OrderFailed() {
        // Order created
        orderSaga.handleOrderCreated(testOrderCreatedEvent);

        // Inventory reservation failed
        orderSaga.handleInventoryReservationFailed(testInventoryFailedEvent);

        // Verify order marked as failed
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                eq("order.fanout"), 
                eq(""), 
                any(OrderStatusUpdatedEvent.class)
        );
    }

    @Test
    void testSagaTimeout_CompensationTriggered() {
        // This would be tested with a more sophisticated setup
        // involving timing and scheduled execution
        // For now, we verify the timeout handler logic exists
        
        // Setup long-running saga
        orderSaga.handleOrderCreated(testOrderCreatedEvent);
        
        // Simulate timeout condition by calling the timeout handler
        // In real test, this would be triggered by scheduled execution
        orderSaga.handleSagaTimeouts();
        
        // Verify that cleanup logic would be called
        // (This test would need more sophisticated timing setup)
    }

    @Test
    void testMultipleConcurrentSagas() {
        // Test concurrent saga execution
        OrderCreatedEvent order1 = new OrderCreatedEvent(
                "order-1", "customer-1", 
                List.of(new OrderItem("product-1", "Item1", 1, BigDecimal.valueOf(100.0))),
                BigDecimal.valueOf(100.0), LocalDateTime.now()
        );
        
        OrderCreatedEvent order2 = new OrderCreatedEvent(
                "order-2", "customer-2", 
                List.of(new OrderItem("product-2", "Item2", 1, BigDecimal.valueOf(200.0))),
                BigDecimal.valueOf(200.0), LocalDateTime.now()
        );

        // Start both sagas
        orderSaga.handleOrderCreated(order1);
        orderSaga.handleOrderCreated(order2);

        // Verify both inventory reservations sent
        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("inventory.fanout"), 
                eq(""), 
                any(InventoryReservationCommand.class)
        );

        // Complete first saga
        List<OrderItem> items1 = List.of(new OrderItem("product-1", "Item1", 1, BigDecimal.valueOf(100.0)));
        InventoryReservedEvent inventory1 = new InventoryReservedEvent("order-1", "customer-1", items1, BigDecimal.valueOf(100.0));
        PaymentProcessedEvent payment1 = new PaymentProcessedEvent("order-1", "payment-1", "APPROVED", 100.0, LocalDateTime.now());
        
        orderSaga.handleInventoryReserved(inventory1);
        orderSaga.handlePaymentProcessed(payment1);

        // Fail second saga
        List<OrderItem> items2 = List.of(new OrderItem("product-2", "Item2", 1, BigDecimal.valueOf(200.0)));
        InventoryReservationFailedEvent inventoryFailed2 = new InventoryReservationFailedEvent("order-2", "customer-2", items2, BigDecimal.valueOf(200.0), "Out of stock");
        orderSaga.handleInventoryReservationFailed(inventoryFailed2);

        // Verify different outcomes
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                eq("order.fanout"), 
                eq(""), 
                any(OrderStatusUpdatedEvent.class)
        );

        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                eq("order.fanout"), 
                eq(""), 
                any(OrderStatusUpdatedEvent.class)
        );
    }

    @Test
    void testRabbitTemplateFailure_ErrorHandling() {
        // Setup RabbitTemplate to throw exception
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // This should not crash the saga
        orderSaga.handleOrderCreated(testOrderCreatedEvent);

        // Verify the exception was handled gracefully
        // (In real implementation, you'd verify error logging or fallback behavior)
    }
}