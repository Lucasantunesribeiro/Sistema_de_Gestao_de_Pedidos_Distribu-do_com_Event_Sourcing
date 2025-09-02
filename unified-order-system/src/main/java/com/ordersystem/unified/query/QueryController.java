package com.ordersystem.unified.query;

import com.ordersystem.unified.order.OrderService;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.query.dto.OrderQueryResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for query and dashboard operations.
 */
@RestController
@RequestMapping("/api")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/dashboard")
    public ResponseEntity<OrderQueryResponse> getDashboard() {
        OrderQueryResponse response = queryService.getOrdersOverview();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getOrders() {
        List<OrderResponse> orders = queryService.getRecentOrders(50);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "unified-order-system",
            "version", "1.0.0"
        ));
    }
}