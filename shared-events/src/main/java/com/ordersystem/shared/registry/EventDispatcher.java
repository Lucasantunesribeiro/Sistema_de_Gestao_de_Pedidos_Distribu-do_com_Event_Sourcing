package com.ordersystem.shared.registry;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Main event dispatcher that replaces manual switch-case statements
 * for event deserialization. Provides type-safe, performant event handling
 * with O(1) lookup time.
 */
public class EventDispatcher {
    
    private final EventTypeRegistry eventTypeRegistry;
    private final ObjectMapper objectMapper;

    public EventDispatcher(EventTypeRegistry eventTypeRegistry, ObjectMapper objectMapper) {
        this.eventTypeRegistry = eventTypeRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Deserializes an event from its type string and JSON data.
     * This method replaces switch-case statements with registry-based lookup.
     * 
     * @param eventType the event type string
     * @param eventData the JSON event data
     * @return the deserialized event object, or null if deserialization fails
     */
    public Object deserializeEvent(String eventType, String eventData) {
        if (eventType == null) {
            return null;
        }

        if (!eventTypeRegistry.isRegistered(eventType)) {
            return null;
        }

        try {
            Class<?> eventClass = eventTypeRegistry.getEventClass(eventType);
            Object deserializedEvent = objectMapper.readValue(eventData, eventClass);
            
            return deserializedEvent;
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if an event type can be handled by this dispatcher.
     * 
     * @param eventType the event type to check
     * @return true if the event type is registered and can be handled
     */
    public boolean canHandle(String eventType) {
        return eventTypeRegistry.isRegistered(eventType);
    }
}