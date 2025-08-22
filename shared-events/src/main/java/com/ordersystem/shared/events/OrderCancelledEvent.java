package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class OrderCancelledEvent extends BaseEvent {
    
    @JsonProperty("orderId")
    private final String orderId;
    
    @JsonProperty("customerId")
    private final String customerId;
    
    @JsonProperty("reason")
    private final String reason;
    
    @JsonProperty("cancelledAt")
    private final LocalDateTime cancelledAt;

    @JsonCreator
    public OrderCancelledEvent(@JsonProperty("orderId") String orderId,
                              @JsonProperty("customerId") String customerId,
                              @JsonProperty("reason") String reason,
                              @JsonProperty("correlationId") String correlationId,
                              @JsonProperty("causationId") String causationId) {
        super(correlationId, causationId, "ORDER_CANCELLED");
        this.orderId = orderId;
        this.customerId = customerId;
        this.reason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getReason() { return reason; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}