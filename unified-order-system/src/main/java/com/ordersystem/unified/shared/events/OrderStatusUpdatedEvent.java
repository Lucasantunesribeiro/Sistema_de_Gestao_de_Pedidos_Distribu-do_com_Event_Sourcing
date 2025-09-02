package com.ordersystem.unified.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published when an order status is updated.
 * Used for internal communication between modules.
 */
public class OrderStatusUpdatedEvent extends BaseEvent {
    
    @JsonProperty("orderId")
    private final String orderId;
    
    @JsonProperty("customerId")
    private final String customerId;
    
    @JsonProperty("oldStatus")
    private final String oldStatus;
    
    @JsonProperty("newStatus")
    private final String newStatus;
    
    @JsonProperty("reason")
    private final String reason;

    @JsonCreator
    public OrderStatusUpdatedEvent(@JsonProperty("orderId") String orderId,
                                 @JsonProperty("customerId") String customerId,
                                 @JsonProperty("oldStatus") String oldStatus,
                                 @JsonProperty("newStatus") String newStatus,
                                 @JsonProperty("reason") String reason,
                                 @JsonProperty("correlationId") String correlationId,
                                 @JsonProperty("causationId") String causationId) {
        super(correlationId, causationId, "ORDER_STATUS_UPDATED");
        this.orderId = orderId;
        this.customerId = customerId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    // Convenience constructor
    public OrderStatusUpdatedEvent(String orderId, String customerId, String oldStatus, 
                                 String newStatus, String reason) {
        this(orderId, customerId, oldStatus, newStatus, reason, null, null);
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getReason() { return reason; }

    @Override
    public String toString() {
        return String.format("OrderStatusUpdatedEvent{orderId='%s', customerId='%s', %s -> %s, reason='%s'}",
                orderId, customerId, oldStatus, newStatus, reason);
    }
}