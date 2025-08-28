package com.ordersystem.query.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ordersystem.query.entity.OrderReadModel;
import com.ordersystem.query.service.OrderQueryService;
import com.ordersystem.query.metrics.CustomMetricsService;
import io.micrometer.core.instrument.Timer;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(
    origins = {"${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}"},
    allowedHeaders = {"*"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}
)
public class OrderQueryController {

    private static final Logger logger = LoggerFactory.getLogger(OrderQueryController.class);

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private CustomMetricsService metricsService;

    @Value("${app.pagination.default-size:20}")
    private int defaultPageSize;

    @Value("${app.pagination.max-size:100}")
    private int maxPageSize;

    @GetMapping
    @Cacheable(value = "orders", key = "#page + '-' + #size + '-' + #sort")
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        // Limit page size to prevent abuse
        size = Math.min(size, maxPageSize);

        logger.info("📋 Query service received paginated getAllOrders request: page={}, size={}, sort={}, correlationId={}", 
                page, size, sort, correlationId);

        try {
            Timer.Sample sample = metricsService.startQueryTimer();
            long startTime = System.currentTimeMillis();
            
            Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            
            Page<OrderReadModel> ordersPage = orderQueryService.getAllOrdersPaged(pageable);
            long queryTime = System.currentTimeMillis() - startTime;

            // Record metrics
            metricsService.recordQueryExecution(sample, "getAllOrders", true);
            metricsService.updateLastQueryTime(queryTime);

            logger.info("✅ Successfully retrieved {} orders (page {} of {}) from read model: queryTime={}ms, correlationId={}",
                    ordersPage.getContent().size(), page + 1, ordersPage.getTotalPages(), queryTime, correlationId);

            Map<String, Object> pagination = Map.of(
                "currentPage", page,
                "totalPages", ordersPage.getTotalPages(),
                "totalElements", ordersPage.getTotalElements(),
                "size", size,
                "hasNext", ordersPage.hasNext(),
                "hasPrevious", ordersPage.hasPrevious()
            );

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Orders retrieved successfully from read model (CQRS with pagination)",
                    "queryTime", queryTime,
                    "correlationId", correlationId,
                    "pagination", pagination,
                    "orders", ordersPage.getContent());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Record error metrics
            metricsService.recordError("query_execution", "getAllOrders");
            
            logger.error("❌ Failed to retrieve paginated orders from read model: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve paginated orders from read model",
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
    @Cacheable(value = "health", key = "'health'", unless = "#result.statusCode != 200")
    public ResponseEntity<Map<String, Object>> health() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.debug("🏥 Query service health check request, correlationId={}", correlationId);

        try {
            long startTime = System.currentTimeMillis();
            
            // Lightweight database connectivity test - just get count without fetching data
            long orderCount = orderQueryService.getOrderCount();
            boolean cacheHealthy = orderQueryService.isCacheHealthy();
            
            long queryTime = System.currentTimeMillis() - startTime;

            Map<String, Object> healthChecks = Map.of(
                "database", orderCount >= 0 ? "healthy" : "unhealthy",
                "cache", cacheHealthy ? "healthy" : "degraded",
                "memory", getMemoryStatus(),
                "uptime", System.currentTimeMillis()
            );

            boolean isHealthy = orderCount >= 0 && queryTime < 5000; // 5s threshold

            if (isHealthy) {
                logger.debug("✅ Query service health check passed: orderCount={}, queryTime={}ms, correlationId={}",
                        orderCount, queryTime, correlationId);

                Map<String, Object> response = Map.of(
                        "status", "healthy",
                        "service", "Order Query Service (CQRS Read Model)",
                        "timestamp", System.currentTimeMillis(),
                        "orderCount", orderCount,
                        "queryTime", queryTime,
                        "checks", healthChecks,
                        "correlationId", correlationId);

                return ResponseEntity.ok(response);
            } else {
                logger.warn("⚠️ Query service health check degraded: queryTime={}ms, correlationId={}", 
                        queryTime, correlationId);
                
                Map<String, Object> response = Map.of(
                        "status", "degraded",
                        "service", "Order Query Service (CQRS Read Model)",
                        "timestamp", System.currentTimeMillis(),
                        "queryTime", queryTime,
                        "checks", healthChecks,
                        "correlationId", correlationId);

                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }

        } catch (Exception e) {
            logger.error("❌ Query service health check failed: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);

            Map<String, Object> response = Map.of(
                    "status", "unhealthy",
                    "service", "Order Query Service (CQRS Read Model)",
                    "timestamp", System.currentTimeMillis(),
                    "error", e.getMessage(),
                    "checks", Map.of("database", "unhealthy", "error", e.getMessage()),
                    "correlationId", correlationId);

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } finally {
            MDC.clear();
        }
    }

    private Map<String, Object> getMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        return Map.of(
            "used", usedMemory / 1024 / 1024 + "MB",
            "free", freeMemory / 1024 / 1024 + "MB",
            "total", totalMemory / 1024 / 1024 + "MB",
            "max", maxMemory / 1024 / 1024 + "MB",
            "usagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0,
            "status", memoryUsagePercent > 85 ? "critical" : memoryUsagePercent > 70 ? "warning" : "healthy"
        );
    }

