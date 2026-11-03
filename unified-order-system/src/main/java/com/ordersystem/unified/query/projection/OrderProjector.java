package com.ordersystem.unified.query.projection;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aplica um evento do stream Order à {@link OrderProjection}.
 *
 * <p>Função determinística de (estado atual, evento): versão {@code <=} à última aplicada é ignorada
 * (já refletida), versão com lacuna lança {@link OutOfOrderEventException}. Nenhum valor vem do relógio.
 */
@Component
public class OrderProjector {

    private static final Logger logger = LoggerFactory.getLogger(OrderProjector.class);

    private final OrderProjectionRepository repository;

    public OrderProjector(OrderProjectionRepository repository) {
        this.repository = repository;
    }

    /**
     * @return {@code true} se a projeção mudou
     */
    public boolean apply(OrderEventMessage event) {
        OrderProjection projection = repository.findById(event.orderId()).orElse(null);
        long lastVersion = projection == null ? 0 : projection.getLastEventVersion();

        if (event.version() <= lastVersion) {
            logger.debug("Stale event ignored by projection: order={}, v{} <= v{}",
                event.orderId(), event.version(), lastVersion);
            return false;
        }
        if (event.version() != lastVersion + 1) {
            throw new OutOfOrderEventException(String.format(
                "Order %s: projection at v%d cannot apply v%d", event.orderId(), lastVersion, event.version()));
        }

        JsonNode payload = event.payload();
        switch (event.eventType()) {
            case "OrderCreatedEvent" -> projection = new OrderProjection(
                event.orderId(),
                text(payload, "customerId"),
                text(payload, "customerName"),
                decimal(payload, "totalAmount"),
                "PENDING");
            case "OrderStatusUpdatedEvent" -> {
                requireCreated(projection, event);
                projection.setStatus(text(payload, "newStatus"));
            }
            case "OrderCancelledEvent" -> {
                requireCreated(projection, event);
                projection.setStatus("CANCELLED");
                projection.setCancellationReason(text(payload, "cancellationReason"));
            }
            default -> {
                requireCreated(projection, event);
                logger.info("Event type {} has no projection effect (version still advances)", event.eventType());
            }
        }
        projection.advance(event.version(), event.eventId(), event.occurredAt());
        repository.save(projection);
        return true;
    }

    private static void requireCreated(OrderProjection projection, OrderEventMessage event) {
        if (projection == null) {
            throw new OutOfOrderEventException(String.format(
                "Order %s: %s v%d arrived before OrderCreatedEvent", event.orderId(), event.eventType(),
                event.version()));
        }
    }

    private static String text(JsonNode payload, String field) {
        JsonNode node = payload == null ? null : payload.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static BigDecimal decimal(JsonNode payload, String field) {
        JsonNode node = payload == null ? null : payload.get(field);
        return node == null || node.isNull() ? null : node.decimalValue();
    }
}
