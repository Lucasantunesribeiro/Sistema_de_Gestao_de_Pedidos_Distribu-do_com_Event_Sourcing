package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class PaymentProcessedEvent {
    private final String orderId;
    private final String paymentId;
    private final String paymentStatus;
    private final double amount;
    private final LocalDateTime timestamp;

    @JsonCreator
    public PaymentProcessedEvent(@JsonProperty("orderId") String orderId,
                               @JsonProperty("paymentId") String paymentId,
                               @JsonProperty("paymentStatus") String paymentStatus,
                               @JsonProperty("amount") double amount,
                               @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getOrderId() { return orderId; }
    public String getPaymentId() { return paymentId; }
    public String getPaymentStatus() { return paymentStatus; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}