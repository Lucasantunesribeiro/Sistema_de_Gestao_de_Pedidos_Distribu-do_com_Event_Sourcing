package com.ordersystem.shared.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Command to release inventory reservation (compensation action)
 */
public class InventoryReleaseCommand {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("items")
    private List<OrderItem> items;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    // Default constructor for JSON deserialization
    public InventoryReleaseCommand() {
        this.timestamp = java.time.Instant.now().toString();
    }
    
    public InventoryReleaseCommand(String orderId, List<OrderItem> items, String reason) {
        this.orderId = orderId;
        this.items = items;
        this.reason = reason;
        this.timestamp = java.time.Instant.now().toString();
    }
    
    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return "InventoryReleaseCommand{" +
                "orderId='" + orderId + '\'' +
                ", items=" + items +
                ", reason='" + reason + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
} 