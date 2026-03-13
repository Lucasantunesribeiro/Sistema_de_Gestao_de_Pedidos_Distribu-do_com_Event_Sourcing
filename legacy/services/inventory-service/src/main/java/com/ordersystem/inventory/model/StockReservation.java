package com.ordersystem.inventory.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a stock reservation with timeout and compensation logic
 */
public class StockReservation {
    private String reservationId;
    private String orderId;
    private String customerId;
    private List<ReservedItem> reservedItems;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private ReservationStatus status;
    
    public enum ReservationStatus {
        PENDING,
        CONFIRMED,
        RELEASED,
        EXPIRED
    }
    
    public static class ReservedItem {
        private String productId;
        private String productName;
        private int quantity;
        
        public ReservedItem() {}
        
        public ReservedItem(String productId, String productName, int quantity) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
        }
        
        // Getters and setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
    
    public StockReservation() {}
    
    public StockReservation(String reservationId, String orderId, String customerId, 
                           List<ReservedItem> reservedItems, int timeoutMinutes) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.reservedItems = reservedItems;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);
        this.status = ReservationStatus.PENDING;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) && status == ReservationStatus.PENDING;
    }
    
    public void confirm() {
        if (status == ReservationStatus.PENDING && !isExpired()) {
            this.status = ReservationStatus.CONFIRMED;
        }
    }
    
    public void release() {
        if (status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED) {
            this.status = ReservationStatus.RELEASED;
        }
    }
    
    public void expire() {
        if (status == ReservationStatus.PENDING) {
            this.status = ReservationStatus.EXPIRED;
        }
    }
    
    // Getters and setters
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public List<ReservedItem> getReservedItems() { return reservedItems; }
    public void setReservedItems(List<ReservedItem> reservedItems) { this.reservedItems = reservedItems; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
}