package com.ordersystem.unified.order.dto;

import java.math.BigDecimal;

/**
 * Order statistics DTO for dashboard
 */
public class OrderStatistics {
    
    private long totalOrders;
    private long confirmedOrders;
    private long cancelledOrders;
    private long pendingOrders;
    private BigDecimal totalRevenue;
    
    // Constructors
    public OrderStatistics() {}
    
    public OrderStatistics(long totalOrders, long confirmedOrders, long cancelledOrders, 
                          long pendingOrders, BigDecimal totalRevenue) {
        this.totalOrders = totalOrders;
        this.confirmedOrders = confirmedOrders;
        this.cancelledOrders = cancelledOrders;
        this.pendingOrders = pendingOrders;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }
    
    // Getters and Setters
    public long getTotalOrders() {
        return totalOrders;
    }
    
    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }
    
    public long getConfirmedOrders() {
        return confirmedOrders;
    }
    
    public void setConfirmedOrders(long confirmedOrders) {
        this.confirmedOrders = confirmedOrders;
    }
    
    public long getCancelledOrders() {
        return cancelledOrders;
    }
    
    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }
    
    public long getPendingOrders() {
        return pendingOrders;
    }
    
    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }
    
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }
    
    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
    
    // Calculated properties
    public double getConfirmationRate() {
        return totalOrders > 0 ? (double) confirmedOrders / totalOrders * 100 : 0.0;
    }
    
    public double getCancellationRate() {
        return totalOrders > 0 ? (double) cancelledOrders / totalOrders * 100 : 0.0;
    }
    
    public BigDecimal getAverageOrderValue() {
        return confirmedOrders > 0 ? totalRevenue.divide(BigDecimal.valueOf(confirmedOrders), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }
    
    @Override
    public String toString() {
        return "OrderStatistics{" +
                "totalOrders=" + totalOrders +
                ", confirmedOrders=" + confirmedOrders +
                ", cancelledOrders=" + cancelledOrders +
                ", pendingOrders=" + pendingOrders +
                ", totalRevenue=" + totalRevenue +
                ", confirmationRate=" + getConfirmationRate() + "%" +
                ", cancellationRate=" + getCancellationRate() + "%" +
                ", averageOrderValue=" + getAverageOrderValue() +
                '}';
    }
}