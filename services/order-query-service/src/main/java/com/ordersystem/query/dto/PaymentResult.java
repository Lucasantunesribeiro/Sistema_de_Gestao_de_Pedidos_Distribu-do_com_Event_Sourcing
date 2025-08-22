package com.ordersystem.query.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Payment result DTO with intelligent fallback support
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResult {
    
    private String status;
    private String message;
    private String transactionId;
    private Boolean isFromFallback;
    
    public PaymentResult() {}
    
    public PaymentResult(String status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public PaymentResult(String status, String message, String transactionId) {
        this.status = status;
        this.message = message;
        this.transactionId = transactionId;
    }
    
    public static PaymentResult queued(String message) {
        PaymentResult result = new PaymentResult("QUEUED", message);
        result.setIsFromFallback(true);
        return result;
    }
    
    public static PaymentResult pendingApproval(String message) {
        PaymentResult result = new PaymentResult("PENDING_APPROVAL", message);
        result.setIsFromFallback(true);
        return result;
    }
    
    public static PaymentResult success(String message, String transactionId) {
        return new PaymentResult("SUCCESS", message, transactionId);
    }
    
    public static PaymentResult failed(String message) {
        return new PaymentResult("FAILED", message);
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public Boolean getIsFromFallback() {
        return isFromFallback;
    }
    
    public void setIsFromFallback(Boolean isFromFallback) {
        this.isFromFallback = isFromFallback;
    }
}