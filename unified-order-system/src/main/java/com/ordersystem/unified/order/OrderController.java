package com.ordersystem.unified.order;

import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for order operations.
 * Spring Boot compatible version with proper annotations.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> createOrder(@RequestBody Map<String, Object> orderRequest) {
        String customerId = (String) orderRequest.get("customerId");
        Object amountObj = orderRequest.get("totalAmount");

        if (customerId == null || amountObj == null) {
            throw new IllegalArgumentException("customerId and totalAmount are required");
        }

        double totalAmount = Double.parseDouble(amountObj.toString());
        String correlationId = UUID.randomUUID().toString();
        OrderResponse response = orderService.createBasicOrder(customerId, totalAmount);
        response.setCorrelationId(correlationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @PostMapping("/full")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> createDetailedOrder(@Valid @RequestBody CreateOrderRequest request) {
        String correlationId = UUID.randomUUID().toString();
        request.setCorrelationId(correlationId);
        OrderResponse response = orderService.createOrder(request);
        response.setCorrelationId(correlationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<OrderResponse> orders = orderService.getOrders(customerId, status, page, size);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable String customerId) {
        List<OrderResponse> orders = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable String status) {
        List<OrderResponse> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId, 
                                                   @RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "Customer requested cancellation");
        OrderResponse response = orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        Map<String, Object> statistics = orderService.getOrderStatistics();
        return ResponseEntity.ok(statistics);
    }
}