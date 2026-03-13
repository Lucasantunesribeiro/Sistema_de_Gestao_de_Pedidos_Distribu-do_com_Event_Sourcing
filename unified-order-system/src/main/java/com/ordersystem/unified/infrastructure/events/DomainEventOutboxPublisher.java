package com.ordersystem.unified.infrastructure.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.common.messaging.MessagingConstants;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.messaging.outbox", name = "enabled", havingValue = "true")
public class DomainEventOutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventOutboxPublisher.class);

    private final DomainEventRepository domainEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxMessagingProperties properties;

    public DomainEventOutboxPublisher(DomainEventRepository domainEventRepository,
                                      RabbitTemplate rabbitTemplate,
                                      ObjectMapper objectMapper,
                                      OutboxMessagingProperties properties) {
        this.domainEventRepository = domainEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.messaging.outbox.fixed-delay:5000}")
    public void publishPendingEvents() {
        List<DomainEventEntity> pendingEvents = domainEventRepository.findPendingForDispatch(
            PageRequest.of(0, properties.getBatchSize())
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        logger.info("Dispatching {} pending domain event(s) to RabbitMQ", pendingEvents.size());
        for (DomainEventEntity event : pendingEvents) {
            dispatchEvent(event);
        }
    }

    @Transactional
    public void dispatchEvent(DomainEventEntity event) {
        if (event.isProcessed()) {
            return;
        }

        try {
            OutboundRoute route = resolveRoute(event);
            JsonNode payload = objectMapper.readTree(event.getEventData());

            rabbitTemplate.convertAndSend(route.exchange(), route.routingKey(), payload, message -> {
                message.getMessageProperties().setMessageId(event.getId());
                message.getMessageProperties().setType(event.getEventType());
                message.getMessageProperties().setHeader("aggregateId", event.getAggregateId());
                message.getMessageProperties().setHeader("aggregateType", event.getAggregateType());
                message.getMessageProperties().setHeader("eventType", event.getEventType());
                message.getMessageProperties().setHeader("eventId", event.getId());
                if (event.getCorrelationId() != null) {
                    message.getMessageProperties().setHeader(
                        MessagingConstants.CORRELATION_ID_HEADER,
                        event.getCorrelationId()
                    );
                }
                return message;
            });

            event.markAsProcessed();
            domainEventRepository.save(event);
            logger.info(
                "Domain event dispatched to broker: eventId={}, eventType={}, routingKey={}",
                event.getId(),
                event.getEventType(),
                route.routingKey()
            );
        } catch (Exception exception) {
            logger.error(
                "Failed to dispatch domain event to broker: eventId={}, eventType={}",
                event.getId(),
                event.getEventType(),
                exception
            );
        }
    }

    private OutboundRoute resolveRoute(DomainEventEntity event) {
        return switch (event.getEventType()) {
            case "OrderCreatedEvent" -> new OutboundRoute(
                MessagingConstants.ORDER_EXCHANGE,
                MessagingConstants.ORDER_CREATED_ROUTING_KEY
            );
            case "OrderCancelledEvent" -> new OutboundRoute(
                MessagingConstants.ORDER_EXCHANGE,
                MessagingConstants.ORDER_CANCELLED_ROUTING_KEY
            );
            case "OrderStatusUpdatedEvent" -> new OutboundRoute(
                MessagingConstants.ORDER_EXCHANGE,
                MessagingConstants.ORDER_STATUS_UPDATED_ROUTING_KEY
            );
            case "PaymentProcessedEvent" -> new OutboundRoute(
                MessagingConstants.PAYMENT_EXCHANGE,
                MessagingConstants.PAYMENT_PROCESSED_ROUTING_KEY
            );
            case "PaymentRefundedEvent" -> new OutboundRoute(
                MessagingConstants.PAYMENT_EXCHANGE,
                MessagingConstants.PAYMENT_REFUNDED_ROUTING_KEY
            );
            case "InventoryReservedEvent" -> new OutboundRoute(
                MessagingConstants.INVENTORY_EXCHANGE,
                MessagingConstants.INVENTORY_RESERVED_ROUTING_KEY
            );
            case "InventoryReleasedEvent" -> new OutboundRoute(
                MessagingConstants.INVENTORY_EXCHANGE,
                MessagingConstants.INVENTORY_RELEASED_ROUTING_KEY
            );
            default -> new OutboundRoute(
                exchangeForAggregate(event.getAggregateType()),
                event.getAggregateType().toLowerCase() + ".updated"
            );
        };
    }

    private String exchangeForAggregate(String aggregateType) {
        return switch (aggregateType) {
            case "Order" -> MessagingConstants.ORDER_EXCHANGE;
            case "Payment" -> MessagingConstants.PAYMENT_EXCHANGE;
            case "Inventory" -> MessagingConstants.INVENTORY_EXCHANGE;
            default -> MessagingConstants.ORDER_EXCHANGE;
        };
    }

    private record OutboundRoute(String exchange, String routingKey) {
    }
}
