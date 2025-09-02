package com.ordersystem.unified.order;

import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
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

import java.util.List;

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

    @Autowired
    private OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "422", description = "Order processing failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody @Parameter(description = "Order creation request") CreateOrderRequest request) {
        logger.info("Creating order for customer: {} with correlation ID: {}", 
                   request.getCustomerId(), request.getCorrelationId());
        
        OrderResponse response = orderService.createOrder(request);
        
        logger.info("Order created successfully: {} for customer: {}", 
                   response.getOrderId(), response.getCustomerId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        logger.debug("Getting order: {}", orderId);
        
        OrderResponse response = orderService.getOrder(orderId);
        
        logger.debug("Order retrieved: {} with status: {}", orderId, response.getStatus());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get orders", description = "Retrieves orders based on optional filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) @Parameter(description = "Filter by customer ID") String customerId,
            @RequestParam(required = false) @Parameter(description = "Filter by order status") OrderStatus status,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        
        logger.debug("Getting orders with filters - customerId: {}, status: {}, page: {}, size: {}", 
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
        logger.debug("Getting orders for customer: {}", customerId);
        
        List<OrderResponse> responses = orderService.getOrdersByCustomer(customerId);
        
        logger.debug("Retrieved {} orders for customer: {}", responses.size(), customerId);
        
        return ResponseEntity.ok(responses);
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
        logger.debug("Getting orders with status: {}", status);
        
        List<OrderResponse> responses = orderService.getOrdersByStatus(status);
        
        logger.debug("Retrieved {} orders with status: {}", responses.size(), status);
        
        return ResponseEntity.ok(responses);
    }
}