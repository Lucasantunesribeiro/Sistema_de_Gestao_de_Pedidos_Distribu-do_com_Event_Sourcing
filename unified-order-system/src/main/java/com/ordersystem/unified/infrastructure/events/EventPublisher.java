package com.ordersystem.unified.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.shared.events.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Event Publisher for domain events (Event Sourcing Pattern).
 * Persists all domain events for audit trail and event replay.
 *
 * Production-ready with transactional guarantees and error handling.
 */
@Component
public class EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);

    @Autowired
    private DomainEventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Publishes a domain event by persisting it to the event store.
     * Uses REQUIRES_NEW to ensure event is persisted even if parent transaction fails.
     *
     * @param event Domain event to publish
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(BaseEvent event) {
        try {
            logger.debug("Publishing event: type={}, aggregateId={}",
                        event.getClass().getSimpleName(), getAggregateId(event));

            // Serialize event data to JSON
            String eventData = objectMapper.writeValueAsString(event);

            // Create event entity
            DomainEventEntity eventEntity = DomainEventEntity.builder()
                .aggregateId(getAggregateId(event))
                .aggregateType(getAggregateType(event))
                .eventType(event.getClass().getSimpleName())
                .eventData(eventData)
                .correlationId(event.getCorrelationId())
                .build();

            // Persist event
            eventRepository.save(eventEntity);

            logger.info("Event published successfully: type={}, id={}",
                       eventEntity.getEventType(), eventEntity.getId());

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Event serialization failed", e);
        } catch (Exception e) {
            logger.error("Failed to publish event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Event publishing failed", e);
        }
    }

    /**
     * Publishes multiple events in a single transaction.
     *
     * @param events List of events to publish
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishAll(List<BaseEvent> events) {
        logger.debug("Publishing {} events", events.size());

        for (BaseEvent event : events) {
            try {
                String eventData = objectMapper.writeValueAsString(event);

                DomainEventEntity eventEntity = DomainEventEntity.builder()
                    .aggregateId(getAggregateId(event))
                    .aggregateType(getAggregateType(event))
                    .eventType(event.getClass().getSimpleName())
                    .eventData(eventData)
                    .correlationId(event.getCorrelationId())
                    .build();

                eventRepository.save(eventEntity);

            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event in batch: {}",
                           event.getClass().getSimpleName(), e);
                // Continue with other events
            }
        }

        logger.info("Published {} events successfully", events.size());
    }

    /**
     * Retrieves event history for an aggregate (for event replay).
     *
     * @param aggregateId Aggregate identifier
     * @return List of events in chronological order
     */
    @Transactional(readOnly = true)
    public List<DomainEventEntity> getEventHistory(String aggregateId) {
        logger.debug("Retrieving event history for aggregate: {}", aggregateId);
        return eventRepository.findByAggregateIdOrderByCreatedAtAsc(aggregateId);
    }

    /**
     * Retrieves events by correlation ID (for distributed tracing).
     *
     * @param correlationId Correlation identifier
     * @return List of related events
     */
    @Transactional(readOnly = true)
    public List<DomainEventEntity> getEventsByCorrelation(String correlationId) {
        logger.debug("Retrieving events by correlation: {}", correlationId);
        return eventRepository.findByCorrelationIdOrderByCreatedAtAsc(correlationId);
    }

    /**
     * Marks an event as processed.
     *
     * @param eventId Event identifier
     */
    @Transactional
    public void markAsProcessed(String eventId) {
        eventRepository.findById(eventId).ifPresent(event -> {
            event.markAsProcessed();
            eventRepository.save(event);
            logger.debug("Event marked as processed: {}", eventId);
        });
    }

    // Helper methods to extract aggregate information from events
    private String getAggregateId(BaseEvent event) {
        // Use reflection or pattern matching to extract aggregate ID
        if (event instanceof com.ordersystem.unified.shared.events.OrderCreatedEvent) {
            return ((com.ordersystem.unified.shared.events.OrderCreatedEvent) event).getOrderId();
        } else if (event instanceof com.ordersystem.unified.shared.events.PaymentProcessedEvent) {
            return ((com.ordersystem.unified.shared.events.PaymentProcessedEvent) event).getOrderId();
        } else if (event instanceof com.ordersystem.unified.shared.events.InventoryReservedEvent) {
            return ((com.ordersystem.unified.shared.events.InventoryReservedEvent) event).getOrderId();
        } else if (event instanceof com.ordersystem.unified.shared.events.OrderStatusUpdatedEvent) {
            return ((com.ordersystem.unified.shared.events.OrderStatusUpdatedEvent) event).getOrderId();
        } else if (event instanceof com.ordersystem.unified.shared.events.OrderCancelledEvent) {
            return ((com.ordersystem.unified.shared.events.OrderCancelledEvent) event).getOrderId();
        }

        logger.warn("Unknown event type for aggregate ID extraction: {}",
                   event.getClass().getSimpleName());
        return "UNKNOWN";
    }

    private String getAggregateType(BaseEvent event) {
        String className = event.getClass().getSimpleName();

        if (className.startsWith("Order")) {
            return "Order";
        } else if (className.startsWith("Payment")) {
            return "Payment";
        } else if (className.startsWith("Inventory")) {
            return "Inventory";
        }

        return "Unknown";
    }
}
