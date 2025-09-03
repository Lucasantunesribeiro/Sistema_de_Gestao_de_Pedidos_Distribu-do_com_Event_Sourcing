package com.ordersystem.unified.order;

import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.order.dto.OrderItemResponse;
import com.ordersystem.unified.shared.events.OrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Order service for business logic.
 * Enhanced version with proper DTO support.
 */
@Service
public class OrderService {

    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderId = "ORDER-" + System.currentTimeMillis();
        
        // Convert OrderItemRequest to OrderItemResponse
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (request.getItems() != null) {
            for (var item : request.getItems()) {
                OrderItemResponse itemResponse = new OrderItemResponse();
                itemResponse.setProductId(item.getProductId());
                itemResponse.setProductName(item.getProductName());
                itemResponse.setQuantity(item.getQuantity());
                itemResponse.setUnitPrice(item.getUnitPrice());
                itemResponse.setTotalPrice(item.getTotalPrice());
                itemResponses.add(itemResponse);
            }
        }
        
        OrderResponse response = new OrderResponse();
        response.setOrderId(orderId);
        response.setCustomerId(request.getCustomerId());
        response.setCustomerName(request.getCustomerName());
        response.setStatus(OrderStatus.PENDING);
        response.setTotalAmount(calculateTotal(request));
        response.setItems(itemResponses);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        response.setCorrelationId(request.getCorrelationId());
        
        return response;
    }

    public OrderResponse getOrder(String orderId) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(orderId);
        response.setCustomerId("CUST-123");
        response.setCustomerName("Sample Customer");
        response.setStatus(OrderStatus.PENDING);
        response.setTotalAmount(new BigDecimal("100.00"));
        response.setItems(new ArrayList<>());
        response.setCreatedAt(LocalDateTime.now().minusHours(1));
        response.setUpdatedAt(LocalDateTime.now());
        response.setCorrelationId("CORR-" + System.currentTimeMillis());
        
        return response;
    }

    public List<OrderResponse> getOrders(String customerId, String status, int page, int size) {
        List<OrderResponse> orders = new ArrayList<>();
        
        for (int i = 1; i <= size; i++) {
            OrderResponse order = new OrderResponse();
            order.setOrderId("ORDER-" + (page * size + i));
            order.setCustomerId(customerId != null ? customerId : "CUST-" + i);
            order.setCustomerName("Customer " + i);
            order.setStatus(status != null ? OrderStatus.valueOf(status.toUpperCase()) : OrderStatus.PENDING);
            order.setTotalAmount(new BigDecimal(100.0 * i));
            order.setItems(new ArrayList<>());
            order.setCreatedAt(LocalDateTime.now().minusHours(i));
            order.setUpdatedAt(LocalDateTime.now());
            order.setCorrelationId("CORR-" + System.currentTimeMillis());
            orders.add(order);
        }
        
        return orders;
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        List<OrderResponse> orders = new ArrayList<>();
        OrderResponse order = new OrderResponse();
        order.setOrderId("ORDER-123");
        order.setCustomerId(customerId);
        order.setCustomerName("Sample Customer");
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setItems(new ArrayList<>());
        order.setCreatedAt(LocalDateTime.now().minusHours(1));
        order.setUpdatedAt(LocalDateTime.now());
        order.setCorrelationId("CORR-" + System.currentTimeMillis());
        orders.add(order);
        return orders;
    }

    public List<OrderResponse> getOrdersByStatus(String status) {
        List<OrderResponse> orders = new ArrayList<>();
        OrderResponse order = new OrderResponse();
        order.setOrderId("ORDER-123");
        order.setCustomerId("CUST-123");
        order.setCustomerName("Sample Customer");
        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setItems(new ArrayList<>());
        order.setCreatedAt(LocalDateTime.now().minusHours(1));
        order.setUpdatedAt(LocalDateTime.now());
        order.setCorrelationId("CORR-" + System.currentTimeMillis());
        orders.add(order);
        return orders;
    }

    public OrderResponse cancelOrder(String orderId, String reason) {
        OrderResponse order = new OrderResponse();
        order.setOrderId(orderId);
        order.setCustomerId("CUST-123");
        order.setCustomerName("Sample Customer");
        order.setStatus(OrderStatus.CANCELLED);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setItems(new ArrayList<>());
        order.setCreatedAt(LocalDateTime.now().minusHours(1));
        order.setUpdatedAt(LocalDateTime.now());
        order.setCancellationReason(reason);
        order.setCorrelationId("CORR-" + System.currentTimeMillis());
        return order;
    }

    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalOrders", 100);
        statistics.put("pendingOrders", 25);
        statistics.put("completedOrders", 70);
        statistics.put("cancelledOrders", 5);
        statistics.put("totalRevenue", 10000.0);
        statistics.put("timestamp", System.currentTimeMillis());
        return statistics;
    }

    private BigDecimal calculateTotal(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}