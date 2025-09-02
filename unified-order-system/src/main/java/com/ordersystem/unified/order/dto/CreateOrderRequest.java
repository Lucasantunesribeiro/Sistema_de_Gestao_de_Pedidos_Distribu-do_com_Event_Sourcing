package com.ordersystem.unified.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for creating a new order.
 */
public class CreateOrderRequest {

    @JsonProperty("customerId")
    @NotBlank(message = "Customer ID cannot be blank")
    private String customerId;

    @JsonProperty("customerName")
    @NotBlank(message = "Customer name cannot be blank")
    private String customerName;

    @JsonProperty("items")
    @NotNull(message = "Items cannot be null")
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @JsonProperty("correlationId")
    private String correlationId;

    // Default constructor for JSON deserialization
    public CreateOrderRequest() {}

    public CreateOrderRequest(String customerId, String customerName, List<OrderItemRequest> items) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.items = items;
    }

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    @Override
    public String toString() {
        return String.format("CreateOrderRequest{customerId='%s', customerName='%s', itemCount=%d}",
                customerId, customerName, items != null ? items.size() : 0);
    }
}