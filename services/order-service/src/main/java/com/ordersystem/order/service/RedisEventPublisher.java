package com.ordersystem.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;

/**
 * Redis Event Publisher for Order Service
 * Publishes events to Redis Streams for consumption by query services
 */
@Service
public class RedisEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RedisEventPublisher.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Redis Stream names - must match what query-service expects
    private static final String ORDER_EVENTS_STREAM = "order-events";

    /**
     * Publishes OrderCreatedEvent to Redis Stream
     */
    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        String correlationId = event.getCorrelationId();
        
        logger.info("🚀 Publishing OrderCreatedEvent to Redis Stream: orderId={}, customerId={}, totalAmount={}, stream={}, correlationId={}",
                event.getOrderId(), event.getCustomerId(), event.getTotalAmount(), ORDER_EVENTS_STREAM, correlationId);

        try {
            // Convert event to JSON string for Redis
            String eventJson = objectMapper.writeValueAsString(event);
            
            // Add event to Redis Stream with event type and data
            redisTemplate.opsForStream().add(ORDER_EVENTS_STREAM, 
                java.util.Map.of(
                    "eventType", "ORDER_CREATED",
                    "orderId", event.getOrderId(),
                    "customerId", event.getCustomerId(),
                    "correlationId", correlationId != null ? correlationId : "",
                    "eventData", eventJson
                ));

            logger.info("✅ Successfully published OrderCreatedEvent to Redis Stream: orderId={}, stream={}, correlationId={}",
                    event.getOrderId(), ORDER_EVENTS_STREAM, correlationId);

        } catch (Exception e) {
            logger.error("❌ Error publishing OrderCreatedEvent to Redis Stream: orderId={}, stream={}, error={}, correlationId={}",
                    event.getOrderId(), ORDER_EVENTS_STREAM, e.getMessage(), correlationId, e);
            throw new RuntimeException("Failed to publish order created event to Redis: " + e.getMessage(), e);
        }
    }

    /**
     * Publishes OrderStatusUpdatedEvent to Redis Stream
     */
    public void publishOrderStatusUpdatedEvent(OrderStatusUpdatedEvent event) {
        String correlationId = event.getCorrelationId();
        
        logger.info("🚀 Publishing OrderStatusUpdatedEvent to Redis Stream: orderId={}, {} -> {}, stream={}, correlationId={}",
                event.getOrderId(), event.getOldStatus(), event.getNewStatus(), ORDER_EVENTS_STREAM, correlationId);

        try {
            // Convert event to JSON string for Redis
            String eventJson = objectMapper.writeValueAsString(event);
            
            // Add event to Redis Stream with event type and data
            redisTemplate.opsForStream().add(ORDER_EVENTS_STREAM, 
                java.util.Map.of(
                    "eventType", "ORDER_STATUS_UPDATED",
                    "orderId", event.getOrderId(),
                    "customerId", event.getCustomerId(),
                    "oldStatus", event.getOldStatus(),
                    "newStatus", event.getNewStatus(),
                    "correlationId", correlationId != null ? correlationId : "",
                    "eventData", eventJson
                ));

            logger.info("✅ Successfully published OrderStatusUpdatedEvent to Redis Stream: orderId={}, {} -> {}, stream={}, correlationId={}",
                    event.getOrderId(), event.getOldStatus(), event.getNewStatus(), ORDER_EVENTS_STREAM, correlationId);

        } catch (Exception e) {
            logger.error("❌ Error publishing OrderStatusUpdatedEvent to Redis Stream: orderId={}, {} -> {}, stream={}, error={}, correlationId={}",
                    event.getOrderId(), event.getOldStatus(), event.getNewStatus(), ORDER_EVENTS_STREAM, e.getMessage(), correlationId, e);
            throw new RuntimeException("Failed to publish order status updated event to Redis: " + e.getMessage(), e);
        }
    }
}