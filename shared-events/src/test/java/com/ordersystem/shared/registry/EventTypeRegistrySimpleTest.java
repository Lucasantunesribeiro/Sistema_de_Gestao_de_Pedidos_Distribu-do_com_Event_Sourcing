package com.ordersystem.shared.registry;

import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Event Type Registry Simple Tests")
class EventTypeRegistrySimpleTest {

    private EventTypeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EventTypeRegistry();
    }

    @Test
    @DisplayName("Should register and retrieve event type successfully")
    void shouldRegisterAndRetrieveEventTypeSuccessfully() {
        // When
        registry.register("OrderCreatedEvent", OrderCreatedEvent.class);
        
        // Then
        assertTrue(registry.isRegistered("OrderCreatedEvent"));
        assertEquals(OrderCreatedEvent.class, registry.getEventClass("OrderCreatedEvent"));
    }

    @Test
    @DisplayName("Should return false for unregistered event type")
    void shouldReturnFalseForUnregisteredEventType() {
        // When/Then
        assertFalse(registry.isRegistered("UnknownEvent"));
        assertNull(registry.getEventClass("UnknownEvent"));
    }

    @Test
    @DisplayName("Should handle multiple event registrations")
    void shouldHandleMultipleEventRegistrations() {
        // Given
        registry.register("OrderCreatedEvent", OrderCreatedEvent.class);
        registry.register("PaymentProcessedEvent", PaymentProcessedEvent.class);
        
        // When/Then
        assertTrue(registry.isRegistered("OrderCreatedEvent"));
        assertTrue(registry.isRegistered("PaymentProcessedEvent"));
        
        assertEquals(OrderCreatedEvent.class, registry.getEventClass("OrderCreatedEvent"));
        assertEquals(PaymentProcessedEvent.class, registry.getEventClass("PaymentProcessedEvent"));
    }

    @Test
    @DisplayName("Should throw exception when registering duplicate event type")
    void shouldThrowExceptionWhenRegisteringDuplicateEventType() {
        // Given
        registry.register("OrderCreatedEvent", OrderCreatedEvent.class);
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.register("OrderCreatedEvent", PaymentProcessedEvent.class)
        );
        
        assertTrue(exception.getMessage().contains("OrderCreatedEvent"));
        assertTrue(exception.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("Should provide all registered event types")
    void shouldProvideAllRegisteredEventTypes() {
        // Given
        registry.register("OrderCreatedEvent", OrderCreatedEvent.class);
        registry.register("PaymentProcessedEvent", PaymentProcessedEvent.class);
        
        // When
        var eventTypes = registry.getAllRegisteredEventTypes();
        
        // Then
        assertEquals(2, eventTypes.size());
        assertTrue(eventTypes.contains("OrderCreatedEvent"));
        assertTrue(eventTypes.contains("PaymentProcessedEvent"));
    }
}