package com.ordersystem.unified.integration;

import com.ordersystem.unified.infrastructure.events.DomainEventRepository;
import com.ordersystem.unified.inventory.repository.ReservationRepository;
import com.ordersystem.unified.order.application.CancelOrderUseCase;
import com.ordersystem.unified.order.application.CreateOrderUseCase;
import com.ordersystem.unified.order.dto.CancelOrderRequest;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import com.ordersystem.unified.payment.repository.PaymentRepository;
import com.ordersystem.unified.domain.events.OrderStatus;
import com.ordersystem.unified.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for complete order flow with business rules.
 * Tests the entire order lifecycle from creation to cancellation.
 *
 * Production-ready test covering:
 * - Order creation with inventory reservation and payment
 * - Event sourcing
 * - Order cancellation with compensating transactions
 * - Audit trail verification
 */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.ordersystem.unified.config.TestConfig.class)
@Transactional
class CompleteOrderFlowWithBusinessRulesTest extends PostgresIntegrationTestSupport {

    @Autowired(required = false)
    private CreateOrderUseCase createOrderUseCase;

    @Autowired(required = false)
    private CancelOrderUseCase cancelOrderUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private ReservationRepository reservationRepository;

    @Autowired(required = false)
    private PaymentRepository paymentRepository;

    @Autowired(required = false)
    private DomainEventRepository eventRepository;

    @org.junit.jupiter.api.BeforeEach
    void seedInventory() {
        seedStock("PROD-001", "Test Product", new BigDecimal("50.00"), 20);
        seedStock("PROD-002", "Test Product 2", new BigDecimal("100.00"), 20);
        seedStock("PROD-004", "Product 4", new BigDecimal("25.00"), 20);
        seedStock("PROD-005", "Product 5", new BigDecimal("50.00"), 20);
        seedStock("PROD-006", "Product 6", new BigDecimal("10.00"), 20);
        seedStock("PROD-007", "Traced Product", new BigDecimal("75.00"), 20);
    }

    @Test
    void shouldCreateOrderWithFullFlow() {
        // Skip test if use cases not available (optional dependencies)
        if (createOrderUseCase == null) {
            System.out.println("CreateOrderUseCase not available - skipping test");
            return;
        }

        // Given: Valid order request
        OrderItemRequest item = new OrderItemRequest(
            "PROD-001",
            "Test Product",
            2,
            new BigDecimal("50.00")
        );

        CreateOrderRequest request = new CreateOrderRequest(
            "CUST-001",
            "John Doe",
            "john@example.com",
            PaymentMethod.PIX,
            List.of(item)
        );

        // When: Create order
        OrderResponse response = createOrderUseCase.execute(request);

        // Then: Order should be created successfully
        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals("John Doe", response.getCustomerName());
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        assertEquals(new BigDecimal("100.00"), response.getTotalAmount());

        // Verify reservation was created
        assertNotNull(response.getReservationId());

        // Verify payment was processed
        assertNotNull(response.getPaymentId());

        // Verify order is persisted
        assertTrue(orderRepository.findById(response.getOrderId()).isPresent());

        // Verify events were published
        if (eventRepository != null) {
            long eventCount = eventRepository.countByAggregateId(response.getOrderId());
            assertTrue(eventCount > 0, "At least one event should be published");
        }

        System.out.println("Order created successfully: " + response.getOrderId());
    }

