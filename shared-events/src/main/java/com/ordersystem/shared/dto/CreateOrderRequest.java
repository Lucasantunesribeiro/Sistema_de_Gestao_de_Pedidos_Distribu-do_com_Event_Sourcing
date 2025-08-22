package com.ordersystem.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateOrderRequest {
    
    @JsonProperty("customerId")
    @NotBlank(message = "Customer ID é obrigatório")
    @Size(min = 1, max = 255, message = "Customer ID deve ter entre 1 e 255 caracteres")
    private String customerId;
    
    @JsonProperty("items")
    @NotEmpty(message = "Lista de itens não pode estar vazia")
    @Valid
    private List<OrderItemRequest> items;

    // Default constructor
    public CreateOrderRequest() {}

    public CreateOrderRequest(String customerId, List<OrderItemRequest> items) {
        this.customerId = customerId;
        this.items = items;
    }

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}