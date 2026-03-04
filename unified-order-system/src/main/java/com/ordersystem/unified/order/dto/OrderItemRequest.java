package com.ordersystem.unified.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ordersystem.unified.shared.validation.ValidationConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for order items.
 */
public class OrderItemRequest {

    @JsonProperty("productId")
    @Size(max = ValidationConstants.MAX_ID_LENGTH, message = ValidationConstants.MSG_ID_TOO_LONG)
    private String productId;

    @JsonProperty("productName")
    @NotBlank(message = "Product name cannot be blank")
    @Size(max = ValidationConstants.MAX_PRODUCT_NAME_LENGTH, message = "Product name exceeds maximum length")
    private String productName;

    @JsonProperty("quantity")
    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    @Max(value = ValidationConstants.MAX_QUANTITY, message = ValidationConstants.MSG_INVALID_QUANTITY)
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

    @com.fasterxml.jackson.annotation.JsonIgnore
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