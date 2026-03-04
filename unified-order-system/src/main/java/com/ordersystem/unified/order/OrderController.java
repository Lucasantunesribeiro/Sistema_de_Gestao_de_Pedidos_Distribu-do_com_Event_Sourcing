package com.ordersystem.unified.order;

import com.ordersystem.unified.order.application.CancelOrderUseCase;
import com.ordersystem.unified.order.application.CreateOrderUseCase;
import com.ordersystem.unified.order.dto.CancelOrderRequest;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for order operations.
 * Production-ready with OpenAPI documentation and use case orchestration.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired(required = false)
    private CreateOrderUseCase createOrderUseCase;

    @Autowired(required = false)
    private CancelOrderUseCase cancelOrderUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new order", description = "Creates a new order with inventory reservation and payment processing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "422", description = "Business validation failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody @Parameter(description = "Order creation request") CreateOrderRequest request) {
        OrderResponse response = (createOrderUseCase != null) ?
            createOrderUseCase.execute(request) :
            orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves order details by order identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable @Parameter(description = "Order ID") String orderId) {
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Lists orders with optional filtering by customer and status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) @Parameter(description = "Filter by customer ID") String customerId,
            @RequestParam(required = false) @Parameter(description = "Filter by status") String status,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "Page size") int size) {
        List<OrderResponse> orders = orderService.getOrders(customerId, status, page, size);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get orders by customer", description = "Retrieves all orders for a specific customer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer orders retrieved successfully")
    })
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @PathVariable @Parameter(description = "Customer ID") String customerId) {
        List<OrderResponse> orders = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieves all orders with specific status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @PathVariable @Parameter(description = "Order status") String status) {
        List<OrderResponse> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order", description = "Cancels an order with compensation (inventory release and payment refund)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid cancellation request"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "422", description = "Order cannot be cancelled in current state")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable @Parameter(description = "Order ID") String orderId,
            @Valid @RequestBody @Parameter(description = "Cancellation request") CancelOrderRequest request) {
        OrderResponse response = (cancelOrderUseCase != null) ?
            cancelOrderUseCase.execute(orderId, request) :
            orderService.cancelOrder(orderId, request.getReason());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel/{orderId}")
    @Operation(summary = "Cancel an order (POST)", description = "Cancels an order using POST method")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> cancelOrderPost(
            @PathVariable @Parameter(description = "Order ID") String orderId,
            @RequestBody @Parameter(description = "Cancellation request") CancelOrderRequest request) {
        OrderResponse response = (cancelOrderUseCase != null) ?
            cancelOrderUseCase.execute(orderId, request) :
            orderService.cancelOrder(orderId, request.getReason());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get order statistics", description = "Retrieves aggregated statistics about orders")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        Map<String, Object> statistics = orderService.getOrderStatistics();
        return ResponseEntity.ok(statistics);
    }
}
