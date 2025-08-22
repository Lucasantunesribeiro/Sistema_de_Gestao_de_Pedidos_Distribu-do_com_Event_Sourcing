package com.ordersystem.payment.model;

import com.ordersystem.shared.events.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Payment {
    
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentMethod;
    private String gatewayTransactionId;
    private String failureReason;
    private String errorCode;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private LocalDateTime updatedAt;
    private int retryCount;
    private String correlationId;

    // Default constructor
    public Payment() {
        this.paymentId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
        this.retryCount = 0;
    }

    // Constructor for new payment
    public Payment(String orderId, BigDecimal amount, String paymentMethod, String correlationId) {
        this();
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.correlationId = correlationId;
    }

    // Business methods
    public void startProcessing() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Cannot start processing payment in status: " + this.status);
        }
        
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void approve(String gatewayTransactionId) {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Cannot approve payment in status: " + this.status);
        }
        
        this.status = PaymentStatus.APPROVED;
        this.gatewayTransactionId = gatewayTransactionId;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.failureReason = null;
        this.errorCode = null;
    }

    public void decline(String reason, String errorCode) {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Cannot decline payment in status: " + this.status);
        }
        
        this.status = PaymentStatus.DECLINED;
        this.failureReason = reason;
        this.errorCode = errorCode;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String reason, String errorCode) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.errorCode = errorCode;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == PaymentStatus.APPROVED) {
            throw new IllegalStateException("Cannot cancel approved payment");
        }
        
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canRetry() {
        return this.retryCount < 3 && 
               (this.status == PaymentStatus.FAILED || this.status == PaymentStatus.DECLINED);
    }

    public boolean isProcessable() {
        return this.status == PaymentStatus.PENDING || 
               (this.status == PaymentStatus.FAILED && canRetry());
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.APPROVED || 
               this.status == PaymentStatus.DECLINED ||
               this.status == PaymentStatus.CANCELLED;
    }

    // Getters and Setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public void setGatewayTransactionId(String gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}