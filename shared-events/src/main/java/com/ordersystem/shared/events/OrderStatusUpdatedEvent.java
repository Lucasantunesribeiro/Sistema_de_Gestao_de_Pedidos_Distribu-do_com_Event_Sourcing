package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class OrderStatusUpdatedEvent {
    private final String orderId;
    private final String customerId;
    private final String oldStatus;
    private final String newStatus;
    private final LocalDateTime timestamp;
    private final String correlationId;
    private final String causationId;

    @JsonCreator
    public OrderStatusUpdatedEvent(@JsonProperty("orderId") String orderId,
                                 @JsonProperty("customerId") String customerId,
                                 @JsonProperty("oldStatus") String oldStatus,
                                 @JsonProperty("newStatus") String newStatus,
                                 @JsonProperty("timestamp") LocalDateTime timestamp,
                                 @JsonProperty("correlationId") String correlationId,
                                 @JsonProperty("causationId") String causationId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.correlationId = correlationId;
        this.causationId = causationId;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public String getCausationId() { return causationId; }
}