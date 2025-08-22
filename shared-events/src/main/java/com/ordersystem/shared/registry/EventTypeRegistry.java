package com.ordersystem.shared.registry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for event types, providing thread-safe mapping between 
 * event type strings and their corresponding classes.
 * 
 * This registry eliminates the need for switch-case statements in event 
 * deserialization by providing O(1) lookup performance.
 */
public class EventTypeRegistry {
    
    private final ConcurrentHashMap<String, Class<?>> eventTypeMap = new ConcurrentHashMap<>();

    /**
     * Registers an event type with its corresponding class.
     * 
     * @param eventType the event type string (must be unique)
     * @param eventClass the event class
     * @throws IllegalArgumentException if eventType or eventClass is null, 
     *         or if eventType is already registered
     */
    public void register(String eventType, Class<?> eventClass) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (eventClass == null) {
            throw new IllegalArgumentException("Event class cannot be null");
        }
        
        Class<?> existingClass = eventTypeMap.putIfAbsent(eventType, eventClass);
        if (existingClass != null) {
            throw new IllegalArgumentException(
                String.format("Event type '%s' is already registered with class '%s'", 
                            eventType, existingClass.getName())
            );
        }
        
        // Debug: Registered event type successfully
    }

    /**
     * Checks if an event type is registered.
     * 
     * @param eventType the event type to check
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(String eventType) {
        if (eventType == null) {
            return false;
        }
        return eventTypeMap.containsKey(eventType);
    }

    /**
     * Gets the event class for a given event type.
     * 
     * @param eventType the event type
     * @return the event class, or null if not registered
     */
    public Class<?> getEventClass(String eventType) {
        return eventTypeMap.get(eventType);
    }

    /**
     * Gets all registered event types.
     * 
     * @return a set of all registered event type strings
     */
    public Set<String> getAllRegisteredEventTypes() {
        return Set.copyOf(eventTypeMap.keySet());
    }

    /**
     * Gets the total number of registered event types.
     * 
     * @return the count of registered event types
     */
    public int getRegisteredEventTypesCount() {
        return eventTypeMap.size();
    }
}