package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderCompletedEvent extends BaseEvent {
    
    @JsonProperty("orderId")
    private final String orderId;
    
    @JsonProperty("customerId")
    private final String customerId;
    
    @JsonProperty("items")
    private final List<OrderItem> items;
    
    @JsonProperty("totalAmount")
    private final BigDecimal totalAmount;
    
    @JsonProperty("paymentId")
    private final String paymentId;
    
    @JsonProperty("completedAt")
    private final LocalDateTime completedAt;

    @JsonCreator
    public OrderCompletedEvent(@JsonProperty("orderId") String orderId,
                              @JsonProperty("customerId") String customerId,
                              @JsonProperty("items") List<OrderItem> items,
                              @JsonProperty("totalAmount") BigDecimal totalAmount,
                              @JsonProperty("paymentId") String paymentId,
                              @JsonProperty("correlationId") String correlationId,
                              @JsonProperty("causationId") String causationId) {
        super(correlationId, causationId, "ORDER_COMPLETED");
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.paymentId = paymentId;
        this.completedAt = LocalDateTime.now();
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getPaymentId() { return paymentId; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}