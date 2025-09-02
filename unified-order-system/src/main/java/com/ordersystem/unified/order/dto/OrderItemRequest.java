package com.ordersystem.unified.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for order items.
 */
public class OrderItemRequest {

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

    // Default constructor for JSON deserialization
    public OrderItemRequest() {}

    public OrderItemRequest(String productId, String productName, Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice() {
        if (quantity != null && unitPrice != null) {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return String.format("OrderItemRequest{productId='%s', productName='%s', quantity=%d, unitPrice=%s}",
                productId, productName, quantity, unitPrice);
    }
}