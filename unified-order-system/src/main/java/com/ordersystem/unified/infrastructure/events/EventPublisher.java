package com.ordersystem.unified.infrastructure.events;

import com.ordersystem.unified.domain.events.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Publica eventos de domínio gravando-os no event store / outbox ({@code domain_events}).
 *
 * <p>Todos os métodos participam da transação do chamador ({@code REQUIRED}). Antes, {@code publish}
 * usava {@code REQUIRES_NEW}: se o caso de uso falhasse depois, o pedido sofria rollback mas o evento
 * ficava gravado (e era enviado ao RabbitMQ) apontando para um pedido inexistente. Falha ao gravar o
 * evento agora também derruba a transação, em vez de ser engolida com um log.
 *
 * <p>Eventos do agregado Order devem ser gravados via {@code OrderEventSourcingRepository}, que usa
 * versão esperada; este publisher acrescenta no fim do stream (Payment/Inventory).
 */
@Component
public class EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);

    @Autowired
    private DomainEventRepository eventRepository;

    @Autowired
    private EventStore eventStore;

    /**
     * Grava o evento no stream do agregado correspondente, na transação corrente.
     *
     * @param event Domain event to publish
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void publish(BaseEvent event) {
        String aggregateType = getAggregateType(event);
        String aggregateId = getAggregateId(event);
        long version = eventStore.appendToStream(aggregateType, aggregateId, List.of(event));
        logger.info("Event stored: type={}, stream={}:{}, version={}",
            event.getClass().getSimpleName(), aggregateType, aggregateId, version);
    }

    /**
     * Mantido por compatibilidade: equivalente a {@link #publish(BaseEvent)}.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishWithinTransaction(BaseEvent event) {
        publish(event);
    }

    /**
     * Publishes multiple events in the current transaction.
     *
     * @param events List of events to publish
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishAll(List<BaseEvent> events) {
        events.forEach(this::publish);
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
        if (event instanceof com.ordersystem.unified.domain.events.OrderCreatedEvent) {
            return ((com.ordersystem.unified.domain.events.OrderCreatedEvent) event).getOrderId();
        } else if (event instanceof com.ordersystem.unified.domain.events.PaymentProcessedEvent) {
            return ((com.ordersystem.unified.domain.events.PaymentProcessedEvent) event).getOrderId();
        } else if (event instanceof com.ordersystem.unified.domain.events.InventoryReservedEvent) {
            return ((com.ordersystem.unified.domain.events.InventoryReservedEvent) event).getOrderId();
        } else if (event instanceof com.ordersystem.unified.domain.events.OrderStatusUpdatedEvent) {
            return ((com.ordersystem.unified.domain.events.OrderStatusUpdatedEvent) event).getOrderId();
        } else if (event instanceof com.ordersystem.unified.domain.events.OrderCancelledEvent) {
            return ((com.ordersystem.unified.domain.events.OrderCancelledEvent) event).getOrderId();
        } else if (event instanceof com.ordersystem.unified.domain.events.PaymentRefundedEvent) {
            return ((com.ordersystem.unified.domain.events.PaymentRefundedEvent) event).getOrderId();
        } else if (event instanceof com.ordersystem.unified.domain.events.InventoryReleasedEvent) {
            return ((com.ordersystem.unified.domain.events.InventoryReleasedEvent) event).getOrderId();
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