    @GetMapping("/dashboard/metrics")
    @Cacheable(value = "dashboard-metrics", key = "'metrics'")
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("📊 Query service received dashboard metrics request (optimized CQRS analytics), correlationId={}",
                correlationId);

        try {
            long startTime = System.currentTimeMillis();
            
            // Use optimized method instead of loading all orders
            Map<String, Object> metrics = orderQueryService.getDashboardMetricsOptimized();
            long queryTime = System.currentTimeMillis() - startTime;

            // Add query performance metrics
            Map<String, Object> enhancedMetrics = new HashMap<>(metrics);
            enhancedMetrics.put("queryTime", queryTime);
            enhancedMetrics.put("correlationId", correlationId);
            enhancedMetrics.put("timestamp", System.currentTimeMillis());
            enhancedMetrics.put("optimized", true);

            logger.info(
                    "✅ Dashboard metrics calculated successfully (optimized CQRS analytics): totalOrders={}, totalRevenue={}, queryTime={}ms, correlationId={}",
                    metrics.get("totalOrders"), metrics.get("totalRevenue"), queryTime, correlationId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Dashboard metrics retrieved successfully from read model (optimized CQRS)",
                    "queryTime", queryTime,
                    "correlationId", correlationId,
                    "metrics", enhancedMetrics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            metricsService.recordError("metrics_calculation", "getDashboardMetrics");
            
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

    @GetMapping("/metrics/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("📊 Query service received performance metrics request, correlationId={}", correlationId);

        try {
            Map<String, Double> performanceSummary = metricsService.getPerformanceSummary();
            Map<String, Object> healthMetrics = metricsService.getHealthMetrics();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Performance metrics retrieved successfully",
                    "timestamp", System.currentTimeMillis(),
                    "correlationId", correlationId,
                    "performance", performanceSummary,
                    "health", healthMetrics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            metricsService.recordError("metrics_retrieval", "getPerformanceMetrics");
            
            logger.error("❌ Failed to retrieve performance metrics: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to retrieve performance metrics",
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

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> orderRequest) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("🛍️ Query service received createOrder request (demo mode), correlationId={}", correlationId);

        try {
            // Simulate order creation (in real system, this would go to order-service)
            String orderId = UUID.randomUUID().toString();
            String customerName = (String) orderRequest.get("customerName");
            Double totalAmount = ((Number) orderRequest.get("totalAmount")).doubleValue();
            
            logger.info("✅ Demo order created: orderId={}, customer={}, total={}, correlationId={}", 
                    orderId, customerName, totalAmount, correlationId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Order created successfully (demo mode)",
                    "orderId", orderId,
                    "customerName", customerName,
                    "totalAmount", totalAmount,
                    "status", "CREATED",
                    "correlationId", correlationId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("❌ Failed to create demo order: error={}, correlationId={}",
                    e.getMessage(), correlationId, e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to create order (demo mode)",
                    "correlationId", correlationId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }
}
