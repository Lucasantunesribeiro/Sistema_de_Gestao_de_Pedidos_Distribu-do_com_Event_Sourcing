package com.ordersystem.order.repository;

import com.ordersystem.order.exception.DatabaseConnectionException;
import com.ordersystem.order.model.Order;
import com.ordersystem.order.model.OrderEvent;
import com.ordersystem.shared.events.*;
import com.ordersystem.shared.registry.EventDispatcher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderEventStore {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventStore.class);

    @Autowired
    private OrderEventRepository eventRepository;
    
    @Autowired
    private EventDispatcher eventDispatcher;

    @Transactional
    @CircuitBreaker(name = "database", fallbackMethod = "saveEventsFallback")
    @Retry(name = "database")
    public void saveEvents(String aggregateId, List<Object> events, Integer expectedVersion) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Saving {} events for aggregate {}, correlationId: {}", 
                       events.size(), aggregateId, correlationId);

            // Verify expected version
            Integer currentVersion = eventRepository.findMaxVersionByAggregateId(aggregateId);
            if (currentVersion == null) {
                currentVersion = 0;
            }

            if (!currentVersion.equals(expectedVersion)) {
                throw new IllegalStateException(
                    String.format("Concurrency conflict: expected version %d, but current version is %d", 
                                expectedVersion, currentVersion));
            }

            // Save events
            for (int i = 0; i < events.size(); i++) {
                Object event = events.get(i);
                Integer version = expectedVersion + i + 1;
                
                OrderEvent orderEvent = new OrderEvent(
                    aggregateId,
                    event.getClass().getSimpleName(),
                    event,
                    version,
                    correlationId,
                    null, // causationId
                    null  // userId
                );

                eventRepository.save(orderEvent);
                
                logger.debug("Saved event {} with version {} for aggregate {}", 
                           event.getClass().getSimpleName(), version, aggregateId);
            }

            logger.info("Successfully saved {} events for aggregate {}", events.size(), aggregateId);

        } catch (DataAccessException e) {
            logger.error("Database error saving events for aggregate {}: {}", aggregateId, e.getMessage());
            throw new DatabaseConnectionException("Failed to save events", e);
        } catch (Exception e) {
            logger.error("Unexpected error saving events for aggregate {}: {}", aggregateId, e.getMessage(), e);
            throw e;
        }
    }

    @CircuitBreaker(name = "database", fallbackMethod = "getEventsFallback")
    @Retry(name = "database")
    public List<Object> getEvents(String aggregateId) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.debug("Loading events for aggregate {}, correlationId: {}", aggregateId, correlationId);

            List<OrderEvent> orderEvents = eventRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
            List<Object> events = new ArrayList<>();

            for (OrderEvent orderEvent : orderEvents) {
                Object event = deserializeEvent(orderEvent);
                if (event != null) {
                    events.add(event);
                }
            }

            logger.debug("Loaded {} events for aggregate {}", events.size(), aggregateId);
            return events;

        } catch (DataAccessException e) {
            logger.error("Database error loading events for aggregate {}: {}", aggregateId, e.getMessage());
            throw new DatabaseConnectionException("Failed to load events", e);
        } catch (Exception e) {
            logger.error("Unexpected error loading events for aggregate {}: {}", aggregateId, e.getMessage(), e);
            throw e;
        }
    }

    @CircuitBreaker(name = "database", fallbackMethod = "getOrderFallback")
    @Retry(name = "database")
    public Optional<Order> getOrder(String orderId) {
        try {
            List<Object> events = getEvents(orderId);
            
            if (events.isEmpty()) {
                return Optional.empty();
            }

            Order order = Order.fromEvents(events);
            return Optional.of(order);

        } catch (Exception e) {
            logger.error("Error reconstructing order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @CircuitBreaker(name = "database", fallbackMethod = "saveOrderFallback")
    @Retry(name = "database")
    public void saveOrder(Order order) {
        List<Object> uncommittedEvents = order.getUncommittedEvents();
        
        if (!uncommittedEvents.isEmpty()) {
            Integer expectedVersion = order.getVersion() - uncommittedEvents.size();
            saveEvents(order.getOrderId(), uncommittedEvents, expectedVersion);
            order.markEventsAsCommitted();
        }
    }

    private Object deserializeEvent(OrderEvent orderEvent) {
        try {
            String eventType = orderEvent.getEventType();
            String eventData = orderEvent.getEventData();
            
            // Use EventDispatcher instead of manual switch-case
            Object deserializedEvent = eventDispatcher.deserializeEvent(eventType, eventData);
            
            if (deserializedEvent == null) {
                logger.warn("Failed to deserialize event type: {}", eventType);
            }
            
            return deserializedEvent;
            
        } catch (Exception e) {
            logger.error("Failed to deserialize event {}: {}", orderEvent.getEventType(), e.getMessage());
            return null;
        }
    }

    // Fallback methods for circuit breaker
    public void saveEventsFallback(String aggregateId, List<Object> events, Integer expectedVersion, Exception ex) {
        logger.error("Circuit breaker activated for saveEvents: {}", ex.getMessage());
        throw new DatabaseConnectionException("Database service is currently unavailable", ex);
    }

    public List<Object> getEventsFallback(String aggregateId, Exception ex) {
        logger.error("Circuit breaker activated for getEvents: {}", ex.getMessage());
        throw new DatabaseConnectionException("Database service is currently unavailable", ex);
    }

    public Optional<Order> getOrderFallback(String orderId, Exception ex) {
        logger.error("Circuit breaker activated for getOrder: {}", ex.getMessage());
        throw new DatabaseConnectionException("Database service is currently unavailable", ex);
    }

    public void saveOrderFallback(Order order, Exception ex) {
        logger.error("Circuit breaker activated for saveOrder: {}", ex.getMessage());
        throw new DatabaseConnectionException("Database service is currently unavailable", ex);
    }
}