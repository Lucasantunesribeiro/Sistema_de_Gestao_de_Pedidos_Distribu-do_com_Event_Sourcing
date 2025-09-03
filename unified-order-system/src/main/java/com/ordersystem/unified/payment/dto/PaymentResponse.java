package com.ordersystem.unified.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment processing results
 */
public class PaymentResponse {

    @JsonProperty("paymentId")
    private String paymentId;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("status")
    private PaymentStatus status;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("processedAt")
    private LocalDateTime processedAt;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("errorMessage")
    private String errorMessage;

    // Default constructor
    public PaymentResponse() {}

    // Constructor for successful payment
    public PaymentResponse(String paymentId, String orderId, PaymentStatus status, 
                          BigDecimal amount, String transactionId, LocalDateTime processedAt,
                          String correlationId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.status = status;
        this.amount = amount;
        this.transactionId = transactionId;
        this.processedAt = processedAt;
        this.correlationId = correlationId;
    }

    // Constructor for failed payment
    public PaymentResponse(String paymentId, String orderId, PaymentStatus status, 
                          String errorMessage, String correlationId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.correlationId = correlationId;
        this.processedAt = LocalDateTime.now();
    }

    // Getters and setters
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

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return status == PaymentStatus.COMPLETED;
    }

    @Override
    public String toString() {
        return "PaymentResponse{" +
                "paymentId='" + paymentId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", status=" + status +
                ", amount=" + amount +
                ", transactionId='" + transactionId + '\'' +
                ", processedAt=" + processedAt +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}