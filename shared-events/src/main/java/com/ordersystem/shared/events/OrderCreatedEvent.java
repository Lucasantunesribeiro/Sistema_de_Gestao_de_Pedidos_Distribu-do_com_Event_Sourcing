package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderCreatedEvent {
    private final String orderId;
    private final String customerId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final LocalDateTime timestamp;

    @JsonCreator
    public OrderCreatedEvent(@JsonProperty("orderId") String orderId,
                           @JsonProperty("customerId") String customerId,
                           @JsonProperty("items") List<OrderItem> items,
                           @JsonProperty("totalAmount") BigDecimal totalAmount,
                           @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}