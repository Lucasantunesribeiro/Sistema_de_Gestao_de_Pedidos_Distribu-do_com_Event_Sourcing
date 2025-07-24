package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class OrderStatusUpdatedEvent {
    private final String orderId;
    private final String oldStatus;
    private final String newStatus;
    private final LocalDateTime timestamp;

    @JsonCreator
    public OrderStatusUpdatedEvent(@JsonProperty("orderId") String orderId,
                                 @JsonProperty("oldStatus") String oldStatus,
                                 @JsonProperty("newStatus") String newStatus,
                                 @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = timestamp;
    }

    public String getOrderId() { return orderId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public LocalDateTime getTimestamp() { return timestamp; }
}