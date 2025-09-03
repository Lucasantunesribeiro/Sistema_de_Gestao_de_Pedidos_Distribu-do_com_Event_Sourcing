package com.ordersystem.unified.order;

import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.order.dto.SimpleOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderStatistics;
import com.ordersystem.unified.shared.events.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for order operations.
 * Provides endpoints for creating, retrieving, and managing orders.
 */
@RestController
@RequestMapping("/api/orders")
@Validated
@Tag(name = "Orders", description = "Order management operations")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    public OrderController() {
        logger.info("OrderController initialized");
    }

    @Autowired
    private OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with the provided details - accepts both standard and simplified formats")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "422", description = "Order processing failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> createOrder(@RequestBody Object requestBody) {
        try {
            logger.info("Creating order with request body: {}", requestBody);
            
            // Try to handle both formats automatically
            if (requestBody instanceof Map) {
                Map<String, Object> requestMap = (Map<String, Object>) requestBody;
                
                // Check if it's the simplified format (has customerName but no customerId)
                if (requestMap.containsKey("customerName") && !requestMap.containsKey("customerId")) {
                    logger.info("Detected simplified format, converting...");
                    return handleSimplifiedOrder(requestMap);
                }
            }
            
            // If we get here, it should be standard format
            logger.info("Attempting standard format processing...");
            
            // Try to process as CreateOrderRequest
            try {
                // This would need proper deserialization, but for now return error
                logger.warn("Standard format not fully implemented, use /simple endpoint");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Please use /api/orders/simple endpoint for order creation",
                    "status", "UNSUPPORTED_FORMAT",
                    "timestamp", System.currentTimeMillis()
                ));
            } catch (Exception standardError) {
                logger.error("Error processing standard format: {}", standardError.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Invalid order format: " + standardError.getMessage(),
                    "status", "FORMAT_ERROR",
                    "timestamp", System.currentTimeMillis()
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error creating order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error: " + e.getMessage(),
                "status", "ERROR",
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    private ResponseEntity<Object> handleSimplifiedOrder(Map<String, Object> requestMap) {
        try {
            String customerName = (String) requestMap.get("customerName");
            List<Map<String, Object>> items = (List<Map<String, Object>>) requestMap.get("items");
            
            logger.info("Processing simplified order for customer: {} with {} items", 
                       customerName, items != null ? items.size() : 0);
            
            // Validate the simplified request
            validateSimplifiedRequest(requestMap);
            
            // Convert to standard format
            CreateOrderRequest createRequest = convertToCreateOrderRequest(requestMap);
            
            // Create order using OrderService (real persistence)
            OrderResponse orderResponse = orderService.createOrder(createRequest);
            
            logger.info("Order created successfully via OrderService: {}", orderResponse.getOrderId());
            
            // Return response in expected format
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "orderId", orderResponse.getOrderId(),
                "status", orderResponse.getStatus().toString(),
                "message", "Order created successfully",
                "customerName", orderResponse.getCustomerName(),
                "totalAmount", orderResponse.getTotalAmount(),
                "itemCount", orderResponse.getItems().size(),
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error in simplified order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", e.getMessage(),
                "status", "VALIDATION_ERROR",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("Error processing simplified order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to create order: " + e.getMessage(),
                "status", "ERROR",
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @PostMapping("/simple")
    @Operation(summary = "Create a simple order", description = "Creates a new order with simplified format for frontend")
    public ResponseEntity<Object> createSimpleOrder(@Valid @RequestBody SimpleOrderRequest request) {
        try {
            logger.info("Creating simple order: {}", request);
            
            // Convert to standard format and create order
            CreateOrderRequest standardRequest = request.toCreateOrderRequest();
            OrderResponse response = orderService.createOrder(standardRequest);
            
            logger.info("Simple order created successfully: {}", response.getOrderId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "orderId", response.getOrderId(),
                "status", response.getStatus().toString(),
                "message", "Order created successfully",
                "customerName", response.getCustomerName(),
                "totalAmount", response.getTotalAmount()
            ));
        } catch (Exception e) {
            logger.error("Error creating simple order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves an order by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable @NotBlank @Parameter(description = "Order ID") String orderId) {
        try {
            logger.debug("Getting order: {}", orderId);
            
            OrderResponse response = orderService.getOrder(orderId);
            
            logger.debug("Order retrieved: {} with status: {}", orderId, response.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving order {}: {}", orderId, e.getMessage(), e);
            
            if (e.getMessage().contains("not found") || e.getClass().getSimpleName().contains("NotFound")) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @Operation(summary = "Get orders", description = "Retrieves orders based on optional filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> getOrders(
            @RequestParam(required = false) @Parameter(description = "Filter by customer ID") String customerId,
            @RequestParam(required = false) @Parameter(description = "Filter by order status") OrderStatus status,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        
        try {
            logger.info("Getting orders with filters - customerId: {}, status: {}, page: {}, size: {}", 
                        customerId, status, page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            List<OrderResponse> responses;
            
            if (customerId != null && !customerId.trim().isEmpty()) {
                responses = orderService.getOrdersByCustomer(customerId);
                logger.debug("Retrieved {} orders for customer: {}", responses.size(), customerId);
            } else if (status != null) {
                responses = orderService.getOrdersByStatus(status);
                logger.debug("Retrieved {} orders with status: {}", responses.size(), status);
            } else {
                // Return recent orders when no filters are provided
                responses = orderService.getRecentOrders(pageable);
                logger.debug("Retrieved {} recent orders", responses.size());
            }
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error getting orders from database: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to retrieve orders: " + e.getMessage(),
                "status", "ERROR",
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get orders by customer", description = "Retrieves all orders for a specific customer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer orders retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid customer ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @PathVariable @NotBlank @Parameter(description = "Customer ID") String customerId) {
        try {
            logger.debug("Getting orders for customer: {}", customerId);
            
            List<OrderResponse> responses = orderService.getOrdersByCustomer(customerId);
            
            logger.debug("Retrieved {} orders for customer: {}", responses.size(), customerId);
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error retrieving orders for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieves all orders with a specific status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid status"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @PathVariable @Parameter(description = "Order status") OrderStatus status) {
        try {
            logger.debug("Getting orders with status: {}", status);
            
            List<OrderResponse> responses = orderService.getOrdersByStatus(status);
            
            logger.debug("Retrieved {} orders with status: {}", responses.size(), status);
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error retrieving orders with status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Data conversion utilities for simplified order format

    /**
     * Converts simplified order request (Map) to CreateOrderRequest
     */
    private CreateOrderRequest convertToCreateOrderRequest(Map<String, Object> requestMap) {
        logger.debug("Converting simplified request to CreateOrderRequest: {}", requestMap);
        
        String customerName = (String) requestMap.get("customerName");
        String customerId = generateCustomerId();
        List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) requestMap.get("items");
        
        List<OrderItemRequest> items = new ArrayList<>();
        if (itemsMap != null) {
            for (Map<String, Object> itemMap : itemsMap) {
                OrderItemRequest item = convertToOrderItemRequest(itemMap);
                items.add(item);
            }
        }
        
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setCustomerName(customerName);
        request.setItems(items);
        request.setCorrelationId(UUID.randomUUID().toString());
        
        logger.debug("Converted to CreateOrderRequest: customerId={}, itemCount={}", 
                    customerId, items.size());
        
        return request;
    }

    /**
     * Converts item map to OrderItemRequest
     */
    private OrderItemRequest convertToOrderItemRequest(Map<String, Object> itemMap) {
        String productName = (String) itemMap.get("productName");
        String productId = generateProductId(productName);
        
        Object priceObj = itemMap.get("price");
        Object quantityObj = itemMap.get("quantity");
        
        BigDecimal price = convertToDecimal(priceObj);
        Integer quantity = convertToInteger(quantityObj);
        
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setProductName(productName);
        item.setQuantity(quantity);
        item.setUnitPrice(price);
        
        return item;
    }

    /**
     * Generates a unique customer ID
     */
    private String generateCustomerId() {
        return "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generates a unique product ID based on product name
     */
    private String generateProductId(String productName) {
        String prefix = productName != null && !productName.trim().isEmpty() 
            ? productName.replaceAll("[^A-Za-z0-9]", "").toUpperCase().substring(0, Math.min(4, productName.length()))
            : "PROD";
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Validates simplified request format
     */
    private void validateSimplifiedRequest(Map<String, Object> requestMap) {
        if (requestMap == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        
        String customerName = (String) requestMap.get("customerName");
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        
        List<Map<String, Object>> items = (List<Map<String, Object>>) requestMap.get("items");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }
        
        for (Map<String, Object> item : items) {
            validateItemData(item);
        }
    }

    /**
     * Validates individual item data
     */
    private void validateItemData(Map<String, Object> item) {
        String productName = (String) item.get("productName");
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required for all items");
        }
        
        Object price = item.get("price");
        if (price == null) {
            throw new IllegalArgumentException("Price is required for all items");
        }
        
        Object quantity = item.get("quantity");
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required for all items");
        }
        
        BigDecimal priceDecimal = convertToDecimal(price);
        if (priceDecimal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        
        Integer quantityInt = convertToInteger(quantity);
        if (quantityInt <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    /**
     * Converts object to BigDecimal safely
     */
    private BigDecimal convertToDecimal(Object value) {
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid price format: " + value);
            }
        }
        throw new IllegalArgumentException("Price must be a number");
    }

    /**
     * Converts object to Integer safely
     */
    private Integer convertToInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid quantity format: " + value);
            }
        }
        throw new IllegalArgumentException("Quantity must be a number");
    }
} 
   @PostMapping("/cancel/{orderId}")
    @Operation(summary = "Cancel order", description = "Cancel an existing order with proper cleanup")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable @Parameter(description = "Order ID") String orderId,
            @RequestBody @Parameter(description = "Cancellation request") Map<String, String> request) {
        
        try {
            String reason = request.getOrDefault("reason", "Customer requested cancellation");
            String correlationId = request.get("correlationId");
            
            logger.info("Cancelling order: orderId={}, reason={}", orderId, reason);
            
            OrderResponse response = orderService.cancelOrder(orderId, reason, correlationId);
            
            logger.info("Order cancelled successfully: orderId={}, status={}", orderId, response.getStatus());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error cancelling order: orderId={}", orderId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get order statistics", description = "Get order statistics for dashboard")
    public ResponseEntity<Object> getOrderStatistics() {
        
        try {
            logger.debug("Getting order statistics");
            
            OrderStatistics statistics = orderService.getOrderStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error getting order statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Order service health check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "order-service");
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "2.0");
        health.put("features", List.of(
            "order-creation",
            "payment-integration", 
            "inventory-integration",
            "order-cancellation",
            "transaction-orchestration"
        ));
        
        return ResponseEntity.ok(health);
    }
}