package com.ordersystem.unified.shared.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when a payment is refunded.
 * Used for event sourcing and compensation logic.
 */
public class PaymentRefundedEvent extends BaseEvent {

    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal originalAmount;
    private BigDecimal refundAmount;
    private String refundReason;
    private String refundTransactionId;
    private LocalDateTime refundedAt;

    // Default constructor
    public PaymentRefundedEvent() {
        super();
    }

    public PaymentRefundedEvent(String paymentId, String orderId, String customerId,
                               BigDecimal originalAmount, BigDecimal refundAmount,
                               String refundReason, String correlationId) {
        super(correlationId);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.originalAmount = originalAmount;
        this.refundAmount = refundAmount;
        this.refundReason = refundReason;
        this.refundedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public String getRefundTransactionId() {
        return refundTransactionId;
    }

    public void setRefundTransactionId(String refundTransactionId) {
        this.refundTransactionId = refundTransactionId;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    @Override
    public String toString() {
        return String.format(
            "PaymentRefundedEvent{paymentId='%s', orderId='%s', refundAmount=%s, reason='%s'}",
            paymentId, orderId, refundAmount, refundReason
        );
    }
}
