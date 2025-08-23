package com.ordersystem.order.controller;

import com.ordersystem.order.model.Order;
import com.ordersystem.order.model.OrderEvent;
import com.ordersystem.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> orderRequest) {
        try {
            String customerId = (String) orderRequest.get("customerId");
            Double totalAmount = Double.valueOf(orderRequest.get("totalAmount").toString());
            List<String> productIds = (List<String>) orderRequest.getOrDefault("productIds", List.of());
            
            Order order = orderService.createOrder(customerId, totalAmount, productIds);
            
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
                    "createdAt", order.getCreatedAt().toString()
                )
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Failed to create order"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOrders() {
        try {
            List<Order> orders = orderService.findAll();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Orders retrieved successfully",
                "count", orders.size(),
                "orders", orders.stream().map(order -> Map.of(
                    "id", order.getId(),
                    "customerId", order.getCustomerId(),
                    "totalAmount", order.getTotalAmount(),
                    "status", order.getStatus().name(),
                    "createdAt", order.getCreatedAt().toString(),
                    "updatedAt", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null
                )).toList()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Failed to retrieve orders"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable String id) {
        try {
            Optional<Order> orderOpt = orderService.findById(id);
            
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Order found",
                    "order", Map.of(
                        "id", order.getId(),
                        "customerId", order.getCustomerId(),
                        "totalAmount", order.getTotalAmount(),
                        "status", order.getStatus().name(),
                        "createdAt", order.getCreatedAt().toString(),
                        "updatedAt", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null
                    )
                );
                
                return ResponseEntity.ok(response);
                
            } else {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Order not found",
                    "orderId", id
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Failed to retrieve order"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable String id, 
            @RequestBody Map<String, String> statusRequest) {
        try {
            String status = statusRequest.get("status");
            
            if (status == null || status.trim().isEmpty()) {
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Status is required",
                    "validStatuses", List.of("PENDING", "CONFIRMED", "PAID", "SHIPPED", "DELIVERED", "CANCELLED")
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            Order updatedOrder = orderService.updateOrderStatus(id, status);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Order status updated successfully with Event Sourcing",
                "orderId", updatedOrder.getId(),
                "newStatus", updatedOrder.getStatus().name(),
                "updatedAt", updatedOrder.getUpdatedAt().toString()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Failed to update order status"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Internal server error"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getOrdersByCustomerId(@PathVariable String customerId) {
        try {
            List<Order> orders = orderService.findByCustomerId(customerId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Customer orders retrieved successfully",
                "customerId", customerId,
                "count", orders.size(),
                "orders", orders.stream().map(order -> Map.of(
                    "id", order.getId(),
                    "totalAmount", order.getTotalAmount(),
                    "status", order.getStatus().name(),
                    "createdAt", order.getCreatedAt().toString()
                )).toList()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Failed to retrieve customer orders"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/{id}/events")
    public ResponseEntity<Map<String, Object>> getOrderEvents(@PathVariable String id) {
        try {
            List<OrderEvent> events = orderService.getOrderEvents(id);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Order events retrieved successfully (Event Sourcing)",
                "orderId", id,
                "eventCount", events.size(),
                "events", events.stream().map(event -> Map.of(
                    "id", event.getId(),
                    "eventType", event.getEventType(),
                    "occurredAt", event.getOccurredAt().toString(),
                    "version", event.getVersion(),
                    "eventData", event.getEventData()
                )).toList()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Failed to retrieve order events"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}