package com.ordersystem.query.controller;

import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ordersystem.query.entity.OrderReadModel;
import com.ordersystem.query.service.OrderQueryService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderQueryController {

    private static final Logger logger = LoggerFactory.getLogger(OrderQueryController.class);

    @Autowired
    private OrderQueryService orderQueryService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOrders() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("📋 Query service received getAllOrders request, correlationId={}", correlationId);

        try {
            logger.debug("🔍 Calling OrderQueryService.getAllOrders(), correlationId={}", correlationId);

            long startTime = System.currentTimeMillis();
            List<OrderReadModel> orders = orderQueryService.getAllOrders();
            long queryTime = System.currentTimeMillis() - startTime;

            logger.info("✅ Successfully retrieved {} orders from read model: queryTime={}ms, correlationId={}",
                    orders.size(), queryTime, correlationId);

            if (logger.isDebugEnabled()) {
                logger.debug("📊 Order summary: correlationId={}, orders={}", correlationId,
                        orders.stream().map(o -> String.format("id=%s,status=%s,amount=%.2f",
                                o.getOrderId(), o.getStatus(), o.getTotalAmount())).toList());
            }

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Orders retrieved successfully from read model (CQRS)",
                    "count", orders.size(),
                    "queryTime", queryTime,
                    "correlationId", correlationId,
                    "orders", orders);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Failed to retrieve orders from read model: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve orders from read model",
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable String orderId) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", orderId);

        logger.info("🔍 Query service received getOrderById request: orderId={}, correlationId={}", orderId,
                correlationId);

        try {
            logger.debug("🔍 Calling OrderQueryService.getOrderById(), orderId={}, correlationId={}", orderId,
                    correlationId);

            long startTime = System.currentTimeMillis();
            Optional<OrderReadModel> order = orderQueryService.getOrderById(orderId);
            long queryTime = System.currentTimeMillis() - startTime;

            if (order.isPresent()) {
                OrderReadModel orderModel = order.get();
                logger.info(
                        "✅ Order found in read model: orderId={}, customerId={}, status={}, amount={}, queryTime={}ms, correlationId={}",
                        orderModel.getOrderId(), orderModel.getCustomerId(), orderModel.getStatus(),
                        orderModel.getTotalAmount(), queryTime, correlationId);

                Map<String, Object> response = Map.of(
                        "success", true,
                        "message", "Order found in read model (CQRS)",
                        "queryTime", queryTime,
                        "correlationId", correlationId,
                        "order", orderModel);

                return ResponseEntity.ok(response);
            } else {
                logger.warn("⚠️ Order not found in read model: orderId={}, queryTime={}ms, correlationId={}",
                        orderId, queryTime, correlationId);

                Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "Order not found in read model",
                        "orderId", orderId,
                        "queryTime", queryTime,
                        "correlationId", correlationId);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            logger.error("❌ Failed to retrieve order from read model: orderId={}, error={}, correlationId={}",
                    orderId, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve order from read model",
                    "orderId", orderId,
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

        logger.info("👤 Query service received getOrdersByCustomerId request: customerId={}, correlationId={}",
                customerId, correlationId);

        try {
            logger.debug("🔍 Calling OrderQueryService.getOrdersByCustomerId(), customerId={}, correlationId={}",
                    customerId, correlationId);

            long startTime = System.currentTimeMillis();
            List<OrderReadModel> orders = orderQueryService.getOrdersByCustomerId(customerId);
            long queryTime = System.currentTimeMillis() - startTime;

            logger.info(
                    "✅ Retrieved {} orders for customer from read model: customerId={}, count={}, queryTime={}ms, correlationId={}",
                    orders.size(), customerId, orders.size(), queryTime, correlationId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Customer orders retrieved successfully from read model (CQRS)",
                    "customerId", customerId,
                    "count", orders.size(),
                    "queryTime", queryTime,
                    "correlationId", correlationId,
                    "orders", orders);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error(
                    "❌ Failed to retrieve customer orders from read model: customerId={}, error={}, correlationId={}",
                    customerId, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve customer orders from read model",
                    "customerId", customerId,
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getOrdersByStatus(@PathVariable String status) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("status", status);

        logger.info("📊 Query service received getOrdersByStatus request: status={}, correlationId={}", status,
                correlationId);

        try {
            long startTime = System.currentTimeMillis();
            List<OrderReadModel> orders = orderQueryService.getOrdersByStatus(status);
            long queryTime = System.currentTimeMillis() - startTime;

            logger.info(
                    "✅ Retrieved {} orders by status from read model: status={}, count={}, queryTime={}ms, correlationId={}",
                    orders.size(), status, orders.size(), queryTime, correlationId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Orders by status retrieved successfully from read model (CQRS)",
                    "status", status,
                    "count", orders.size(),
                    "queryTime", queryTime,
                    "correlationId", correlationId,
                    "orders", orders);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Failed to retrieve orders by status from read model: status={}, error={}, correlationId={}",
                    status, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve orders by status from read model",
                    "status", status,
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/customer/{customerId}/status/{status}")
    public ResponseEntity<Map<String, Object>> getOrdersByCustomerIdAndStatus(
            @PathVariable String customerId,
            @PathVariable String status) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("customerId", customerId);
        MDC.put("status", status);

        logger.info(
                "👤📊 Query service received getOrdersByCustomerIdAndStatus request: customerId={}, status={}, correlationId={}",
                customerId, status, correlationId);

        try {
            long startTime = System.currentTimeMillis();
            List<OrderReadModel> orders = orderQueryService.getOrdersByCustomerIdAndStatus(customerId, status);
            long queryTime = System.currentTimeMillis() - startTime;

            logger.info(
                    "✅ Retrieved {} orders by customer and status from read model: customerId={}, status={}, count={}, queryTime={}ms, correlationId={}",
                    orders.size(), customerId, status, orders.size(), queryTime, correlationId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Orders by customer and status retrieved successfully from read model (CQRS)",
                    "customerId", customerId,
                    "status", status,
                    "count", orders.size(),
                    "queryTime", queryTime,
                    "correlationId", correlationId,
                    "orders", orders);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error(
                    "❌ Failed to retrieve orders by customer and status from read model: customerId={}, status={}, error={}, correlationId={}",
                    customerId, status, e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve orders by customer and status from read model",
                    "customerId", customerId,
                    "status", status,
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("🏥 Query service health check request, correlationId={}", correlationId);

        try {
            // Test database connectivity by trying to get order count
            long startTime = System.currentTimeMillis();
            List<OrderReadModel> orders = orderQueryService.getAllOrders();
            long queryTime = System.currentTimeMillis() - startTime;

            logger.info("✅ Query service health check passed: orderCount={}, queryTime={}ms, correlationId={}",
                    orders.size(), queryTime, correlationId);

            Map<String, Object> response = Map.of(
                    "status", "healthy",
                    "service", "Order Query Service (CQRS Read Model)",
                    "timestamp", System.currentTimeMillis(),
                    "database", "connected",
                    "orderCount", orders.size(),
                    "queryTime", queryTime,
                    "correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Query service health check failed: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);

            Map<String, Object> response = Map.of(
                    "status", "unhealthy",
                    "service", "Order Query Service (CQRS Read Model)",
                    "timestamp", System.currentTimeMillis(),
                    "error", e.getMessage(),
                    "correlationId", correlationId);

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("📊 Query service received dashboard metrics request (CQRS analytics), correlationId={}",
                correlationId);

        try {
            logger.debug("🔍 Calling OrderQueryService.getAllOrders() for metrics, correlationId={}", correlationId);

            long startTime = System.currentTimeMillis();
            List<OrderReadModel> allOrders = orderQueryService.getAllOrders();
            long queryTime = System.currentTimeMillis() - startTime;

            logger.debug("📊 Calculating dashboard metrics from {} orders, correlationId={}", allOrders.size(),
                    correlationId);

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalOrders", allOrders.size());

            double totalRevenue = allOrders.stream()
                    .mapToDouble(OrderReadModel::getTotalAmount)
                    .sum();
            metrics.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);

            long completedOrders = allOrders.stream()
                    .filter(order -> "COMPLETED".equals(order.getStatus()) || "PAID".equals(order.getStatus()))
                    .count();
            metrics.put("completedOrders", completedOrders);

            long pendingOrders = allOrders.stream()
                    .filter(order -> "PENDING".equals(order.getStatus()) || "PROCESSING".equals(order.getStatus()))
                    .count();
            metrics.put("pendingOrders", pendingOrders);

            long cancelledOrders = allOrders.stream()
                    .filter(order -> "CANCELLED".equals(order.getStatus()))
                    .count();
            metrics.put("cancelledOrders", cancelledOrders);

            double averageOrderValue = allOrders.isEmpty() ? 0.0 : totalRevenue / allOrders.size();
            metrics.put("averageOrderValue", Math.round(averageOrderValue * 100.0) / 100.0);

            // Add query performance metrics
            metrics.put("queryTime", queryTime);
            metrics.put("correlationId", correlationId);
            metrics.put("timestamp", System.currentTimeMillis());

            logger.info(
                    "✅ Dashboard metrics calculated successfully (CQRS analytics): totalOrders={}, totalRevenue={}, queryTime={}ms, correlationId={}",
                    allOrders.size(), totalRevenue, queryTime, correlationId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Dashboard metrics retrieved successfully from read model (CQRS)",
                    "queryTime", queryTime,
                    "correlationId", correlationId,
                    "metrics", metrics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Failed to calculate dashboard metrics: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to calculate dashboard metrics",
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/cqrs/demo")
    public ResponseEntity<Map<String, Object>> demonstrateCQRS() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("🏗️ CQRS demonstration request, correlationId={}", correlationId);

        try {
            long startTime = System.currentTimeMillis();

            // Query side - optimized read models
            List<OrderReadModel> queryOrders = orderQueryService.getAllOrders();
            long queryTime = System.currentTimeMillis() - startTime;

            logger.info("✅ CQRS demonstration completed: queryOrders={}, queryTime={}ms, correlationId={}",
                    queryOrders.size(), queryTime, correlationId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "CQRS demonstration - showing separation of command and query responsibilities",
                    "explanation", Map.of(
                            "commandSide", "Order Service handles writes (create, update) with Event Sourcing",
                            "querySide", "Query Service handles reads with optimized read models",
                            "eventDriven", "Services communicate via RabbitMQ events for eventual consistency",
                            "benefits", List.of(
                                    "Optimized read models for fast queries",
                                    "Scalable - can scale read and write sides independently",
                                    "Flexible - different data models for different use cases",
                                    "Resilient - eventual consistency with event replay capability")),
                    "queryPerformance", Map.of(
                            "readModelCount", queryOrders.size(),
                            "queryTime", queryTime,
                            "averageQueryTime", queryTime > 0 ? queryTime / Math.max(1, queryOrders.size()) : 0),
                    "correlationId", correlationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ CQRS demonstration failed: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "CQRS demonstration failed",
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }
}