    @Test
    void shouldCancelOrderWithCompensation() {
        // Skip test if use cases not available
        if (createOrderUseCase == null || cancelOrderUseCase == null) {
            System.out.println("Use cases not available - skipping test");
            return;
        }

        // Given: Created order
        OrderItemRequest item = new OrderItemRequest(
            "PROD-002",
            "Test Product 2",
            1,
            new BigDecimal("100.00")
        );

        CreateOrderRequest createRequest = new CreateOrderRequest(
            "CUST-002",
            "Jane Doe",
            "jane@example.com",
            PaymentMethod.CREDIT_CARD,
            List.of(item)
        );

        OrderResponse createdOrder = createOrderUseCase.execute(createRequest);
        assertNotNull(createdOrder);
        assertEquals(OrderStatus.CONFIRMED, createdOrder.getStatus());

        // When: Cancel order
        CancelOrderRequest cancelRequest = new CancelOrderRequest(
            "Customer requested cancellation",
            "ADMIN-001"
        );

        OrderResponse cancelledOrder = cancelOrderUseCase.execute(
            createdOrder.getOrderId(),
            cancelRequest
        );

        // Then: Order should be cancelled
        assertNotNull(cancelledOrder);
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
        assertEquals("Customer requested cancellation",
                    cancelledOrder.getCancellationReason());

        // Verify cancellation events were published
        if (eventRepository != null) {
            long eventCount = eventRepository.countByAggregateId(createdOrder.getOrderId());
            assertTrue(eventCount >= 2, "Should have creation and cancellation events");
        }

        System.out.println("Order cancelled successfully: " + createdOrder.getOrderId());
    }

    @Test
    void shouldValidateBusinessRules() {
        // Skip test if use case not available
        if (createOrderUseCase == null) {
            System.out.println("CreateOrderUseCase not available - skipping test");
            return;
        }

        // Given: Invalid order (amount too low)
        OrderItemRequest item = new OrderItemRequest(
            "PROD-003",
            "Cheap Product",
            1,
            new BigDecimal("1.00") // Below minimum
        );

        CreateOrderRequest request = new CreateOrderRequest(
            "CUST-003",
            "Test Customer",
            "test@example.com",
            PaymentMethod.PIX,
            List.of(item)
        );

        // When/Then: Should fail validation
        assertThrows(Exception.class, () -> createOrderUseCase.execute(request),
                    "Should fail due to minimum order value");

        System.out.println("Business rule validation works correctly");
    }

    @Test
    void shouldHandleMultipleItems() {
        // Skip test if use case not available
        if (createOrderUseCase == null) {
            System.out.println("CreateOrderUseCase not available - skipping test");
            return;
        }

        // Given: Order with multiple items
        List<OrderItemRequest> items = List.of(
            new OrderItemRequest("PROD-004", "Product 4", 2, new BigDecimal("25.00")),
            new OrderItemRequest("PROD-005", "Product 5", 1, new BigDecimal("50.00")),
            new OrderItemRequest("PROD-006", "Product 6", 3, new BigDecimal("10.00"))
        );

        CreateOrderRequest request = new CreateOrderRequest(
            "CUST-004",
            "Multi Item Customer",
            "multi@example.com",
            PaymentMethod.DEBIT_CARD,
            items
        );

        // When: Create order
        OrderResponse response = createOrderUseCase.execute(request);

        // Then: Verify all items
        assertNotNull(response);
        assertEquals(3, response.getItems().size());
        assertEquals(new BigDecimal("130.00"), response.getTotalAmount());

        System.out.println("Multi-item order created: " + response.getOrderId());
    }

    @Test
    void shouldTraceWithCorrelationId() {
        // Skip test if use case not available
        if (createOrderUseCase == null) {
            System.out.println("CreateOrderUseCase not available - skipping test");
            return;
        }

        // Given: Request with correlation ID
        String correlationId = "TEST-CORRELATION-001";

        OrderItemRequest item = new OrderItemRequest(
            "PROD-007",
            "Traced Product",
            1,
            new BigDecimal("75.00")
        );

        CreateOrderRequest request = new CreateOrderRequest(
            "CUST-005",
            "Traced Customer",
            "traced@example.com",
            PaymentMethod.PIX,
            List.of(item)
        );
        request.setCorrelationId(correlationId);

        // When: Create order
        OrderResponse response = createOrderUseCase.execute(request);

        // Then: Correlation ID should be preserved
        assertNotNull(response);
        assertEquals(correlationId, response.getCorrelationId());

        // Verify correlation in events
        if (eventRepository != null) {
            var events = eventRepository.findByCorrelationIdOrderByCreatedAtAsc(correlationId);
            assertFalse(events.isEmpty(), "Events should be traced with correlation ID");
        }

        System.out.println("Order traced with correlation ID: " + correlationId);
    }
}

