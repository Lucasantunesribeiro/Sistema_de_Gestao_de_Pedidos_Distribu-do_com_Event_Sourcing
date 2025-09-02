package com.ordersystem.unified.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Inventory entity representing product stock levels in the system.
 */
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @Column(name = "product_id", length = 255)
    private String productId;

    @NotBlank(message = "Product name cannot be blank")
    @Column(name = "product_name", nullable = false)
    private String productName;

    @NotNull(message = "Available quantity cannot be null")
    @Min(value = 0, message = "Available quantity cannot be negative")
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @NotNull(message = "Reserved quantity cannot be null")
    @Min(value = 0, message = "Reserved quantity cannot be negative")
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "reorder_level")
    private Integer reorderLevel;

    @Column(name = "max_stock_level")
    private Integer maxStockLevel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    protected Inventory() {}

    public Inventory(String productId, String productName, Integer availableQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
    }

    public Inventory(String productId, String productName, Integer availableQuantity, 
                    Integer reorderLevel, Integer maxStockLevel) {
        this.productId = productId;
        this.productName = productName;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
        this.reorderLevel = reorderLevel;
        this.maxStockLevel = maxStockLevel;
    }

    // Business methods
    public boolean canReserve(Integer quantity) {
        return availableQuantity >= quantity;
    }

    public void reserve(Integer quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalArgumentException(
                String.format("Cannot reserve %d units of product %s. Available: %d", 
                             quantity, productId, availableQuantity));
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    public void release(Integer quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalArgumentException(
                String.format("Cannot release %d units of product %s. Reserved: %d", 
                             quantity, productId, reservedQuantity));
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    public void confirmReservation(Integer quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalArgumentException(
                String.format("Cannot confirm %d units of product %s. Reserved: %d", 
                             quantity, productId, reservedQuantity));
        }
        this.reservedQuantity -= quantity;
        // Note: Available quantity is not increased as the items are now sold
    }

    public void addStock(Integer quantity) {
        this.availableQuantity += quantity;
    }

    public void removeStock(Integer quantity) {
        if (availableQuantity < quantity) {
            throw new IllegalArgumentException(
                String.format("Cannot remove %d units of product %s. Available: %d", 
                             quantity, productId, availableQuantity));
        }
        this.availableQuantity -= quantity;
    }

    public Integer getTotalQuantity() {
        return availableQuantity + reservedQuantity;
    }

    public boolean isLowStock() {
        return reorderLevel != null && availableQuantity <= reorderLevel;
    }

    public boolean isOutOfStock() {
        return availableQuantity == 0;
    }

    public boolean isOverStock() {
        return maxStockLevel != null && getTotalQuantity() > maxStockLevel;
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }

    public Integer getMaxStockLevel() { return maxStockLevel; }
    public void setMaxStockLevel(Integer maxStockLevel) { this.maxStockLevel = maxStockLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(productId, inventory.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return String.format("Inventory{productId='%s', productName='%s', available=%d, reserved=%d, total=%d}",
                productId, productName, availableQuantity, reservedQuantity, getTotalQuantity());
    }
}