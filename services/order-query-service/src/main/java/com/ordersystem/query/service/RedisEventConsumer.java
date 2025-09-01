package com.ordersystem.query.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;

/**
 * Redis Event Consumer for Order Query Service
 * Polls Redis Streams for order events using scheduled polling
 * 
 * TEMPORARILY DISABLED for H2 validation phase
 */
// @Service // DISABLED: Requires RedisTemplate which is not available during H2
// testing
public class RedisEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RedisEventConsumer.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private ObjectMapper objectMapper;

    // Stream and consumer group names
    private static final String ORDER_EVENTS_STREAM = "order-events";
    private static final String CONSUMER_GROUP = "order-query-service-group";
    private static final String CONSUMER_NAME = "order-query-service-consumer";

    /**
     * Polls Redis Stream for new events every 2 seconds
     */
    @Scheduled(fixedDelay = 2000)
    public void pollForEvents() {
        try {
            // Create consumer group if it doesn't exist (ignore error if it already exists)
            try {
                redisTemplate.opsForStream().createGroup(ORDER_EVENTS_STREAM, CONSUMER_GROUP);
                logger.debug("✅ Created consumer group: stream={}, group={}", ORDER_EVENTS_STREAM, CONSUMER_GROUP);
            } catch (Exception e) {
                // Group probably already exists, which is fine
                logger.debug("Consumer group already exists or stream doesn't exist yet: {}", e.getMessage());
            }

            // Read from stream
            List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream().read(
                    Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                    StreamReadOptions.empty().count(10).block(Duration.ofSeconds(1)),
                    StreamOffset.create(ORDER_EVENTS_STREAM, ReadOffset.lastConsumed()));

            if (messages != null && !messages.isEmpty()) {
                logger.info("📨 Received {} messages from Redis Stream: {}", messages.size(), ORDER_EVENTS_STREAM);

                for (MapRecord<String, Object, Object> message : messages) {
                    processMessage(message);

                    // Acknowledge the message
                    redisTemplate.opsForStream().acknowledge(ORDER_EVENTS_STREAM, CONSUMER_GROUP, message.getId());
                }
            }

        } catch (Exception e) {
            logger.error("❌ Error polling Redis Stream: stream={}, error={}", ORDER_EVENTS_STREAM, e.getMessage(), e);
        }
    }

    private void processMessage(MapRecord<String, Object, Object> message) {
        String correlationId = "N/A";
        String eventType = "UNKNOWN";
        String orderId = "N/A";

        try {
            // Extract message data
            Map<Object, Object> messageBody = message.getValue();
            eventType = (String) messageBody.get("eventType");
            orderId = (String) messageBody.get("orderId");
            correlationId = (String) messageBody.getOrDefault("correlationId", "N/A");

            // Set correlation ID in MDC for request tracing
            MDC.put("correlationId", correlationId);
            MDC.put("orderId", orderId);
            MDC.put("eventType", eventType);

            logger.info("🔧 Processing Redis Stream event: eventType={}, orderId={}, correlationId={}, streamId={}",
                    eventType, orderId, correlationId, message.getId());

            // Process based on event type
            switch (eventType) {
                case "ORDER_CREATED":
                    handleOrderCreatedEvent(messageBody, correlationId);
                    break;
                case "ORDER_STATUS_UPDATED":
                    handleOrderStatusUpdatedEvent(messageBody, correlationId);
                    break;
                default:
                    logger.warn("⚠️ Unknown event type received: eventType={}, orderId={}, correlationId={}",
                            eventType, orderId, correlationId);
                    break;
            }

            logger.info("✅ Successfully processed Redis Stream event: eventType={}, orderId={}, correlationId={}",
                    eventType, orderId, correlationId);

        } catch (Exception e) {
            logger.error("❌ Failed to process Redis Stream event: eventType={}, orderId={}, error={}, correlationId={}",
                    eventType, orderId, e.getMessage(), correlationId, e);
            // Don't re-throw - we'll acknowledge the message anyway to avoid infinite
            // retries
        } finally {
            MDC.clear();
        }
    }

    private void handleOrderCreatedEvent(Map<Object, Object> messageBody, String correlationId) {
        try {
            String eventData = (String) messageBody.get("eventData");
            OrderCreatedEvent event = objectMapper.readValue(eventData, OrderCreatedEvent.class);

            logger.info(
                    "🔧 Processing OrderCreatedEvent from Redis: orderId={}, customerId={}, totalAmount={}, itemCount={}, correlationId={}",
                    event.getOrderId(), event.getCustomerId(), event.getTotalAmount(),
                    event.getItems() != null ? event.getItems().size() : 0, correlationId);

            // Validate event before processing
            if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
                logger.error("❌ Received OrderCreatedEvent with null/empty orderId, correlationId={}", correlationId);
                return; // Don't retry invalid events
            }

            long startTime = System.currentTimeMillis();
            orderQueryService.handleOrderCreated(event);
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info(
                    "✅ Successfully processed OrderCreatedEvent from Redis: orderId={}, processingTime={}ms, correlationId={}",
                    event.getOrderId(), processingTime, correlationId);

        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid OrderCreatedEvent data from Redis: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);
        } catch (Exception e) {
            logger.error("❌ Failed to process OrderCreatedEvent from Redis: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);
        }
    }

    private void handleOrderStatusUpdatedEvent(Map<Object, Object> messageBody, String correlationId) {
        try {
            String eventData = (String) messageBody.get("eventData");
            OrderStatusUpdatedEvent event = objectMapper.readValue(eventData, OrderStatusUpdatedEvent.class);

            logger.info(
                    "🔧 Processing OrderStatusUpdatedEvent from Redis: orderId={}, customerId={}, {} -> {}, correlationId={}",
                    event.getOrderId(), event.getCustomerId(), event.getOldStatus(), event.getNewStatus(),
                    correlationId);

            // Validate event before processing
            if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
                logger.error("❌ Received OrderStatusUpdatedEvent with null/empty orderId, correlationId={}",
                        correlationId);
                return; // Don't retry invalid events
            }

            long startTime = System.currentTimeMillis();
            orderQueryService.handleOrderStatusUpdated(event);
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info(
                    "✅ Successfully processed OrderStatusUpdatedEvent from Redis: orderId={}, {} -> {}, processingTime={}ms, correlationId={}",
                    event.getOrderId(), event.getOldStatus(), event.getNewStatus(), processingTime, correlationId);

        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid OrderStatusUpdatedEvent data from Redis: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);
        } catch (Exception e) {
            logger.error("❌ Failed to process OrderStatusUpdatedEvent from Redis: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);
        }
    }
}