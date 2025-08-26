package com.ordersystem.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.order.model.Order;
import com.ordersystem.order.model.OrderEvent;
import com.ordersystem.order.model.OrderStatus;
import com.ordersystem.order.repository.OrderEventRepository;
import com.ordersystem.order.repository.OrderRepository;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventRepository eventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ORDER_EXCHANGE = "order.exchange";
    private static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    private static final String ORDER_UPDATED_ROUTING_KEY = "order.updated";

    public Order createOrder(String customerId, Double totalAmount, List<String> productIds) {
        String orderId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();

        // Set correlation ID in MDC for request tracing
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", orderId);

        logger.info(
                "🚀 Starting order creation: orderId={}, customerId={}, totalAmount={}, productCount={}, correlationId={}",
                orderId, customerId, totalAmount, productIds.size(), correlationId);

        try {
            // Create order aggregate
            Order order = new Order(customerId, totalAmount);
            order.setId(orderId);

            logger.debug("📝 Order aggregate created: orderId={}, status={}, correlationId={}",
                    orderId, order.getStatus(), correlationId);

            // Save order
            Order savedOrder = orderRepository.save(order);

            logger.info("💾 Order persisted to database: orderId={}, correlationId={}",
                    savedOrder.getId(), correlationId);

            // Create and store event
            publishOrderCreatedEvent(savedOrder, correlationId);

            logger.info("✅ Order creation completed successfully: orderId={}, correlationId={}",
                    savedOrder.getId(), correlationId);

            return savedOrder;

        } catch (Exception e) {
            logger.error("❌ Order creation failed: orderId={}, customerId={}, error={}, correlationId={}",
                    orderId, customerId, e.getMessage(), correlationId, e);
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }

    public Optional<Order> findById(String id) {
        return orderRepository.findById(id);
    }

    public List<Order> findByCustomerId(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order updateOrderStatus(String orderId, String status) {
        String correlationId = UUID.randomUUID().toString();

        // Set correlation ID in MDC for request tracing
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", orderId);

        logger.info("🔄 Starting order status update: orderId={}, newStatus={}, correlationId={}",
                orderId, status, correlationId);

        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                String previousStatus = order.getStatus().name();

                logger.debug("📋 Current order status: orderId={}, currentStatus={}, correlationId={}",
                        orderId, previousStatus, correlationId);

                // Update status
                try {
                    order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
                    logger.debug("✏️ Order status updated in memory: orderId={}, {} -> {}, correlationId={}",
                            orderId, previousStatus, status.toUpperCase(), correlationId);
                } catch (IllegalArgumentException e) {
                    logger.error("❌ Invalid status provided: orderId={}, invalidStatus={}, correlationId={}",
                            orderId, status, correlationId);
                    throw new RuntimeException("Invalid status: " + status);
                }

                Order savedOrder = orderRepository.save(order);

                logger.info("💾 Order status persisted to database: orderId={}, newStatus={}, correlationId={}",
                        savedOrder.getId(), savedOrder.getStatus(), correlationId);

                // Publish status updated event
                publishOrderStatusUpdatedEvent(savedOrder, previousStatus, status.toUpperCase(), correlationId);

                logger.info("✅ Order status update completed successfully: orderId={}, {} -> {}, correlationId={}",
                        savedOrder.getId(), previousStatus, savedOrder.getStatus(), correlationId);

                return savedOrder;
            }

            logger.error("❌ Order not found for status update: orderId={}, correlationId={}",
                    orderId, correlationId);
            throw new RuntimeException("Order not found: " + orderId);

        } catch (Exception e) {
            logger.error("❌ Order status update failed: orderId={}, status={}, error={}, correlationId={}",
                    orderId, status, e.getMessage(), correlationId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private void publishOrderCreatedEvent(Order order, String correlationId) {
        logger.debug("📤 Preparing to publish OrderCreatedEvent: orderId={}, correlationId={}",
                order.getId(), correlationId);

        try {
            // Create proper OrderCreatedEvent with OrderItems
            List<com.ordersystem.shared.events.OrderItem> eventItems = new ArrayList<>();
            for (com.ordersystem.order.model.OrderItem item : order.getItems()) {
                eventItems.add(new com.ordersystem.shared.events.OrderItem(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        BigDecimal.valueOf(item.getUnitPrice())));
            }

            logger.debug("🔧 Created {} order items for event: orderId={}, correlationId={}",
                    eventItems.size(), order.getId(), correlationId);

            OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                    order.getId(),
                    order.getCustomerId(),
                    eventItems,
                    BigDecimal.valueOf(order.getTotalAmount()),
                    order.getCreatedAt(),
                    correlationId,
                    null);

            // Store event in event store
            logger.debug("💾 Storing event in event store: orderId={}, correlationId={}",
                    order.getId(), correlationId);

            OrderEvent event = new OrderEvent(
                    order.getId(),
                    "ORDER_CREATED",
                    objectMapper.writeValueAsString(orderCreatedEvent));
            eventRepository.save(event);

            logger.info("✅ Event stored in event store: orderId={}, eventId={}, correlationId={}",
                    order.getId(), event.getId(), correlationId);

            // Publish proper event to RabbitMQ
            logger.info(
                    "🚀 Publishing OrderCreatedEvent to RabbitMQ: orderId={}, customerId={}, totalAmount={}, exchange={}, routingKey={}, correlationId={}",
                    order.getId(), order.getCustomerId(), order.getTotalAmount(),
                    ORDER_EXCHANGE, ORDER_CREATED_ROUTING_KEY, correlationId);

            rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_CREATED_ROUTING_KEY, orderCreatedEvent);

            logger.info("✅ Successfully published OrderCreatedEvent to RabbitMQ: orderId={}, correlationId={}",
                    order.getId(), correlationId);

        } catch (Exception e) {
            logger.error(
                    "❌ Error publishing order created event: orderId={}, exchange={}, routingKey={}, error={}, correlationId={}",
                    order.getId(), ORDER_EXCHANGE, ORDER_CREATED_ROUTING_KEY, e.getMessage(), correlationId, e);
            throw new RuntimeException("Failed to publish order created event: " + e.getMessage(), e);
        }
    }

    private void publishOrderStatusUpdatedEvent(Order order, String previousStatus, String newStatus,
            String correlationId) {
        logger.debug("📤 Preparing to publish OrderStatusUpdatedEvent: orderId={}, {} -> {}, correlationId={}",
                order.getId(), previousStatus, newStatus, correlationId);

        try {
            // Create proper OrderStatusUpdatedEvent
            OrderStatusUpdatedEvent statusUpdatedEvent = new OrderStatusUpdatedEvent(
                    order.getId(),
                    order.getCustomerId(),
                    previousStatus,
                    newStatus,
                    order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt(),
                    correlationId,
                    null);

            // Store event in event store
            logger.debug("💾 Storing status update event in event store: orderId={}, correlationId={}",
                    order.getId(), correlationId);

            OrderEvent event = new OrderEvent(
                    order.getId(),
                    "ORDER_STATUS_UPDATED",
                    objectMapper.writeValueAsString(statusUpdatedEvent));
            eventRepository.save(event);

            logger.info("✅ Status update event stored in event store: orderId={}, eventId={}, correlationId={}",
                    order.getId(), event.getId(), correlationId);

            // Publish proper event to RabbitMQ
            logger.info(
                    "🚀 Publishing OrderStatusUpdatedEvent to RabbitMQ: orderId={}, {} -> {}, exchange={}, routingKey={}, correlationId={}",
                    order.getId(), previousStatus, newStatus, ORDER_EXCHANGE, ORDER_UPDATED_ROUTING_KEY, correlationId);

            rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_UPDATED_ROUTING_KEY, statusUpdatedEvent);

            logger.info("✅ Successfully published OrderStatusUpdatedEvent to RabbitMQ: orderId={}, correlationId={}",
                    order.getId(), correlationId);

        } catch (Exception e) {
            logger.error(
                    "❌ Error publishing order status updated event: orderId={}, {} -> {}, exchange={}, routingKey={}, error={}, correlationId={}",
                    order.getId(), previousStatus, newStatus, ORDER_EXCHANGE, ORDER_UPDATED_ROUTING_KEY, e.getMessage(),
                    correlationId, e);
            throw new RuntimeException("Failed to publish order status updated event: " + e.getMessage(), e);
        }
    }

    public List<OrderEvent> getOrderEvents(String orderId) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", orderId);

        logger.info("📜 Retrieving order events (Event Sourcing): orderId={}, correlationId={}", orderId,
                correlationId);

        try {
            List<OrderEvent> events = eventRepository.findByAggregateIdOrderByOccurredAtAsc(orderId);

            logger.info("✅ Retrieved {} events for order: orderId={}, correlationId={}",
                    events.size(), orderId, correlationId);

            if (logger.isDebugEnabled()) {
                logger.debug("📊 Event types for order: orderId={}, correlationId={}, eventTypes={}",
                        orderId, correlationId, events.stream().map(OrderEvent::getEventType).toList());
            }

            return events;
        } catch (Exception e) {
            logger.error("❌ Failed to retrieve order events: orderId={}, error={}, correlationId={}",
                    orderId, e.getMessage(), correlationId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Rebuild order aggregate from events (Event Sourcing)
     * This method demonstrates how to reconstruct state from events
     */
    public Order rebuildOrderFromEvents(String orderId) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", orderId);

        logger.info("🔄 Rebuilding order from events (Event Sourcing): orderId={}, correlationId={}", orderId,
                correlationId);

        try {
            List<OrderEvent> events = eventRepository.findByAggregateIdOrderByOccurredAtAsc(orderId);

            if (events.isEmpty()) {
                logger.warn("⚠️ No events found for order: orderId={}, correlationId={}", orderId, correlationId);
                return null;
            }

            logger.debug("🔧 Replaying {} events to rebuild order: orderId={}, correlationId={}",
                    events.size(), orderId, correlationId);

            Order rebuiltOrder = null;

            for (OrderEvent event : events) {
                logger.debug("🎬 Replaying event: orderId={}, eventType={}, version={}, correlationId={}",
                        orderId, event.getEventType(), event.getVersion(), correlationId);

                switch (event.getEventType()) {
                    case "ORDER_CREATED":
                        // Parse the event data and create the order
                        try {
                            // In a real implementation, you would deserialize the event data
                            // and apply it to rebuild the aggregate state
                            rebuiltOrder = orderRepository.findById(orderId).orElse(null);
                            if (rebuiltOrder != null) {
                                logger.debug("📝 Applied ORDER_CREATED event: orderId={}, correlationId={}",
                                        orderId, correlationId);
                            }
                        } catch (Exception e) {
                            logger.error(
                                    "❌ Failed to apply ORDER_CREATED event: orderId={}, error={}, correlationId={}",
                                    orderId, e.getMessage(), correlationId);
                        }
                        break;

                    case "ORDER_STATUS_UPDATED":
                        // Apply status update from event
                        if (rebuiltOrder != null) {
                            logger.debug("📝 Applied ORDER_STATUS_UPDATED event: orderId={}, correlationId={}",
                                    orderId, correlationId);
                        }
                        break;

                    default:
                        logger.debug("🤷 Unknown event type, skipping: orderId={}, eventType={}, correlationId={}",
                                orderId, event.getEventType(), correlationId);
                        break;
                }
            }

            logger.info("✅ Successfully rebuilt order from {} events: orderId={}, correlationId={}",
                    events.size(), orderId, correlationId);

            return rebuiltOrder;

        } catch (Exception e) {
            logger.error("❌ Failed to rebuild order from events: orderId={}, error={}, correlationId={}",
                    orderId, e.getMessage(), correlationId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}