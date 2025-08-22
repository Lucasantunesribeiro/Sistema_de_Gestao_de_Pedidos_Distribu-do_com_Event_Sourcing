package com.ordersystem.order.config;

import com.ordersystem.shared.registry.EventDispatcher;
import com.ordersystem.shared.registry.EventRegistryInitializer;
import com.ordersystem.shared.registry.EventTypeRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Configuration for Event Registry Pattern components.
 * This replaces the manual switch-case statements in event deserialization.
 */
@Configuration
public class EventRegistryConfig {

    @Bean
    public EventTypeRegistry eventTypeRegistry() {
        return new EventTypeRegistry();
    }

    @Bean
    public EventDispatcher eventDispatcher(EventTypeRegistry eventTypeRegistry, ObjectMapper objectMapper) {
        return new EventDispatcher(eventTypeRegistry, objectMapper);
    }

    @Bean
    public EventRegistryInitializer eventRegistryInitializer(EventTypeRegistry eventTypeRegistry) {
        return new EventRegistryInitializer(eventTypeRegistry);
    }

    /**
     * Initialize event types on application startup
     */
    @PostConstruct
    public void initializeEventRegistry() {
        EventTypeRegistry registry = eventTypeRegistry();
        EventRegistryInitializer initializer = new EventRegistryInitializer(registry);
        initializer.initializeEventTypes();
    }
}