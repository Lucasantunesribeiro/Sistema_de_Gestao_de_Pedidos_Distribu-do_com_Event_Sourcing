package com.ordersystem.unified.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.domain.events.BaseEvent;
import com.ordersystem.unified.domain.events.OrderCancelledEvent;
import com.ordersystem.unified.domain.events.OrderCreatedEvent;
import com.ordersystem.unified.domain.events.OrderItem;
import com.ordersystem.unified.domain.events.OrderStatusUpdatedEvent;
import com.ordersystem.unified.infrastructure.events.DomainEventEntity;
import com.ordersystem.unified.infrastructure.events.EventStore;
import com.ordersystem.unified.order.domain.OrderAggregate;
import com.ordersystem.unified.order.domain.RecordedOrderEvent;
import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.shared.exceptions.EventStreamCorruptedException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega e grava o {@link OrderAggregate} no event store.
 *
 * <p>{@link #load(String)} faz o replay do stream {@code Order:<id>} em ordem de versão.
 * {@link #save(OrderAggregate)} grava os eventos pendentes com a versão lida como versão esperada;
 * se outro writer avançou o stream, falha com {@code ConcurrencyException}.
 */
@Component
public class OrderEventSourcingRepository {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventSourcingRepository.class);

    private static final Map<String, Class<? extends BaseEvent>> EVENT_TYPES = Map.of(
        OrderCreatedEvent.class.getSimpleName(), OrderCreatedEvent.class,
        OrderStatusUpdatedEvent.class.getSimpleName(), OrderStatusUpdatedEvent.class,
        OrderCancelledEvent.class.getSimpleName(), OrderCancelledEvent.class
    );

    private final EventStore eventStore;
    private final ObjectMapper objectMapper;

    public OrderEventSourcingRepository(EventStore eventStore, ObjectMapper objectMapper) {
        this.eventStore = eventStore;
        this.objectMapper = objectMapper;
    }

    /** Reconstrói o agregado; devolve agregado vazio (versão 0) se não houver stream. */
    @Transactional(readOnly = true)
    public OrderAggregate load(String orderId) {
        List<RecordedOrderEvent> history = eventStore.readStream(OrderAggregate.AGGREGATE_TYPE, orderId).stream()
            .map(this::toRecordedEvent)
            .collect(Collectors.toList());
        return OrderAggregate.rehydrate(orderId, history);
    }

    /**
     * Carrega o agregado; se o pedido é anterior ao event sourcing (sem stream), inicia o stream
     * a partir do estado atual da tabela {@code system_orders} com um {@link OrderCreatedEvent}.
     * O histórico anterior não é recuperável; apenas o ponto de partida fica registrado.
     */
    @Transactional(readOnly = true)
    public OrderAggregate loadOrStartFrom(Order order) {
        OrderAggregate aggregate = load(order.getId());
        if (aggregate.exists()) {
            return aggregate;
        }
        logger.warn("Order {} has no event stream (legacy data); starting stream from current row", order.getId());
        List<OrderItem> items = order.getItems().stream()
            .map(item -> new OrderItem(item.getProductId(), item.getProductName(), item.getQuantity(),
                item.getUnitPrice()))
            .collect(Collectors.toList());
        return OrderAggregate.create(order.getId(), order.getCustomerId(), order.getCustomerName(), items,
            order.getTotalAmount(), order.getCorrelationId());
    }

    /** Grava os eventos pendentes com concorrência otimista e marca o agregado como confirmado. */
    @Transactional
    public void save(OrderAggregate aggregate) {
        if (aggregate.getUncommittedEvents().isEmpty()) {
            return;
        }
        eventStore.append(OrderAggregate.AGGREGATE_TYPE, aggregate.getOrderId(),
            aggregate.getCommittedVersion(), aggregate.getUncommittedEvents());
        aggregate.markCommitted();
    }

    private RecordedOrderEvent toRecordedEvent(DomainEventEntity entity) {
        Class<? extends BaseEvent> type = EVENT_TYPES.get(entity.getEventType());
        if (type == null) {
            throw new EventStreamCorruptedException(String.format(
                "Unknown event type %s in stream Order:%s (v%s)",
                entity.getEventType(), entity.getAggregateId(), entity.getAggregateVersion()));
        }
        try {
            return new RecordedOrderEvent(entity.getAggregateVersion(),
                objectMapper.readValue(entity.getEventData(), type));
        } catch (JsonProcessingException exception) {
            throw new EventStreamCorruptedException(String.format(
                "Cannot deserialize %s v%s of Order:%s", entity.getEventType(), entity.getAggregateVersion(),
                entity.getAggregateId()), exception);
        }
    }
}
