package com.ordersystem.shared.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderCreatedEvent {
    private final String orderId;
    private final String customerId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final LocalDateTime timestamp;
    private final String correlationId;
    private final String causationId;

    @JsonCreator
    public OrderCreatedEvent(@JsonProperty("orderId") String orderId,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("items") List<OrderItem> items,
            @JsonProperty("totalAmount") BigDecimal totalAmount,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("causationId") String causationId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
        this.causationId = causationId;
    }

    // Convenience constructor for backward compatibility
    public OrderCreatedEvent(String orderId, String customerId, List<OrderItem> items,
            BigDecimal totalAmount, LocalDateTime timestamp) {
        this(orderId, customerId, items, totalAmount, timestamp, null, null);
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getCausationId() {
        return causationId;
    }
}