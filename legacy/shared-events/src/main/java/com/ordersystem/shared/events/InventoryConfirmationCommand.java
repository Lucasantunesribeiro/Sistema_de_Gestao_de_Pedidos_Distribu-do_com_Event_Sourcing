package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class InventoryConfirmationCommand {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("items")
    private List<OrderItem> items;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    // Default constructor for JSON deserialization
    public InventoryConfirmationCommand() {}
    
    public InventoryConfirmationCommand(String orderId, List<OrderItem> items) {
        this.orderId = orderId;
        this.items = items;
        this.timestamp = java.time.Instant.now().toString();
    }
    
    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
} 