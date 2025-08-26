package com.ordersystem.order.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ordersystem.order.model.Order;
import com.ordersystem.order.model.OrderEvent;
import com.ordersystem.order.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> orderRequest) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("🚀 Order creation request received: customerId={}, totalAmount={}, correlationId={}",
                orderRequest.get("customerId"), orderRequest.get("totalAmount"), correlationId);

        try {
            String customerId = (String) orderRequest.get("customerId");
            Double totalAmount = Double.valueOf(orderRequest.get("totalAmount").toString());
            @SuppressWarnings("unchecked")
            List<String> productIds = (List<String>) orderRequest.getOrDefault("productIds", List.of());

            logger.info("📝 Creating order: customerId={}, totalAmount={}, productCount={}, correlationId={}",
                    customerId, totalAmount, productIds.size(), correlationId);

            Order order = orderService.createOrder(customerId, totalAmount, productIds);

            logger.info("✅ Order created successfully: orderId={}, status={}, correlationId={}",
                    order.getId(), order.getStatus(), correlationId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "orderId", order.getId(),
                    "status", order.getStatus().name(),
                    "message", "Order created successfully with Event Sourcing",
                    "order", Map.of(
                            "id", order.getId(),
                            "customerId", order.getCustomerId(),
                            "totalAmount", order.getTotalAmount(),
                            "status", order.getStatus().name(),
                            "createdAt", order.getCreatedAt().toString()));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("❌ Order creation failed: customerId={}, error={}, correlationId={}",
                    orderRequest.get("customerId"), e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to create order",
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOrders() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("📋 Get all orders request received, correlationId={}", correlationId);

        try {
            logger.debug("🔍 Calling OrderService.findAll(), correlationId={}", correlationId);
            List<Order> orders = orderService.findAll();

            logger.info("✅ Retrieved {} orders successfully, correlationId={}", orders.size(), correlationId);

            if (logger.isDebugEnabled()) {
                logger.debug("📊 Order details: correlationId={}, orders={}", correlationId,
                        orders.stream().map(o -> String.format("id=%s,status=%s", o.getId(), o.getStatus())).toList());
            }

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Orders retrieved successfully",
                    "count", orders.size(),
                    "correlationId", correlationId,
                    "orders", orders.stream().map(order -> Map.of(
                            "id", order.getId(),
                            "customerId", order.getCustomerId(),
                            "totalAmount", order.getTotalAmount(),
                            "status", order.getStatus().name(),
                            "createdAt", order.getCreatedAt().toString(),
                            "updatedAt", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null))
                            .toList());

            logger.debug("📤 Sending response with {} orders, correlationId={}", orders.size(), correlationId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Failed to retrieve orders: error={}, correlationId={}", e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve orders",
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable String id) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", id);

        logger.info("🔍 Get order by ID request: orderId={}, correlationId={}", id, correlationId);

        try {
            logger.debug("🔍 Calling OrderService.findById(), orderId={}, correlationId={}", id, correlationId);
            Optional<Order> orderOpt = orderService.findById(id);

            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();

                logger.info("✅ Order found: orderId={}, customerId={}, status={}, correlationId={}",
                        order.getId(), order.getCustomerId(), order.getStatus(), correlationId);

                Map<String, Object> response = Map.of(
                        "success", true,
                        "message", "Order found",
                        "correlationId", correlationId,
                        "order", Map.of(
                                "id", order.getId(),
                                "customerId", order.getCustomerId(),
                                "totalAmount", order.getTotalAmount(),
                                "status", order.getStatus().name(),
                                "createdAt", order.getCreatedAt().toString(),
                                "updatedAt", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null));

                return ResponseEntity.ok(response);

            } else {
                logger.warn("⚠️ Order not found: orderId={}, correlationId={}", id, correlationId);

                Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "Order not found",
                        "orderId", id,
                        "correlationId", correlationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            logger.error("❌ Failed to retrieve order: orderId={}, error={}, correlationId={}",
                    id, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve order",
                    "orderId", id,
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> statusRequest) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("🔄 Order status update request: orderId={}, newStatus={}, correlationId={}",
                id, statusRequest.get("status"), correlationId);

        try {
            String status = statusRequest.get("status");

            if (status == null || status.trim().isEmpty()) {
                Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "message", "Status is required",
                        "validStatuses", List.of("PENDING", "CONFIRMED", "PAID", "SHIPPED", "DELIVERED", "CANCELLED"));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Order updatedOrder = orderService.updateOrderStatus(id, status);

            logger.info("✅ Order status updated successfully: orderId={}, newStatus={}, correlationId={}",
                    updatedOrder.getId(), updatedOrder.getStatus().name(), correlationId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Order status updated successfully with Event Sourcing",
                    "orderId", updatedOrder.getId(),
                    "newStatus", updatedOrder.getStatus().name(),
                    "updatedAt", updatedOrder.getUpdatedAt().toString(),
                    "correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error(
                    "❌ Order status update failed (business logic): orderId={}, status={}, error={}, correlationId={}",
                    id, statusRequest.get("status"), e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to update order status",
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.error(
                    "❌ Order status update failed (system error): orderId={}, status={}, error={}, correlationId={}",
                    id, statusRequest.get("status"), e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Internal server error",
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getOrdersByCustomerId(@PathVariable String customerId) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("customerId", customerId);

        logger.info("👤 Get orders by customer ID request: customerId={}, correlationId={}", customerId, correlationId);

        try {
            logger.debug("🔍 Calling OrderService.findByCustomerId(), customerId={}, correlationId={}", customerId,
                    correlationId);
            List<Order> orders = orderService.findByCustomerId(customerId);

            logger.info("✅ Retrieved {} orders for customer: customerId={}, count={}, correlationId={}",
                    orders.size(), customerId, orders.size(), correlationId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Customer orders retrieved successfully",
                    "customerId", customerId,
                    "count", orders.size(),
                    "correlationId", correlationId,
                    "orders", orders.stream().map(order -> Map.of(
                            "id", order.getId(),
                            "totalAmount", order.getTotalAmount(),
                            "status", order.getStatus().name(),
                            "createdAt", order.getCreatedAt().toString())).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Failed to retrieve customer orders: customerId={}, error={}, correlationId={}",
                    customerId, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve customer orders",
                    "customerId", customerId,
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<Map<String, Object>> getOrderEvents(@PathVariable String id) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", id);

        logger.info("📜 Get order events request (Event Sourcing): orderId={}, correlationId={}", id, correlationId);

        try {
            logger.debug("🔍 Calling OrderService.getOrderEvents(), orderId={}, correlationId={}", id, correlationId);
            List<OrderEvent> events = orderService.getOrderEvents(id);

            logger.info("✅ Retrieved {} events for order: orderId={}, eventCount={}, correlationId={}",
                    events.size(), id, events.size(), correlationId);

            if (logger.isDebugEnabled()) {
                logger.debug("📊 Event types: orderId={}, correlationId={}, eventTypes={}",
                        id, correlationId, events.stream().map(OrderEvent::getEventType).toList());
            }

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Order events retrieved successfully (Event Sourcing)",
                    "orderId", id,
                    "eventCount", events.size(),
                    "correlationId", correlationId,
                    "events", events.stream().map(event -> Map.of(
                            "id", event.getId(),
                            "eventType", event.getEventType(),
                            "occurredAt", event.getOccurredAt().toString(),
                            "version", event.getVersion(),
                            "eventData", event.getEventData())).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Failed to retrieve order events: orderId={}, error={}, correlationId={}",
                    id, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve order events",
                    "orderId", id,
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{id}/rebuild")
    public ResponseEntity<Map<String, Object>> rebuildOrderFromEvents(@PathVariable String id) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", id);

        logger.info("🔄 Rebuild order from events request (Event Sourcing): orderId={}, correlationId={}", id,
                correlationId);

        try {
            logger.debug("🔍 Calling OrderService.rebuildOrderFromEvents(), orderId={}, correlationId={}", id,
                    correlationId);
            Order rebuiltOrder = orderService.rebuildOrderFromEvents(id);

            if (rebuiltOrder != null) {
                logger.info("✅ Order successfully rebuilt from events: orderId={}, status={}, correlationId={}",
                        rebuiltOrder.getId(), rebuiltOrder.getStatus(), correlationId);

                Map<String, Object> response = Map.of(
                        "success", true,
                        "message", "Order successfully rebuilt from events (Event Sourcing)",
                        "orderId", rebuiltOrder.getId(),
                        "correlationId", correlationId,
                        "rebuiltOrder", Map.of(
                                "id", rebuiltOrder.getId(),
                                "customerId", rebuiltOrder.getCustomerId(),
                                "totalAmount", rebuiltOrder.getTotalAmount(),
                                "status", rebuiltOrder.getStatus().name(),
                                "createdAt", rebuiltOrder.getCreatedAt().toString(),
                                "updatedAt",
                                rebuiltOrder.getUpdatedAt() != null ? rebuiltOrder.getUpdatedAt().toString() : null));

                return ResponseEntity.ok(response);
            } else {
                logger.warn("⚠️ Could not rebuild order from events: orderId={}, correlationId={}", id, correlationId);

                Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "Could not rebuild order from events - no events found",
                        "orderId", id,
                        "correlationId", correlationId);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            logger.error("❌ Failed to rebuild order from events: orderId={}, error={}, correlationId={}",
                    id, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to rebuild order from events",
                    "orderId", id,
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }
}