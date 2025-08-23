package com.ordersystem.inventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    
    @Id
    private String productId;
    
    @Column(nullable = false)
    private String productName;
    
    @Column(nullable = false)
    private int availableStock;
    
    @Column(nullable = false)
    private int reservedStock;
    
    @Column(nullable = false)
    private double price;
    
    private String location;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public InventoryItem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public InventoryItem(String productId, String productName, int availableStock, double price, String location) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.availableStock = availableStock;
        this.reservedStock = 0;
        this.price = price;
        this.location = location;
    }

    // Business methods
    public boolean canReserve(int quantity) {
        return availableStock >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient stock to reserve " + quantity + " of " + productId);
        }
        availableStock -= quantity;
        reservedStock += quantity;
        updatedAt = LocalDateTime.now();
    }

    public void release(int quantity) {
        if (reservedStock < quantity) {
            throw new IllegalStateException("Cannot release " + quantity + " - only " + reservedStock + " reserved");
        }
        reservedStock -= quantity;
        availableStock += quantity;
        updatedAt = LocalDateTime.now();
    }

    public void confirmReservation(int quantity) {
        if (reservedStock < quantity) {
            throw new IllegalStateException("Cannot confirm " + quantity + " - only " + reservedStock + " reserved");
        }
        reservedStock -= quantity;
        updatedAt = LocalDateTime.now();
    }
    
    public void confirm(int quantity) {
        confirmReservation(quantity);
    }

    public void addStock(int quantity) {
        availableStock += quantity;
        updatedAt = LocalDateTime.now();
    }

    public int getTotalStock() {
        return availableStock + reservedStock;
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getAvailableStock() { return availableStock; }
    public void setAvailableStock(int availableStock) { 
        this.availableStock = availableStock;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Alias methods for compatibility with InventoryService
    public int getAvailableQuantity() { return availableStock; }
    public void setAvailableQuantity(int quantity) { setAvailableStock(quantity); }

    public int getReservedStock() { return reservedStock; }
    public void setReservedStock(int reservedStock) { 
        this.reservedStock = reservedStock;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Alias methods for compatibility with InventoryService
    public int getReservedQuantity() { return reservedStock; }
    public void setReservedQuantity(int quantity) { setReservedStock(quantity); }

    public double getPrice() { return price; }
    public void setPrice(double price) { 
        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}