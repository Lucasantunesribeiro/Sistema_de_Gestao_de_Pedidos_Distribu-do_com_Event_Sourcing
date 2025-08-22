package com.ordersystem.query.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Order summary DTO with partial data support
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSummary {
    
    private String orderId;
    private String paymentStatus;
    private String inventoryStatus;
    private String orderStatus;
    private Boolean hasPartialData;
    private Long lastUpdated;
    
    public OrderSummary() {}
    
    public OrderSummary(String orderId) {
        this.orderId = orderId;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getInventoryStatus() {
        return inventoryStatus;
    }
    
    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }
    
    public String getOrderStatus() {
        return orderStatus;
    }
    
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public Boolean hasPartialData() {
        return hasPartialData != null && hasPartialData;
    }
    
    public Boolean getHasPartialData() {
        return hasPartialData;
    }
    
    public void setHasPartialData(Boolean hasPartialData) {
        this.hasPartialData = hasPartialData;
    }
    
    public Long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}