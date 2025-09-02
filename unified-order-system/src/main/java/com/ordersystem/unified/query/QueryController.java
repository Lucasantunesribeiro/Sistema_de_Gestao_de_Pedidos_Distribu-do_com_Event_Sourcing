package com.ordersystem.unified.query;

import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.shared.events.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for querying orders and related data.
 * Provides read-only operations optimized for performance.
 */
@RestController
@RequestMapping("/api/query")
public class QueryController {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);
    
    @Autowired
    private QueryService queryService;
    
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        logger.debug("Query: Getting order {}", orderId);
        OrderResponse order = queryService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.debug("Query: Getting orders with customerId={}, status={}, page={}, size={}", 
                    customerId, status, page, size);
        
        List<OrderResponse> orders;
        if (customerId != null && !customerId.trim().isEmpty()) {
            orders = queryService.getOrdersByCustomer(customerId);
        } else if (status != null) {
            orders = queryService.getOrdersByStatus(status);
        } else {
            orders = queryService.getRecentOrders(page, size);
        }
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/orders/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable String customerId) {
        logger.debug("Query: Getting orders for customer {}", customerId);
        List<OrderResponse> orders = queryService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/orders/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
        logger.debug("Query: Getting orders with status {}", status);
        List<OrderResponse> orders = queryService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }
}