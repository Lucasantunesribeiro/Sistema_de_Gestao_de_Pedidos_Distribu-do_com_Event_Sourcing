package com.ordersystem.unified.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Item reservation DTO for inventory requests
 */
public class ItemReservation {
    
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    private String warehouseId;
    
    // Constructors
    public ItemReservation() {}
    
    public ItemReservation(String productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
    
    public ItemReservation(String productId, Integer quantity, String warehouseId) {
        this.productId = productId;
        this.quantity = quantity;
        this.warehouseId = warehouseId;
    }
    
    // Getters and Setters
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
    
    public String getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    @Override
    public String toString() {
        return "ItemReservation{" +
                "productId='" + productId + '\'' +
                ", quantity=" + quantity +
                ", warehouseId='" + warehouseId + '\'' +
                '}';
    }
}