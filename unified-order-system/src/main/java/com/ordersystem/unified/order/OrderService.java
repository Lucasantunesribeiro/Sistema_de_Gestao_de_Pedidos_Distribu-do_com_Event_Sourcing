package com.ordersystem.unified.order;

import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.order.dto.OrderItemResponse;
import com.ordersystem.unified.shared.events.OrderStatus;
import com.ordersystem.unified.shared.exceptions.OrderNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Order service for business logic.
 * Enhanced version with proper DTO support.
 */
@Service
public class OrderService {

    private final Map<String, OrderResponse> orders = new ConcurrentHashMap<>();

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

        orders.put(orderId, response);
        return response;
    }

    public OrderResponse createBasicOrder(String customerId, double totalAmount) {
        String orderId = "ORDER-" + System.currentTimeMillis();

        OrderResponse response = new OrderResponse();
        response.setOrderId(orderId);
        response.setCustomerId(customerId);
        response.setStatus(OrderStatus.PENDING);
        response.setTotalAmount(BigDecimal.valueOf(totalAmount));
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());

        orders.put(orderId, response);
        return response;
    }

    public OrderResponse getOrder(String orderId) {
        return orders.get(orderId);
    }

    public List<OrderResponse> getOrders(String customerId, String status, int page, int size) {
        return orders.values().stream()
                .filter(o -> customerId == null || customerId.equals(o.getCustomerId()))
                .filter(o -> status == null || status.equalsIgnoreCase(o.getStatus().name()))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orders.values().stream()
                .filter(o -> customerId.equals(o.getCustomerId()))
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByStatus(String status) {
        return orders.values().stream()
                .filter(o -> status.equalsIgnoreCase(o.getStatus().name()))
                .collect(Collectors.toList());
    }

    public OrderResponse cancelOrder(String orderId, String reason) {
        OrderResponse order = orders.get(orderId);
        if (order != null) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            order.setCancellationReason(reason);
        }
        return order;
    }

    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        long pending = orders.values().stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long completed = orders.values().stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED || o.getStatus() == OrderStatus.COMPLETED)
                .count();
        long cancelled = orders.values().stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
        BigDecimal revenue = orders.values().stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED || o.getStatus() == OrderStatus.COMPLETED)
                .map(OrderResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        statistics.put("totalOrders", orders.size());
        statistics.put("pendingOrders", pending);
        statistics.put("completedOrders", completed);
        statistics.put("cancelledOrders", cancelled);
        statistics.put("totalRevenue", revenue);
        statistics.put("timestamp", System.currentTimeMillis());
        return statistics;
    }

    public OrderResponse updateStatus(String orderId, OrderStatus newStatus) {
        OrderResponse order = orders.get(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        OrderStatus current = order.getStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition: %s -> %s", current, newStatus));
        }
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus target) {
        if (current == target) return true;
        if (current.isTerminal()) return false;
        return true;
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
