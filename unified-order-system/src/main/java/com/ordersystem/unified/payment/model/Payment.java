package com.ordersystem.unified.payment.model;

import com.ordersystem.unified.shared.events.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payment entity representing a payment transaction in the system.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @NotBlank(message = "Order ID cannot be blank")
    @Column(name = "order_id", nullable = false)
    private String orderId;

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "correlation_id")
    private String correlationId;

    // Default constructor for JPA
    protected Payment() {}

    public Payment(String id, String orderId, BigDecimal amount) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.paymentMethod = "CREDIT_CARD"; // Default payment method
    }

    public Payment(String id, String orderId, BigDecimal amount, String paymentMethod) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.paymentMethod = paymentMethod;
    }

    // Business methods
    public void markAsCompleted(String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.transactionId = transactionId;
        this.failureReason = null;
        this.errorCode = null;
    }

    public void markAsFailed(String reason, String errorCode) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.errorCode = errorCode;
        this.transactionId = null;
    }

    public void markAsPending() {
        this.status = PaymentStatus.PENDING;
        this.failureReason = null;
        this.errorCode = null;
    }

    public boolean isCompleted() {
        return PaymentStatus.COMPLETED.equals(status);
    }

    public boolean isFailed() {
        return PaymentStatus.FAILED.equals(status);
    }

    public boolean isPending() {
        return PaymentStatus.PENDING.equals(status);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Payment{id='%s', orderId='%s', amount=%s, status=%s, transactionId='%s'}",
                id, orderId, amount, status, transactionId);
    }
}