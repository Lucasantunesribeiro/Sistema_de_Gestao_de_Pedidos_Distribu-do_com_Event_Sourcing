package com.ordersystem.inventory.service;

import com.ordersystem.common.messaging.MessagingConstants;
import com.ordersystem.shared.events.InventoryReservationFailedEvent;
import com.ordersystem.shared.events.InventoryReservedEvent;
import com.ordersystem.shared.events.InventoryStatus;
import com.ordersystem.shared.events.InventoryUpdatedEvent;
import com.ordersystem.shared.events.OrderItem;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class InventoryEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public InventoryEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @CircuitBreaker(name = "inventory-publisher", fallbackMethod = "fallbackPublishReserved")
    @Retry(name = "inventory-publisher")
    public void publishInventoryReserved(String orderId, String customerId, List<OrderItem> items, String reservationId) {
        InventoryReservedEvent event = new InventoryReservedEvent(
                orderId, customerId, items, reservationId, Instant.now().toString()
        );

        rabbitTemplate.convertAndSend(
                MessagingConstants.INVENTORY_EXCHANGE,
                MessagingConstants.INVENTORY_RESERVED_ROUTING_KEY,
                event
        );

        logger.info("Published InventoryReservedEvent for order {} with reservation {}", orderId, reservationId);
    }

    @CircuitBreaker(name = "inventory-publisher", fallbackMethod = "fallbackPublishFailed")
    @Retry(name = "inventory-publisher")
    public void publishInventoryReservationFailed(String orderId, String customerId,
                                                  List<OrderItem> items, String reason) {
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(
                orderId, customerId, items, reason
        );

        rabbitTemplate.convertAndSend(
                MessagingConstants.INVENTORY_EXCHANGE,
                MessagingConstants.INVENTORY_FAILED_ROUTING_KEY,
                event
        );

        logger.info("Published InventoryReservationFailedEvent for order {} - reason: {}", orderId, reason);
    }

    @CircuitBreaker(name = "inventory-publisher", fallbackMethod = "fallbackPublishUpdated")
    @Retry(name = "inventory-publisher")
    public void publishInventoryUpdated(String productId, int previousQuantity, int newQuantity,
                                        String reason, InventoryStatus status) {
        InventoryUpdatedEvent event = new InventoryUpdatedEvent(
                productId, previousQuantity, newQuantity, reason, status, Instant.now().toString()
        );

        rabbitTemplate.convertAndSend(
                MessagingConstants.INVENTORY_EXCHANGE,
                MessagingConstants.INVENTORY_UPDATED_ROUTING_KEY,
                event
        );

        logger.info("Published InventoryUpdatedEvent for product {} - quantity changed from {} to {}",
                productId, previousQuantity, newQuantity);
    }

    public void fallbackPublishReserved(String orderId, String customerId, List<OrderItem> items,
                                        String reservationId, Exception ex) {
        logger.error("Fallback: Failed to publish InventoryReservedEvent for order {}", orderId, ex);
        throw new IllegalStateException("Inventory publish fallback", ex);
    }

    public void fallbackPublishFailed(String orderId, String customerId, List<OrderItem> items,
                                      String reason, Exception ex) {
        logger.error("Fallback: Failed to publish InventoryReservationFailedEvent for order {}", orderId, ex);
        throw new IllegalStateException("Inventory publish fallback", ex);
    }

    public void fallbackPublishUpdated(String productId, int previousQuantity, int newQuantity,
                                       String reason, InventoryStatus status, Exception ex) {
        logger.error("Fallback: Failed to publish InventoryUpdatedEvent for product {}", productId, ex);
        throw new IllegalStateException("Inventory publish fallback", ex);
    }
}
