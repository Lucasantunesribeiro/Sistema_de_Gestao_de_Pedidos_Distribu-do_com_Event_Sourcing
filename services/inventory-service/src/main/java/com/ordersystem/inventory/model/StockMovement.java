package com.ordersystem.inventory.model;

import java.time.LocalDateTime;

/**
 * Represents a stock movement for audit trail and analytics
 */
public class StockMovement {
    private String movementId;
    private String productId;
    private MovementType type;
    private int quantity;
    private String orderId;
    private String reason;
    private LocalDateTime timestamp;
    private int previousAvailable;
    private int previousReserved;
    private int newAvailable;
    private int newReserved;
    
    public enum MovementType {
        RESERVE,
        CONFIRM,
        RELEASE,
        RESTOCK,
        ADJUSTMENT,
        EXPIRY
    }
    
    public StockMovement() {}
    
    public StockMovement(String movementId, String productId, MovementType type, 
                        int quantity, String orderId, String reason,
                        int previousAvailable, int previousReserved,
                        int newAvailable, int newReserved) {
        this.movementId = movementId;
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.orderId = orderId;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
        this.previousAvailable = previousAvailable;
        this.previousReserved = previousReserved;
        this.newAvailable = newAvailable;
        this.newReserved = newReserved;
    }
    
    // Getters and setters
    public String getMovementId() { return movementId; }
    public void setMovementId(String movementId) { this.movementId = movementId; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public int getPreviousAvailable() { return previousAvailable; }
    public void setPreviousAvailable(int previousAvailable) { this.previousAvailable = previousAvailable; }
    
    public int getPreviousReserved() { return previousReserved; }
    public void setPreviousReserved(int previousReserved) { this.previousReserved = previousReserved; }
    
    public int getNewAvailable() { return newAvailable; }
    public void setNewAvailable(int newAvailable) { this.newAvailable = newAvailable; }
    
    public int getNewReserved() { return newReserved; }
    public void setNewReserved(int newReserved) { this.newReserved = newReserved; }
}