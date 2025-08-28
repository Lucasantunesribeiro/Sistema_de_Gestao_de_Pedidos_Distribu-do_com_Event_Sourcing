package com.ordersystem.query.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordersystem.query.entity.OrderItemReadModel;
import com.ordersystem.query.entity.OrderReadModel;
import com.ordersystem.query.repository.OrderReadModelRepository;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderItem;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;

@Service
public class OrderQueryService {

    private static final Logger logger = LoggerFactory.getLogger(OrderQueryService.class);

    @Autowired
    private OrderReadModelRepository orderReadModelRepository;

    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    @Autowired
    private CacheManager cacheManager;

    private void validateOrderCreatedEvent(OrderCreatedEvent event, String correlationId) {
        if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
            logger.error("❌ Invalid OrderCreatedEvent: orderId is null or empty, correlationId={}", correlationId);
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        if (event.getCustomerId() == null || event.getCustomerId().trim().isEmpty()) {
            logger.warn("⚠️ OrderCreatedEvent has null/empty customerId, using default: orderId={}, correlationId={}",
                    event.getOrderId(), correlationId);
            // Don't throw - we can handle missing customer ID by using a default
        }

        if (event.getTotalAmount() == null) {
            logger.error("❌ Invalid OrderCreatedEvent: totalAmount is null, orderId={}, correlationId={}",
                    event.getOrderId(), correlationId);
            throw new IllegalArgumentException("Total amount cannot be null");
        }

        if (event.getTotalAmount().doubleValue() < 0) {
            logger.warn(
                    "⚠️ OrderCreatedEvent has negative totalAmount, setting to 0: orderId={}, amount={}, correlationId={}",
                    event.getOrderId(), event.getTotalAmount(), correlationId);
            // Don't throw - we can handle negative amounts by setting to 0
        }

        if (event.getTimestamp() == null) {
            logger.warn("⚠️ OrderCreatedEvent has null timestamp, using current time: orderId={}, correlationId={}",
                    event.getOrderId(), correlationId);
            // Don't throw - we can handle missing timestamp by using current time
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCreated(OrderCreatedEvent event) {
        String correlationId = event.getCorrelationId() != null ? event.getCorrelationId() : "N/A";

        // Set correlation ID in MDC for request tracing
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", event.getOrderId());

        // Validate event data
        validateOrderCreatedEvent(event, correlationId);

        logger.info(
                "🔧 Processing OrderCreatedEvent in read model: orderId={}, customerId={}, totalAmount={}, itemCount={}, correlationId={}",
                event.getOrderId(), event.getCustomerId(), event.getTotalAmount(),
                event.getItems() != null ? event.getItems().size() : 0, correlationId);

        try {
            // Check if order already exists to prevent duplicates
            if (orderReadModelRepository.existsById(event.getOrderId())) {
                logger.warn("⚠️ Order already exists in read model, skipping creation: orderId={}, correlationId={}",
                        event.getOrderId(), correlationId);
                return; // Skip processing duplicate events
            }

            logger.debug("📝 Creating OrderReadModel: orderId={}, correlationId={}", event.getOrderId(), correlationId);

            // Handle missing or invalid data gracefully
            String customerId = (event.getCustomerId() != null && !event.getCustomerId().trim().isEmpty())
                    ? event.getCustomerId()
                    : "UNKNOWN_CUSTOMER";

            double totalAmount = event.getTotalAmount() != null
                    ? Math.max(0.0, event.getTotalAmount().doubleValue())
                    : 0.0;

            LocalDateTime timestamp = event.getTimestamp() != null
                    ? event.getTimestamp()
                    : LocalDateTime.now();

            OrderReadModel orderReadModel = new OrderReadModel(
                    event.getOrderId(),
                    customerId,
                    "PENDING",
                    totalAmount,
                    timestamp);

            // Process order items if they exist
            if (event.getItems() != null && !event.getItems().isEmpty()) {
                logger.debug("📦 Processing {} order items: orderId={}, correlationId={}",
                        event.getItems().size(), event.getOrderId(), correlationId);

                for (OrderItem item : event.getItems()) {
                    if (item == null) {
                        logger.warn("⚠️ Skipping null order item: orderId={}, correlationId={}",
                                event.getOrderId(), correlationId);
                        continue;
                    }

                    if (item.getProductId() == null || item.getProductId().trim().isEmpty()) {
                        logger.warn("⚠️ Skipping order item with null/empty productId: orderId={}, correlationId={}",
                                event.getOrderId(), correlationId);
                        continue;
                    }

                    OrderItemReadModel orderItem = new OrderItemReadModel(
                            item.getProductId(),
                            item.getProductName() != null ? item.getProductName() : "Unknown Product",
                            item.getQuantity() != null ? item.getQuantity() : 1,
                            item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0,
                            orderReadModel);
                    orderReadModel.getItems().add(orderItem);

                    logger.debug("📦 Added item to read model: orderId={}, productId={}, quantity={}, correlationId={}",
                            event.getOrderId(), item.getProductId(), item.getQuantity(), correlationId);
                }
            } else {
                logger.warn("⚠️ OrderCreatedEvent has no items: orderId={}, correlationId={}",
                        event.getOrderId(), correlationId);
            }

            logger.debug("💾 Saving OrderReadModel to database: orderId={}, correlationId={}",
                    event.getOrderId(), correlationId);

            orderReadModelRepository.save(orderReadModel);

            logger.info(
                    "✅ Order read model created successfully (CQRS projection): orderId={}, customerId={}, itemCount={}, correlationId={}",
                    event.getOrderId(), event.getCustomerId(), orderReadModel.getItems().size(), correlationId);

        } catch (Exception e) {
            logger.error("❌ Failed to create order read model: orderId={}, customerId={}, error={}, correlationId={}",
                    event.getOrderId(), event.getCustomerId(), e.getMessage(), correlationId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        String correlationId = event.getCorrelationId() != null ? event.getCorrelationId() : "N/A";

        // Set correlation ID in MDC for request tracing
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", event.getOrderId());

        // Validate event data
        if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
            logger.error("❌ Invalid OrderStatusUpdatedEvent: orderId is null or empty, correlationId={}",
                    correlationId);
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        if (event.getNewStatus() == null || event.getNewStatus().trim().isEmpty()) {
            logger.warn(
                    "⚠️ OrderStatusUpdatedEvent has null/empty newStatus, skipping update: orderId={}, correlationId={}",
                    event.getOrderId(), correlationId);
            return; // Skip processing if new status is invalid
        }

        logger.info("🔧 Processing OrderStatusUpdatedEvent in read model: orderId={}, {} -> {}, correlationId={}",
                event.getOrderId(), event.getOldStatus(), event.getNewStatus(), correlationId);

        try {
            logger.debug("🔍 Looking up order in read model: orderId={}, correlationId={}", event.getOrderId(),
                    correlationId);

            Optional<OrderReadModel> orderOpt = orderReadModelRepository.findById(event.getOrderId());
            if (orderOpt.isPresent()) {
                OrderReadModel order = orderOpt.get();
                String previousStatus = order.getStatus();

                logger.debug(
                        "📝 Updating order status in read model: orderId={}, currentStatus={}, newStatus={}, correlationId={}",
                        event.getOrderId(), previousStatus, event.getNewStatus(), correlationId);

                order.setStatus(event.getNewStatus());
                order.setLastUpdated(LocalDateTime.now());

                logger.debug("💾 Saving updated order to database: orderId={}, correlationId={}", event.getOrderId(),
                        correlationId);
                orderReadModelRepository.save(order);

                // Invalidate cache after updating the order
                logger.debug("🗑️ Invalidating cache for order: orderId={}, correlationId={}", event.getOrderId(),
                        correlationId);
                cacheInvalidationService.handleOrderStatusUpdated(event);

                logger.info(
                        "✅ Order status updated successfully in read model (CQRS projection): orderId={}, {} -> {}, correlationId={}",
                        event.getOrderId(), previousStatus, event.getNewStatus(), correlationId);
            } else {
                logger.warn("⚠️ Order not found in read model for status update: orderId={}, correlationId={}",
                        event.getOrderId(), correlationId);
            }

        } catch (Exception e) {
            logger.error(
                    "❌ Failed to update order status in read model: orderId={}, {} -> {}, error={}, correlationId={}",
                    event.getOrderId(), event.getOldStatus(), event.getNewStatus(), e.getMessage(), correlationId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        String correlationId = "N/A"; // PaymentProcessedEvent might not have correlationId

        // Set correlation ID in MDC for request tracing
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", event.getOrderId());

        // Validate event data
        if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
            logger.error("❌ Invalid PaymentProcessedEvent: orderId is null or empty, correlationId={}", correlationId);
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        if (event.getPaymentId() == null || event.getPaymentId().trim().isEmpty()) {
            logger.error("❌ Invalid PaymentProcessedEvent: paymentId is null or empty, orderId={}, correlationId={}",
                    event.getOrderId(), correlationId);
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }

        if (event.getPaymentStatus() == null || event.getPaymentStatus().trim().isEmpty()) {
            logger.error(
                    "❌ Invalid PaymentProcessedEvent: paymentStatus is null or empty, orderId={}, correlationId={}",
                    event.getOrderId(), correlationId);
            throw new IllegalArgumentException("Payment status cannot be null or empty");
        }

        logger.info(
                "🔧 Processing PaymentProcessedEvent in read model: orderId={}, paymentId={}, status={}, amount={}, correlationId={}",
                event.getOrderId(), event.getPaymentId(), event.getPaymentStatus(), event.getAmount(), correlationId);

        try {
            logger.debug("🔍 Looking up order in read model: orderId={}, correlationId={}", event.getOrderId(),
                    correlationId);

            Optional<OrderReadModel> orderOpt = orderReadModelRepository.findById(event.getOrderId());
            if (orderOpt.isPresent()) {
                OrderReadModel order = orderOpt.get();
                String previousStatus = order.getStatus();
                String previousPaymentStatus = order.getPaymentStatus();

                logger.debug(
                        "💳 Updating payment info in read model: orderId={}, paymentId={}, previousPaymentStatus={}, newPaymentStatus={}, correlationId={}",
                        event.getOrderId(), event.getPaymentId(), previousPaymentStatus, event.getPaymentStatus(),
                        correlationId);

                order.setPaymentId(event.getPaymentId());
                order.setPaymentStatus(event.getPaymentStatus());
                order.setLastUpdated(LocalDateTime.now());

                // Update order status based on payment result
                String newOrderStatus;
                if ("APPROVED".equals(event.getPaymentStatus())) {
                    newOrderStatus = "PAID";
                    order.setStatus(newOrderStatus);
                    logger.debug("✅ Payment approved, updating order status: orderId={}, {} -> {}, correlationId={}",
                            event.getOrderId(), previousStatus, newOrderStatus, correlationId);
                } else {
                    newOrderStatus = "CANCELLED";
                    order.setStatus(newOrderStatus);
                    logger.debug("❌ Payment failed, cancelling order: orderId={}, {} -> {}, correlationId={}",
                            event.getOrderId(), previousStatus, newOrderStatus, correlationId);
                }

                logger.debug("💾 Saving updated order with payment info: orderId={}, correlationId={}",
                        event.getOrderId(), correlationId);
                orderReadModelRepository.save(order);

                // Invalidate cache after processing payment
                logger.debug("🗑️ Invalidating cache after payment processing: orderId={}, correlationId={}",
                        event.getOrderId(), correlationId);
                cacheInvalidationService.handlePaymentProcessed(event);

                logger.info(
                        "✅ Payment processed successfully in read model (CQRS projection): orderId={}, paymentStatus={}, orderStatus={} -> {}, correlationId={}",
                        event.getOrderId(), event.getPaymentStatus(), previousStatus, newOrderStatus, correlationId);
            } else {
                logger.warn(
                        "⚠️ Order not found in read model for payment processing: orderId={}, paymentId={}, correlationId={}",
                        event.getOrderId(), event.getPaymentId(), correlationId);
            }

        } catch (Exception e) {
            logger.error(
                    "❌ Failed to process payment in read model: orderId={}, paymentId={}, status={}, error={}, correlationId={}",
                    event.getOrderId(), event.getPaymentId(), event.getPaymentStatus(), e.getMessage(), correlationId,
                    e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @Cacheable(value = "orders", key = "'findAllOrderByCreatedAtDesc'")
    public List<OrderReadModel> getAllOrders() {
        return orderReadModelRepository.findAllOrderByCreatedAtDesc();
    }

    @Cacheable(value = "single-order", key = "#orderId")
    public Optional<OrderReadModel> getOrderById(String orderId) {
        return orderReadModelRepository.findById(orderId);
    }

    @Cacheable(value = "customer-orders", key = "#customerId")
    public List<OrderReadModel> getOrdersByCustomerId(String customerId) {
        return orderReadModelRepository.findByCustomerId(customerId);
    }

    @Cacheable(value = "status-orders", key = "#status")
    public List<OrderReadModel> getOrdersByStatus(String status) {
        return orderReadModelRepository.findByStatus(status);
    }

    @Cacheable(value = "customer-orders", key = "#customerId + '::' + #status")
    public List<OrderReadModel> getOrdersByCustomerIdAndStatus(String customerId, String status) {
        return orderReadModelRepository.findByCustomerIdAndStatus(customerId, status);
    }

    // New methods for optimized performance and pagination

    @Cacheable(value = "orders-paged", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<OrderReadModel> getAllOrdersPaged(Pageable pageable) {
        logger.debug("🔍 Getting paginated orders: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        return orderReadModelRepository.findAll(pageable);
    }

    @Cacheable(value = "order-count", key = "'total-count'")
    public long getOrderCount() {
        logger.debug("📊 Getting total order count for health check");
        try {
            return orderReadModelRepository.count();
        } catch (Exception e) {
            logger.error("❌ Failed to get order count: {}", e.getMessage());
            return -1; // Indicate failure
        }
    }

    public boolean isCacheHealthy() {
        try {
            // Check if cache manager is available and healthy
            if (cacheManager == null) {
                return false;
            }
            
            // Try to access a cache - this will fail if cache is unhealthy
            cacheManager.getCache("orders");
            return true;
            
        } catch (Exception e) {
            logger.warn("⚠️ Cache health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Cacheable(value = "dashboard-metrics", key = "'metrics'")
    public Map<String, Object> getDashboardMetricsOptimized() {
        logger.debug("📊 Calculating optimized dashboard metrics");
        
        try {
            // Use aggregate queries instead of loading all data
            long totalOrders = orderReadModelRepository.count();
            
            if (totalOrders == 0) {
                return Map.of(
                    "totalOrders", 0L,
                    "totalRevenue", 0.0,
                    "completedOrders", 0L,
                    "pendingOrders", 0L,
                    "cancelledOrders", 0L,
                    "averageOrderValue", 0.0
                );
            }

            // Use native queries or repository methods for aggregation
            Double totalRevenue = orderReadModelRepository.getTotalRevenue();
            Long completedOrders = orderReadModelRepository.countByStatusIn(List.of("COMPLETED", "PAID"));
            Long pendingOrders = orderReadModelRepository.countByStatusIn(List.of("PENDING", "PROCESSING"));
            Long cancelledOrders = orderReadModelRepository.countByStatus("CANCELLED");
            
            double averageOrderValue = totalRevenue != null && totalOrders > 0 ? 
                totalRevenue / totalOrders : 0.0;

            return Map.of(
                "totalOrders", totalOrders,
                "totalRevenue", Math.round((totalRevenue != null ? totalRevenue : 0.0) * 100.0) / 100.0,
                "completedOrders", completedOrders != null ? completedOrders : 0L,
                "pendingOrders", pendingOrders != null ? pendingOrders : 0L,
                "cancelledOrders", cancelledOrders != null ? cancelledOrders : 0L,
                "averageOrderValue", Math.round(averageOrderValue * 100.0) / 100.0
            );

        } catch (Exception e) {
            logger.error("❌ Failed to calculate dashboard metrics: {}", e.getMessage());
            // Return empty metrics on failure
            return Map.of(
                "totalOrders", 0L,
                "totalRevenue", 0.0,
                "completedOrders", 0L,
                "pendingOrders", 0L,
                "cancelledOrders", 0L,
                "averageOrderValue", 0.0,
                "error", "Failed to calculate metrics"
            );
        }
    }
}