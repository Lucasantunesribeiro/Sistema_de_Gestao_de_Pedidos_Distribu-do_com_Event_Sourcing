package com.ordersystem.unified.inventory.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stock entity for inventory tracking
 */
@Entity
@Table(name = "stocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "warehouse_id"})
})
public class Stock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId = "DEFAULT";
    
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity = 0;
    
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;
    
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity = 0;
    
    @Column(name = "minimum_stock")
    private Integer minimumStock = 0;
    
    @Column(name = "maximum_stock")
    private Integer maximumStock;
    
    @Column(name = "reorder_point")
    private Integer reorderPoint;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "last_stock_update")
    private LocalDateTime lastStockUpdate;
    
    // Constructors
    public Stock() {}
    
    public Stock(Product product, String warehouseId, Integer totalQuantity) {
        this.product = product;
        this.warehouseId = warehouseId;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity;
        this.reservedQuantity = 0;
        this.lastStockUpdate = LocalDateTime.now();
    }
    
    // Business methods
    public boolean canReserve(Integer quantity) {
        return availableQuantity >= quantity;
    }
    
    public void reserveStock(Integer quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient stock to reserve: requested=" + quantity + ", available=" + availableQuantity);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
        this.lastStockUpdate = LocalDateTime.now();
    }
    
    public void releaseReservation(Integer quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more than reserved: requested=" + quantity + ", reserved=" + reservedQuantity);
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
        this.lastStockUpdate = LocalDateTime.now();
    }
    
    public void confirmReservation(Integer quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot confirm more than reserved: requested=" + quantity + ", reserved=" + reservedQuantity);
        }
        this.reservedQuantity -= quantity;
        this.totalQuantity -= quantity;
        this.lastStockUpdate = LocalDateTime.now();
    }
    
    public void addStock(Integer quantity) {
        this.totalQuantity += quantity;
        this.availableQuantity += quantity;
        this.lastStockUpdate = LocalDateTime.now();
    }
    
    public boolean isLowStock() {
        return minimumStock != null && availableQuantity <= minimumStock;
    }
    
    public boolean needsReorder() {
        return reorderPoint != null && availableQuantity <= reorderPoint;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public String getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
    
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
    
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    public Integer getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public Integer getMinimumStock() {
        return minimumStock;
    }
    
    public void setMinimumStock(Integer minimumStock) {
        this.minimumStock = minimumStock;
    }
    
    public Integer getMaximumStock() {
        return maximumStock;
    }
    
    public void setMaximumStock(Integer maximumStock) {
        this.maximumStock = maximumStock;
    }
    
    public Integer getReorderPoint() {
        return reorderPoint;
    }
    
    public void setReorderPoint(Integer reorderPoint) {
        this.reorderPoint = reorderPoint;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastStockUpdate() {
        return lastStockUpdate;
    }
    
    public void setLastStockUpdate(LocalDateTime lastStockUpdate) {
        this.lastStockUpdate = lastStockUpdate;
    }
    
    @Override
    public String toString() {
        return "Stock{" +
                "id='" + id + '\'' +
                ", productId='" + (product != null ? product.getId() : null) + '\'' +
                ", warehouseId='" + warehouseId + '\'' +
                ", availableQuantity=" + availableQuantity +
                ", reservedQuantity=" + reservedQuantity +
                ", totalQuantity=" + totalQuantity +
                ", lastStockUpdate=" + lastStockUpdate +
                '}';
    }
}