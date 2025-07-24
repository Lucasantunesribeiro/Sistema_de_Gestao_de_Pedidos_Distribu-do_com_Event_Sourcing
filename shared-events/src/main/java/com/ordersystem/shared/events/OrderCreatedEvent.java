package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class OrderCreatedEvent {
    private final String orderId;
    private final String customerId;
    private final List<OrderItem> items;
    private final double totalAmount;
    private final LocalDateTime timestamp;

    @JsonCreator
    public OrderCreatedEvent(@JsonProperty("orderId") String orderId,
                           @JsonProperty("customerId") String customerId,
                           @JsonProperty("items") List<OrderItem> items,
                           @JsonProperty("totalAmount") double totalAmount,
                           @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public static class OrderItem {
        private final String productId;
        private final String productName;
        private final int quantity;
        private final double price;

        @JsonCreator
        public OrderItem(@JsonProperty("productId") String productId,
                        @JsonProperty("productName") String productName,
                        @JsonProperty("quantity") int quantity,
                        @JsonProperty("price") double price) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }

        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
    }
}