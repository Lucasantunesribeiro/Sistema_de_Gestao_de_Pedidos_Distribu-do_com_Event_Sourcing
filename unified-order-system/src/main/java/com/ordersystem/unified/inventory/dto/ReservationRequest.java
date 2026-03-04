package com.ordersystem.unified.inventory.dto;

import com.ordersystem.unified.shared.validation.ValidationConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.util.List;

/**
 * Inventory reservation request DTO
 */
public class ReservationRequest {

    @NotBlank(message = "Order ID is required")
    @Size(max = ValidationConstants.MAX_ID_LENGTH, message = ValidationConstants.MSG_ID_TOO_LONG)
    private String orderId;

    @NotEmpty(message = "At least one item is required")
    @Size(min = 1, max = ValidationConstants.MAX_RESERVATION_ITEMS, message = "Too many items (maximum: " + ValidationConstants.MAX_RESERVATION_ITEMS + ")")
    @Valid
    private List<ItemReservation> items;

    private Duration reservationTimeout = Duration.ofMinutes(15); // Default 15 minutes

    @Size(max = ValidationConstants.MAX_ID_LENGTH, message = "Correlation ID too long")
    private String correlationId;

    @Size(max = ValidationConstants.MAX_ID_LENGTH, message = "Warehouse ID too long")
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