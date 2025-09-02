package com.ordersystem.unified.query.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.shared.events.OrderStatus;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for order queries and dashboard data.
 */
public class OrderQueryResponse {

    @JsonProperty("orders")
    private List<OrderResponse> orders;

    @JsonProperty("statusCounts")
    private Map<OrderStatus, Long> statusCounts;

    @JsonProperty("totalCount")
    private int totalCount;

    @JsonProperty("message")
    private String message;

    public OrderQueryResponse() {}

    public OrderQueryResponse(List<OrderResponse> orders, Map<OrderStatus, Long> statusCounts, 
                            int totalCount, String message) {
        this.orders = orders;
        this.statusCounts = statusCounts;
        this.totalCount = totalCount;
        this.message = message;
    }

    // Getters and Setters
    public List<OrderResponse> getOrders() { return orders; }
    public void setOrders(List<OrderResponse> orders) { this.orders = orders; }

    public Map<OrderStatus, Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<OrderStatus, Long> statusCounts) { this.statusCounts = statusCounts; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}