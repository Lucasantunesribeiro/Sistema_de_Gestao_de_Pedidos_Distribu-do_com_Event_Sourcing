package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentFailedEvent extends BaseEvent {
    
    @JsonProperty("orderId")
    private final String orderId;
    
    @JsonProperty("paymentId")
    private final String paymentId;
    
    @JsonProperty("amount")
    private final BigDecimal amount;
    
    @JsonProperty("reason")
    private final String reason;
    
    @JsonProperty("errorCode")
    private final String errorCode;
    
    @JsonProperty("failedAt")
    private final LocalDateTime failedAt;

    @JsonCreator
    public PaymentFailedEvent(@JsonProperty("orderId") String orderId,
                             @JsonProperty("paymentId") String paymentId,
                             @JsonProperty("amount") BigDecimal amount,
                             @JsonProperty("reason") String reason,
                             @JsonProperty("errorCode") String errorCode,
                             @JsonProperty("correlationId") String correlationId,
                             @JsonProperty("causationId") String causationId) {
        super(correlationId, causationId, "PAYMENT_FAILED");
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
        this.errorCode = errorCode;
        this.failedAt = LocalDateTime.now();
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getPaymentId() { return paymentId; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public String getErrorCode() { return errorCode; }
    public LocalDateTime getFailedAt() { return failedAt; }
}