package com.ordersystem.unified.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simplified request DTO for creating orders from frontend.
 */
public class SimpleOrderRequest {

    @JsonProperty("customerName")
    @NotBlank(message = "Customer name cannot be blank")
    private String customerName;

    @JsonProperty("items")
    @NotNull(message = "Items cannot be null")
    @NotEmpty(message = "Order must contain at least one item")
    private List<SimpleOrderItem> items;

    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    // Default constructor
    public SimpleOrderRequest() {}

    // Getters and Setters
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public List<SimpleOrderItem> getItems() { return items; }
    public void setItems(List<SimpleOrderItem> items) { this.items = items; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    /**
     * Convert to standard CreateOrderRequest format.
     */
    public CreateOrderRequest toCreateOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("frontend-customer"); // Default customer ID for frontend orders
        request.setCustomerName(this.customerName);
        
        List<OrderItemRequest> orderItems = this.items.stream()
            .map(item -> {
                OrderItemRequest orderItem = new OrderItemRequest();
                orderItem.setProductId("product-" + System.currentTimeMillis()); // Generate product ID
                orderItem.setProductName(item.getProductName());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setUnitPrice(item.getPrice());
                return orderItem;
            })
            .collect(java.util.stream.Collectors.toList());
        
        request.setItems(orderItems);
        return request;
    }

    public static class SimpleOrderItem {
        @JsonProperty("productName")
        @NotBlank(message = "Product name cannot be blank")
        private String productName;

        @JsonProperty("price")
        @NotNull(message = "Price cannot be null")
        private BigDecimal price;

        @JsonProperty("quantity")
        @NotNull(message = "Quantity cannot be null")
        private Integer quantity;

        // Default constructor
        public SimpleOrderItem() {}

        // Getters and Setters
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    @Override
    public String toString() {
        return String.format("SimpleOrderRequest{customerName='%s', itemCount=%d, totalAmount=%s}",
                customerName, items != null ? items.size() : 0, totalAmount);
    }
}