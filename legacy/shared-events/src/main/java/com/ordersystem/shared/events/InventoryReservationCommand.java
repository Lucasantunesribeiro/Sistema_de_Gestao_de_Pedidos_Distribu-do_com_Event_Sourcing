package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Command to reserve inventory for an order
 */
public class InventoryReservationCommand {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("customerId")
    private String customerId;
    
    @JsonProperty("items")
    private List<OrderItem> items;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    // Default constructor for Jackson
    public InventoryReservationCommand() {
        this.timestamp = java.time.Instant.now().toString();
    }
    
    public InventoryReservationCommand(String orderId, String customerId, List<OrderItem> items) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.timestamp = java.time.Instant.now().toString();
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return "InventoryReservationCommand{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", items=" + items +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}