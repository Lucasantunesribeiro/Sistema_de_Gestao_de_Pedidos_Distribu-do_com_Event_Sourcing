package com.ordersystem.shared.registry;

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

@DisplayName("Event Dispatcher Simple Tests")
class EventDispatcherSimpleTest {

    private EventTypeRegistry eventTypeRegistry;
    private EventDispatcher eventDispatcher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventTypeRegistry = new EventTypeRegistry();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        eventDispatcher = new EventDispatcher(eventTypeRegistry, objectMapper);
        
        // Register known event types
        eventTypeRegistry.register("OrderCreatedEvent", OrderCreatedEvent.class);
    }

    @Test
    @DisplayName("Should deserialize known event type successfully")
    void shouldDeserializeKnownEventTypeSuccessfully() throws Exception {
        // Given
        String eventData = """
            {
                "orderId": "123",
                "customerId": "456",
                "items": [],
                "totalAmount": 100.00,
                "timestamp": "2023-12-01T10:00:00"
            }
            """;
        
        // When
        Object result = eventDispatcher.deserializeEvent("OrderCreatedEvent", eventData);
        
        // Then
        assertNotNull(result);
        assertInstanceOf(OrderCreatedEvent.class, result);
        
        OrderCreatedEvent event = (OrderCreatedEvent) result;
        assertEquals("123", event.getOrderId());
        assertEquals("456", event.getCustomerId());
        assertEquals(0, event.getTotalAmount().compareTo(BigDecimal.valueOf(100.00)));
    }

    @Test
    @DisplayName("Should return null for unknown event type")
    void shouldReturnNullForUnknownEventType() {
        // Given
        String eventData = "{\"data\":\"value\"}";
        
        // When
        Object result = eventDispatcher.deserializeEvent("UnknownEvent", eventData);
        
        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle null event type")
    void shouldHandleNullEventType() {
        // When
        Object result = eventDispatcher.deserializeEvent(null, "{\"data\":\"value\"}");
        
        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle invalid JSON gracefully")
    void shouldHandleInvalidJsonGracefully() {
        // Given
        String invalidJson = "{ invalid json }";
        
        // When
        Object result = eventDispatcher.deserializeEvent("OrderCreatedEvent", invalidJson);
        
        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should check if event type can be handled")
    void shouldCheckIfEventTypeCanBeHandled() {
        // When/Then
        assertTrue(eventDispatcher.canHandle("OrderCreatedEvent"));
        assertFalse(eventDispatcher.canHandle("UnknownEvent"));
    }
}