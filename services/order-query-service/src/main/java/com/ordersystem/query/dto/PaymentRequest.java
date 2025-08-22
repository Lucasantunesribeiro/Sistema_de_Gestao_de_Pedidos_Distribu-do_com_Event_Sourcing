package com.ordersystem.query.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Payment request DTO
 */
public class PaymentRequest {
    
    @NotBlank
    private String orderId;
    
    @Positive
    private Double amount;
    
    @NotBlank
    private String currency;
    
    public PaymentRequest() {}
    
    public PaymentRequest(String orderId, Double amount, String currency) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}