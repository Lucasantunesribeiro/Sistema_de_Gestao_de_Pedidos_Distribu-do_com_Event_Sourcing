package com.ordersystem.unified.query.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.infrastructure.events.DomainEventEntity;
import com.ordersystem.unified.infrastructure.events.DomainEventRepository;
import com.ordersystem.unified.infrastructure.messaging.ProcessedEventRepository;
import com.ordersystem.unified.order.domain.OrderAggregate;
import com.ordersystem.unified.shared.exceptions.EventStreamCorruptedException;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconstrói {@code order_projection} do zero a partir do event store.
 *
 * <p>Apaga a projeção e os marcadores do consumidor e reaplica todos os eventos do tipo Order em
 * ordem (aggregate_id, aggregate_version) pelo mesmo caminho do consumidor. Como o projetor não lê
 * relógio, rodar duas vezes produz exatamente as mesmas linhas. Recomenda-se pausar o consumidor
 * durante o rebuild.
 */
@Service
public class OrderProjectionRebuilder {

    private static final Logger logger = LoggerFactory.getLogger(OrderProjectionRebuilder.class);

    private final DomainEventRepository eventRepository;
    private final OrderProjectionRepository projectionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderProjectionMessageHandler handler;
    private final ObjectMapper objectMapper;

    public OrderProjectionRebuilder(DomainEventRepository eventRepository,
                                    OrderProjectionRepository projectionRepository,
                                    ProcessedEventRepository processedEventRepository,
                                    OrderProjectionMessageHandler handler,
                                    ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.projectionRepository = projectionRepository;
        this.processedEventRepository = processedEventRepository;
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    public record RebuildResult(int eventsReplayed, long projections) {
    }

    @Transactional
    public RebuildResult rebuildAll() {
        projectionRepository.deleteAllInBatch();
        processedEventRepository.deleteByConsumer(OrderProjectionMessageHandler.CONSUMER);

        List<DomainEventEntity> events =
            eventRepository.findByAggregateTypeOrderByAggregateIdAscAggregateVersionAsc(OrderAggregate.AGGREGATE_TYPE);
        for (DomainEventEntity event : events) {
            handler.handle(toMessage(event));
        }
        long projections = projectionRepository.count();
        logger.info("Order projection rebuilt: {} event(s) replayed, {} projection(s)", events.size(), projections);
        return new RebuildResult(events.size(), projections);
    }

    private OrderEventMessage toMessage(DomainEventEntity event) {
        try {
            return new OrderEventMessage(event.getId(), event.getAggregateId(), event.getAggregateVersion(),
                event.getEventType(), event.getCreatedAt(), objectMapper.readTree(event.getEventData()));
        } catch (IOException exception) {
            throw new EventStreamCorruptedException("Cannot parse stored event " + event.getId(), exception);
        }
    }
}
