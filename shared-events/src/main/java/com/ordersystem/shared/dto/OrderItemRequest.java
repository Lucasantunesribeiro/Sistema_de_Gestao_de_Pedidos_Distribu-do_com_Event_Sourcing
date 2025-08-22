package com.ordersystem.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class OrderItemRequest {
    
    @JsonProperty("productId")
    @NotBlank(message = "Product ID é obrigatório")
    @Size(min = 1, max = 255, message = "Product ID deve ter entre 1 e 255 caracteres")
    private String productId;
    
    @JsonProperty("productName")
    @NotBlank(message = "Nome do produto é obrigatório")
    @Size(min = 1, max = 255, message = "Nome do produto deve ter entre 1 e 255 caracteres")
    private String productName;
    
    @JsonProperty("quantity")
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantity;
    
    @JsonProperty("price")
    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    private BigDecimal price;

    // Default constructor
    public OrderItemRequest() {}

    public OrderItemRequest(String productId, String productName, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}