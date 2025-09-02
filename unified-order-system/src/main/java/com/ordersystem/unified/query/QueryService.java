package com.ordersystem.unified.query;

import com.ordersystem.unified.order.OrderService;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.query.dto.OrderQueryResponse;
import com.ordersystem.unified.shared.events.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for querying and aggregating data across all modules.
 */
@Service
public class QueryService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Cacheable(value = "dashboard", key = "'orders'")
    public OrderQueryResponse getOrdersOverview() {
        List<OrderResponse> allOrders = orderService.getOrdersByStatus(OrderStatus.CONFIRMED);
        
        Map<OrderStatus, Long> statusCounts = allOrders.stream()
            .collect(Collectors.groupingBy(OrderResponse::getStatus, Collectors.counting()));

        return new OrderQueryResponse(
            allOrders,
            statusCounts,
            allOrders.size(),
            "Orders retrieved successfully"
        );
    }

    public List<OrderResponse> getRecentOrders(int limit) {
        // For demo, return confirmed orders
        return orderService.getOrdersByStatus(OrderStatus.CONFIRMED)
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
}