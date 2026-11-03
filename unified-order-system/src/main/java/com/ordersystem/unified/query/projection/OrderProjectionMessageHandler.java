package com.ordersystem.unified.query.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.infrastructure.messaging.IdempotentMessageProcessor;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

/**
 * Traduz a mensagem AMQP publicada pelo outbox em {@link OrderEventMessage} e aplica na projeção
 * com deduplicação por {@code eventId} ({@code messageId}).
 *
 * <p>Mensagens malformadas (sem id, sem versão, JSON inválido) lançam
 * {@link AmqpRejectAndDontRequeueException}: não adianta reprocessar, vão para a DLQ.
 */
@Component
public class OrderProjectionMessageHandler {

    public static final String CONSUMER = "order-projection";

    private final IdempotentMessageProcessor processor;
    private final OrderProjector projector;
    private final ObjectMapper objectMapper;

    public OrderProjectionMessageHandler(IdempotentMessageProcessor processor,
                                         OrderProjector projector,
                                         ObjectMapper objectMapper) {
        this.processor = processor;
        this.projector = projector;
        this.objectMapper = objectMapper;
    }

    /**
     * @return {@code true} se aplicado; {@code false} se era entrega duplicada
     */
    public boolean handle(Message message) {
        return handle(toEvent(message));
    }

    public boolean handle(OrderEventMessage event) {
        return processor.processOnce(CONSUMER, event.eventId(), () -> projector.apply(event));
    }

    OrderEventMessage toEvent(Message message) {
        MessageProperties properties = message.getMessageProperties();
        String eventId = firstNonBlank(properties.getMessageId(), header(properties, "eventId"));
        String orderId = header(properties, "aggregateId");
        String eventType = firstNonBlank(header(properties, "eventType"), properties.getType());
        String version = header(properties, "aggregateVersion");
        if (eventId == null || orderId == null || eventType == null || version == null) {
            throw new AmqpRejectAndDontRequeueException(
                "Order event without messageId/aggregateId/eventType/aggregateVersion");
        }
        if (!"Order".equals(header(properties, "aggregateType"))) {
            throw new AmqpRejectAndDontRequeueException("Not an Order stream event: " + eventId);
        }
        try {
            JsonNode payload = objectMapper.readTree(message.getBody());
            return new OrderEventMessage(eventId, orderId, Long.parseLong(version), eventType,
                parseTimestamp(header(properties, "occurredAt")), payload);
        } catch (IOException | NumberFormatException exception) {
            throw new AmqpRejectAndDontRequeueException("Malformed order event " + eventId, exception);
        }
    }

    private static LocalDateTime parseTimestamp(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String header(MessageProperties properties, String name) {
        Object value = properties.getHeader(name);
        return value == null ? null : firstNonBlank(value.toString(), null);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }
}
