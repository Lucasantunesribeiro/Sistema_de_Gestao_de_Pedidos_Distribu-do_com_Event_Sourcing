package com.ordersystem.unified.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when a new order is created.
 * Used for internal communication between modules.
 */
public class OrderCreatedEvent extends BaseEvent {
    
    @JsonProperty("orderId")
    private final String orderId;
    
    @JsonProperty("customerId")
    private final String customerId;
    
    @JsonProperty("customerName")
    private final String customerName;
    
    @JsonProperty("items")
    private final List<OrderItem> items;
    
    @JsonProperty("totalAmount")
    private final BigDecimal totalAmount;

    @JsonCreator
    public OrderCreatedEvent(@JsonProperty("orderId") String orderId,
                           @JsonProperty("customerId") String customerId,
                           @JsonProperty("customerName") String customerName,
                           @JsonProperty("items") List<OrderItem> items,
                           @JsonProperty("totalAmount") BigDecimal totalAmount,
                           @JsonProperty("correlationId") String correlationId,
                           @JsonProperty("causationId") String causationId) {
        super(correlationId, causationId, "ORDER_CREATED");
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    // Convenience constructor
    public OrderCreatedEvent(String orderId, String customerId, String customerName, 
                           List<OrderItem> items, BigDecimal totalAmount) {
        this(orderId, customerId, customerName, items, totalAmount, null, null);
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }

    @Override
    public String toString() {
        return String.format("OrderCreatedEvent{orderId='%s', customerId='%s', totalAmount=%s, itemCount=%d}",
                orderId, customerId, totalAmount, items != null ? items.size() : 0);
    }
}