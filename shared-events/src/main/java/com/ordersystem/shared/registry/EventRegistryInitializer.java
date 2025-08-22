package com.ordersystem.shared.registry;

import com.ordersystem.shared.events.*;

/**
 * Initializes the event registry with all known event types.
 * This maintains backward compatibility by registering the same events
 * that were handled in the original switch-case statement.
 */
public class EventRegistryInitializer {
    
    private final EventTypeRegistry eventTypeRegistry;

    public EventRegistryInitializer(EventTypeRegistry eventTypeRegistry) {
        this.eventTypeRegistry = eventTypeRegistry;
    }

    /**
     * Initializes all known event types in the registry.
     */
    public void initializeEventTypes() {
        
        try {
            // Register all events that were in the original switch-case
            registerEventType("OrderCreatedEvent", OrderCreatedEvent.class);
            registerEventType("OrderStatusUpdatedEvent", OrderStatusUpdatedEvent.class);
            registerEventType("PaymentProcessedEvent", PaymentProcessedEvent.class);
            registerEventType("PaymentFailedEvent", PaymentFailedEvent.class);
            registerEventType("InventoryReservedEvent", InventoryReservedEvent.class);
            registerEventType("InventoryReservationFailedEvent", InventoryReservationFailedEvent.class);
            registerEventType("OrderCompletedEvent", OrderCompletedEvent.class);
            registerEventType("OrderCancelledEvent", OrderCancelledEvent.class);
            
            // Successfully initialized event types
                       
        } catch (Exception e) {
            throw new RuntimeException("Event registry initialization failed", e);
        }
    }

    /**
     * Registers a single event type, handling any potential exceptions.
     */
    private void registerEventType(String eventType, Class<?> eventClass) {
        try {
            eventTypeRegistry.register(eventType, eventClass);
        } catch (Exception e) {
            // Continue with other registrations rather than failing completely
        }
    }
}