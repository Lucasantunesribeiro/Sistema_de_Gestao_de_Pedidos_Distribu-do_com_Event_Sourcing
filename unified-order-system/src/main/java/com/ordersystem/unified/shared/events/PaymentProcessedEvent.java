package com.ordersystem.unified.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Event published when a payment is processed.
 * Used for internal communication between modules.
 */
public class PaymentProcessedEvent extends BaseEvent {
    
    @JsonProperty("paymentId")
    private final String paymentId;
    
    @JsonProperty("orderId")
    private final String orderId;
    
    @JsonProperty("customerId")
    private final String customerId;
    
    @JsonProperty("amount")
    private final BigDecimal amount;
    
    @JsonProperty("status")
    private final String status;
    
    @JsonProperty("transactionId")
    private final String transactionId;

    @JsonCreator
    public PaymentProcessedEvent(@JsonProperty("paymentId") String paymentId,
                               @JsonProperty("orderId") String orderId,
                               @JsonProperty("customerId") String customerId,
                               @JsonProperty("amount") BigDecimal amount,
                               @JsonProperty("status") String status,
                               @JsonProperty("transactionId") String transactionId,
                               @JsonProperty("correlationId") String correlationId,
                               @JsonProperty("causationId") String causationId) {
        super(correlationId, causationId, "PAYMENT_PROCESSED");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.transactionId = transactionId;
    }

    // Convenience constructor
    public PaymentProcessedEvent(String paymentId, String orderId, String customerId, 
                               BigDecimal amount, String status, String transactionId) {
        this(paymentId, orderId, customerId, amount, status, transactionId, null, null);
    }

    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getTransactionId() { return transactionId; }

    @Override
    public String toString() {
        return String.format("PaymentProcessedEvent{paymentId='%s', orderId='%s', amount=%s, status='%s'}",
                paymentId, orderId, amount, status);
    }
}