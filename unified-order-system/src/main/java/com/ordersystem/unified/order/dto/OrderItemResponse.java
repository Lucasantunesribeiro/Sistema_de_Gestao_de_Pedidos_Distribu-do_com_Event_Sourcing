package com.ordersystem.unified.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Response DTO for order items.
 */
public class OrderItemResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("productId")
    private String productId;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("unitPrice")
    private BigDecimal unitPrice;

    @JsonProperty("totalPrice")
    private BigDecimal totalPrice;

    // Default constructor for JSON serialization
    public OrderItemResponse() {}

    public OrderItemResponse(Long id, String productId, String productName, Integer quantity,
                           BigDecimal unitPrice, BigDecimal totalPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    @Override
    public String toString() {
        return String.format("OrderItemResponse{id=%d, productId='%s', productName='%s', quantity=%d, unitPrice=%s, totalPrice=%s}",
                id, productId, productName, quantity, unitPrice, totalPrice);
    }
}