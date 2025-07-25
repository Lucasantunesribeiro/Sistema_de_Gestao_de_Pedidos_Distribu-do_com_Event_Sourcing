package com.ordersystem.query.controller;

import com.ordersystem.query.entity.OrderReadModel;
import com.ordersystem.query.service.OrderQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    @Autowired
    private OrderQueryService orderQueryService;

    @GetMapping
    public ResponseEntity<List<OrderReadModel>> getAllOrders() {
        List<OrderReadModel> orders = orderQueryService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderReadModel> getOrderById(@PathVariable String orderId) {
        Optional<OrderReadModel> order = orderQueryService.getOrderById(orderId);
        return order.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderReadModel>> getOrdersByCustomerId(@PathVariable String customerId) {
        List<OrderReadModel> orders = orderQueryService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderReadModel>> getOrdersByStatus(@PathVariable String status) {
        List<OrderReadModel> orders = orderQueryService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/customer/{customerId}/status/{status}")
    public ResponseEntity<List<OrderReadModel>> getOrdersByCustomerIdAndStatus(
            @PathVariable String customerId, 
            @PathVariable String status) {
        List<OrderReadModel> orders = orderQueryService.getOrdersByCustomerIdAndStatus(customerId, status);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Query Service is healthy");
    }
}