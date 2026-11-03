package com.ordersystem.unified.query.projection;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

/**
 * Evento do stream Order como chega ao consumidor (mensagem RabbitMQ) ou ao rebuild (linha do
 * event store). {@code eventId} é o id da linha em {@code domain_events}, enviado como
 * {@code messageId} pelo outbox: é a chave de deduplicação.
 */
public record OrderEventMessage(String eventId,
                                String orderId,
                                long version,
                                String eventType,
                                LocalDateTime occurredAt,
                                JsonNode payload) {
}
