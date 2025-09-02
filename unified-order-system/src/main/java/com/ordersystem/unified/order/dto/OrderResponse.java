package com.ordersystem.unified.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ordersystem.unified.shared.events.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for order operations.
 */
public class OrderResponse {

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("customerName")
    private String customerName;

    @JsonProperty("status")
    private OrderStatus status;

    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("items")
    private List<OrderItemResponse> items;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @JsonProperty("correlationId")
    private String correlationId;

    // Default constructor for JSON serialization
    public OrderResponse() {}

    public OrderResponse(String orderId, String customerId, String customerName, OrderStatus status,
                        BigDecimal totalAmount, List<OrderItemResponse> items, LocalDateTime createdAt,
                        LocalDateTime updatedAt, String correlationId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.correlationId = correlationId;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    @Override
    public String toString() {
        return String.format("OrderResponse{orderId='%s', customerId='%s', status=%s, totalAmount=%s}",
                orderId, customerId, status, totalAmount);
    }
}