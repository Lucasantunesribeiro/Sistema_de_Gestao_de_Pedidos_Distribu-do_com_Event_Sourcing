package com.ordersystem.unified.shared.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents an item within an order.
 * Used across all modules for order processing.
 */
public class OrderItem {
    
    @JsonProperty("productId")
    @NotBlank(message = "Product ID cannot be blank")
    private String productId;
    
    @JsonProperty("productName")
    @NotBlank(message = "Product name cannot be blank")
    private String productName;
    
    @JsonProperty("quantity")
    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
    
    @JsonProperty("unitPrice")
    @NotNull(message = "Unit price cannot be null")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;
    
    @JsonProperty("totalPrice")
    private BigDecimal totalPrice;
    
    // Default constructor for JSON deserialization
    public OrderItem() {}
    
    public OrderItem(String productId, String productName, Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { 
        this.quantity = quantity; 
        if (this.unitPrice != null && quantity != null) {
            this.totalPrice = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
    
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { 
        this.unitPrice = unitPrice; 
        if (this.quantity != null && unitPrice != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
    
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(productId, orderItem.productId) &&
               Objects.equals(quantity, orderItem.quantity) &&
               Objects.equals(unitPrice, orderItem.unitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity, unitPrice);
    }

    @Override
    public String toString() {
        return String.format("OrderItem{productId='%s', productName='%s', quantity=%d, unitPrice=%s, totalPrice=%s}",
                productId, productName, quantity, unitPrice, totalPrice);
    }
}