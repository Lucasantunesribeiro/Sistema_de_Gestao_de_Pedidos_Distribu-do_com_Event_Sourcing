package com.ordersystem.unified.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ordersystem.unified.shared.events.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for order operations with payment and inventory tracking.
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

    @JsonProperty("reservationId")
    private String reservationId;

    @JsonProperty("paymentId")
    private String paymentId;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("cancellationReason")
    private String cancellationReason;

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

    public OrderResponse(String orderId, String customerId, String customerName, OrderStatus status,
                        BigDecimal totalAmount, List<OrderItemResponse> items, LocalDateTime createdAt,
                        LocalDateTime updatedAt, String correlationId, String reservationId, 
                        String paymentId, String transactionId, String cancellationReason) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.correlationId = correlationId;
        this.reservationId = reservationId;
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.cancellationReason = cancellationReason;
    }

    // Getters and Setters
    public String getOrderId() { 
        return orderId; 
    }
    
    public void setOrderId(String orderId) { 
        this.orderId = orderId; 
    }

    public String getCustomerId() { 
        return customerId; 
    }
    
    public void setCustomerId(String customerId) { 
        this.customerId = customerId; 
    }

    public String getCustomerName() { 
        return customerName; 
    }
    
    public void setCustomerName(String customerName) { 
        this.customerName = customerName; 
    }

    public OrderStatus getStatus() { 
        return status; 
    }
    
    public void setStatus(OrderStatus status) { 
        this.status = status; 
    }

    public BigDecimal getTotalAmount() { 
        return totalAmount; 
    }
    
    public void setTotalAmount(BigDecimal totalAmount) { 
        this.totalAmount = totalAmount; 
    }

    public List<OrderItemResponse> getItems() { 
        return items; 
    }
    
    public void setItems(List<OrderItemResponse> items) { 
        this.items = items; 
    }

    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }
    
    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt; 
    }

    public LocalDateTime getUpdatedAt() { 
        return updatedAt; 
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) { 
        this.updatedAt = updatedAt; 
    }

    public String getCorrelationId() { 
        return correlationId; 
    }
    
    public void setCorrelationId(String correlationId) { 
        this.correlationId = correlationId; 
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    @Override
    public String toString() {
        return String.format("OrderResponse{orderId='%s', customerId='%s', status=%s, totalAmount=%s, paymentId='%s', reservationId='%s'}",
                orderId, customerId, status, totalAmount, paymentId, reservationId);
    }
}