package com.ordersystem.query.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Stock level DTO with cache support
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockLevel {
    
    private String productId;
    private Integer quantity;
    private Boolean isStale;
    private Boolean isEstimated;
    private Long lastUpdated;
    
    public StockLevel() {}
    
    public StockLevel(String productId, Integer quantity, Boolean isStale) {
        this.productId = productId;
        this.quantity = quantity;
        this.isStale = isStale;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public static StockLevel limited(String productId, Integer conservativeQuantity) {
        StockLevel level = new StockLevel(productId, conservativeQuantity, false);
        level.setIsEstimated(true);
        return level;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Boolean isStale() {
        return isStale != null && isStale;
    }
    
    public Boolean getIsStale() {
        return isStale;
    }
    
    public void setIsStale(Boolean isStale) {
        this.isStale = isStale;
    }
    
    public Boolean isEstimated() {
        return isEstimated != null && isEstimated;
    }
    
    public Boolean getIsEstimated() {
        return isEstimated;
    }
    
    public void setIsEstimated(Boolean isEstimated) {
        this.isEstimated = isEstimated;
    }
    
    public Long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}