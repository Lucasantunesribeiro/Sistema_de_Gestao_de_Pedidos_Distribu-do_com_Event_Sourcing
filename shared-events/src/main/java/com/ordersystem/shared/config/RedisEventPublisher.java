package com.ordersystem.shared.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "messaging.type", havingValue = "redis", matchIfMissing = false)
public class RedisEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisEventPublisher.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final StreamOperations<String, Object, Object> streamOps;
    
    public RedisEventPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.streamOps = redisTemplate.opsForStream();
    }
    
    public void publishEvent(String stream, String eventType, Object event) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("eventType", eventType);
            eventData.put("payload", event);
            eventData.put("timestamp", System.currentTimeMillis());
            
            var messageId = streamOps.add(stream, eventData);
            logger.info("Event published to stream {}: {} with ID {}", stream, eventType, messageId.getValue());
        } catch (Exception e) {
            logger.error("Failed to publish event to stream {}: {}", stream, eventType, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
    
    public void publishOrderEvent(Object event) {
        publishEvent("order-events", event.getClass().getSimpleName(), event);
    }
    
    public void publishPaymentEvent(Object event) {
        publishEvent("payment-events", event.getClass().getSimpleName(), event);
    }
    
    public void publishInventoryEvent(Object event) {
        publishEvent("inventory-events", event.getClass().getSimpleName(), event);
    }
}