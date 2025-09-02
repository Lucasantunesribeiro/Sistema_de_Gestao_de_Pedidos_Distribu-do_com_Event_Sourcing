package com.ordersystem.unified.query;

import com.ordersystem.unified.order.OrderService;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.shared.events.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for querying orders and related data.
 * Provides read-only operations optimized for performance.
 */
@Service
public class QueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);
    
    @Autowired
    private OrderService orderService;
    
    /**
     * Get order by ID.
     */
    public OrderResponse getOrder(String orderId) {
        logger.debug("Querying order: {}", orderId);
        return orderService.getOrder(orderId);
    }
    
    /**
     * Get orders by customer.
     */
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        logger.debug("Querying orders for customer: {}", customerId);
        return orderService.getOrdersByCustomer(customerId);
    }
    
    /**
     * Get orders by status.
     */
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        logger.debug("Querying orders with status: {}", status);
        return orderService.getOrdersByStatus(status);
    }
    
    /**
     * Get recent orders with pagination.
     */
    public List<OrderResponse> getRecentOrders(int page, int size) {
        logger.debug("Querying recent orders - page: {}, size: {}", page, size);
        // For now, return orders by status CONFIRMED as recent orders
        return orderService.getOrdersByStatus(OrderStatus.CONFIRMED);
    }
}