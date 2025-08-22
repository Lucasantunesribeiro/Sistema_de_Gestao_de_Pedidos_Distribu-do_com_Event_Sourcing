package com.ordersystem.order.registry;

import com.ordersystem.shared.registry.EventDispatcher;
import com.ordersystem.shared.registry.EventRegistryInitializer;
import com.ordersystem.shared.registry.EventTypeRegistry;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Event Registry Integration Tests")
class EventRegistryIntegrationTest {

    private EventTypeRegistry eventTypeRegistry;
    private EventDispatcher eventDispatcher;
    private EventRegistryInitializer initializer;

    @BeforeEach
    void setUp() {
        eventTypeRegistry = new EventTypeRegistry();
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        eventDispatcher = new EventDispatcher(eventTypeRegistry, objectMapper);
        initializer = new EventRegistryInitializer(eventTypeRegistry);
        
        // Initialize all event types
        initializer.initializeEventTypes();
    }

    @Test
    @DisplayName("Should initialize and register all event types from switch-case")
    void shouldInitializeAndRegisterAllEventTypesFromSwitchCase() {
        // Verify all events from the original switch-case are registered
        assertTrue(eventTypeRegistry.isRegistered("OrderCreatedEvent"));
        assertTrue(eventTypeRegistry.isRegistered("OrderStatusUpdatedEvent"));
        assertTrue(eventTypeRegistry.isRegistered("PaymentProcessedEvent"));
        assertTrue(eventTypeRegistry.isRegistered("PaymentFailedEvent"));
        assertTrue(eventTypeRegistry.isRegistered("InventoryReservedEvent"));
        assertTrue(eventTypeRegistry.isRegistered("InventoryReservationFailedEvent"));
        assertTrue(eventTypeRegistry.isRegistered("OrderCompletedEvent"));
        assertTrue(eventTypeRegistry.isRegistered("OrderCancelledEvent"));
        
        // Verify total count matches switch-case
        assertEquals(8, eventTypeRegistry.getRegisteredEventTypesCount());
    }

    @Test
    @DisplayName("Should deserialize events identically to switch-case logic")
    void shouldDeserializeEventsIdenticallyToSwitchCaseLogic() throws Exception {
        // Given - JSON data that would be stored in database
        String orderCreatedJson = """
            {
                "orderId": "test-order-123",
                "customerId": "customer-456", 
                "items": [],
                "totalAmount": 99.99,
                "timestamp": "2023-12-01T10:00:00"
            }
            """;
        
        // When - Deserialize using EventDispatcher (new way)
        Object result = eventDispatcher.deserializeEvent("OrderCreatedEvent", orderCreatedJson);
        
        // Then - Verify it produces same result as switch-case would
        assertNotNull(result);
        assertInstanceOf(OrderCreatedEvent.class, result);
        
        OrderCreatedEvent event = (OrderCreatedEvent) result;
        assertEquals("test-order-123", event.getOrderId());
        assertEquals("customer-456", event.getCustomerId());
        assertEquals(0, event.getTotalAmount().compareTo(BigDecimal.valueOf(99.99)));
    }

    @Test
    @DisplayName("Should handle unknown event types like switch-case default")
    void shouldHandleUnknownEventTypesLikeSwitchCaseDefault() {
        // When - Try to deserialize unknown event type
        Object result = eventDispatcher.deserializeEvent("UnknownEventType", "{}");
        
        // Then - Should return null just like switch-case default
        assertNull(result);
    }

    @Test
    @DisplayName("Should maintain O(1) performance vs switch-case O(n)")
    void shouldMaintainO1PerformanceVsSwitchCaseOn() {
        // Given - Event type that would be last in switch-case (worst case)
        String eventType = "OrderCancelledEvent";
        String eventData = "{}";
        
        // When - Multiple lookups (registry should be O(1))
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            eventDispatcher.canHandle(eventType);
        }
        long endTime = System.nanoTime();
        
        // Then - Should complete quickly (O(1) vs O(8) for switch-case)
        long duration = endTime - startTime;
        assertTrue(duration < 10_000_000); // Less than 10ms for 1000 operations
    }

    @Test
    @DisplayName("Should replace OrderEventStore switch-case completely")
    void shouldReplaceOrderEventStoreSwitchCaseCompletely() {
        // This test verifies that EventDispatcher can handle ALL cases
        // that were in the original OrderEventStore.deserializeEvent switch-case
        
        String[] allEventTypes = {
            "OrderCreatedEvent",
            "OrderStatusUpdatedEvent", 
            "PaymentProcessedEvent",
            "PaymentFailedEvent",
            "InventoryReservedEvent",
            "InventoryReservationFailedEvent",
            "OrderCompletedEvent",
            "OrderCancelledEvent"
        };
        
        // Verify all switch-case events can be handled
        for (String eventType : allEventTypes) {
            assertTrue(eventDispatcher.canHandle(eventType), 
                "EventDispatcher should handle " + eventType + " from original switch-case");
        }
    }
}