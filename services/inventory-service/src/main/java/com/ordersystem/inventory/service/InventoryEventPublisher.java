package com.ordersystem.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.shared.events.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service for publishing inventory-related events with resilience patterns
 */
@Service
public class InventoryEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryEventPublisher.class);
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${rabbitmq.exchange.inventory:inventory.exchange}")
    private String inventoryExchange;
    
    @Value("${rabbitmq.routing.inventory.reserved:inventory.reserved}")
    private String inventoryReservedRoutingKey;
    
    @Value("${rabbitmq.routing.inventory.failed:inventory.failed}")
    private String inventoryFailedRoutingKey;
    
    @Value("${rabbitmq.routing.inventory.updated:inventory.updated}")
    private String inventoryUpdatedRoutingKey;
    
    /**
     * Publish inventory reserved event
     */
    @CircuitBreaker(name = "inventory-publisher", fallbackMethod = "fallbackPublishReserved")
    @Retry(name = "inventory-publisher")
    public void publishInventoryReserved(String orderId, String customerId, List<OrderItem> items, String reservationId) {
        try {
            InventoryReservedEvent event = new InventoryReservedEvent(
                orderId, customerId, items, reservationId, Instant.now().toString()
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            rabbitTemplate.convertAndSend(
                inventoryExchange, 
                inventoryReservedRoutingKey, 
                eventJson
            );
            
            logger.info("Published InventoryReservedEvent for order {} with reservation {}", 
                orderId, reservationId);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize InventoryReservedEvent for order {}", orderId, e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }
    
    /**
     * Publish inventory reservation failed event
     */
    @CircuitBreaker(name = "inventory-publisher", fallbackMethod = "fallbackPublishFailed")
    @Retry(name = "inventory-publisher")
    public void publishInventoryReservationFailed(String orderId, String customerId, 
                                                 List<OrderItem> items, String reason) {
        try {
            InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(
                orderId, customerId, items, reason, Instant.now().toString()
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            rabbitTemplate.convertAndSend(
                inventoryExchange, 
                inventoryFailedRoutingKey, 
                eventJson
            );
            
            logger.info("Published InventoryReservationFailedEvent for order {} - reason: {}", 
                orderId, reason);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize InventoryReservationFailedEvent for order {}", orderId, e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }
    
    /**
     * Publish inventory updated event (for stock level changes)
     */
    @CircuitBreaker(name = "inventory-publisher", fallbackMethod = "fallbackPublishUpdated")
    @Retry(name = "inventory-publisher")
    public void publishInventoryUpdated(String productId, int previousQuantity, int newQuantity, 
                                      String reason, InventoryStatus status) {
        try {
            InventoryUpdatedEvent event = new InventoryUpdatedEvent(
                productId, previousQuantity, newQuantity, reason, status, Instant.now().toString()
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            rabbitTemplate.convertAndSend(
                inventoryExchange, 
                inventoryUpdatedRoutingKey, 
                eventJson
            );
            
            logger.info("Published InventoryUpdatedEvent for product {} - quantity changed from {} to {}", 
                productId, previousQuantity, newQuantity);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize InventoryUpdatedEvent for product {}", productId, e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }
    
    // Fallback methods for circuit breaker
    
    public void fallbackPublishReserved(String orderId, String customerId, List<OrderItem> items, 
                                       String reservationId, Exception ex) {
        logger.error("Fallback: Failed to publish InventoryReservedEvent for order {} - will retry later", 
            orderId, ex);
        // TODO: Store in dead letter queue or retry mechanism
    }
    
    public void fallbackPublishFailed(String orderId, String customerId, List<OrderItem> items, 
                                     String reason, Exception ex) {
        logger.error("Fallback: Failed to publish InventoryReservationFailedEvent for order {} - will retry later", 
            orderId, ex);
        // TODO: Store in dead letter queue or retry mechanism
    }
    
    public void fallbackPublishUpdated(String productId, int previousQuantity, int newQuantity, 
                                     String reason, InventoryStatus status, Exception ex) {
        logger.error("Fallback: Failed to publish InventoryUpdatedEvent for product {} - will retry later", 
            productId, ex);
        // TODO: Store in dead letter queue or retry mechanism
    }
}