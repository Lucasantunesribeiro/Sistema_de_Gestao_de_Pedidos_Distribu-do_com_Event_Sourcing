package com.ordersystem.unified.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Duration;
import java.util.List;

/**
 * Inventory reservation request DTO
 */
public class ReservationRequest {
    
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<ItemReservation> items;
    
    private Duration reservationTimeout = Duration.ofMinutes(15); // Default 15 minutes
    
    private String correlationId;
    
    private String warehouseId;
    
    // Constructors
    public ReservationRequest() {}
    
    public ReservationRequest(String orderId, List<ItemReservation> items) {
        this.orderId = orderId;
        this.items = items;
    }
    
    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public List<ItemReservation> getItems() {
        return items;
    }
    
    public void setItems(List<ItemReservation> items) {
        this.items = items;
    }
    
    public Duration getReservationTimeout() {
        return reservationTimeout;
    }
    
    public void setReservationTimeout(Duration reservationTimeout) {
        this.reservationTimeout = reservationTimeout;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    @Override
    public String toString() {
        return "ReservationRequest{" +
                "orderId='" + orderId + '\'' +
                ", items=" + items +
                ", reservationTimeout=" + reservationTimeout +
                ", correlationId='" + correlationId + '\'' +
                ", warehouseId='" + warehouseId + '\'' +
                '}';
    }
}