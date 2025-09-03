package com.ordersystem.unified.inventory.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Inventory reservation response DTO
 */
public class ReservationResponse {
    
    private String reservationId;
    private String orderId;
    private ReservationStatus status;
    private String message;
    private List<ItemReservationResult> itemResults;
    private LocalDateTime reservationExpiry;
    private LocalDateTime createdAt;
    private String correlationId;
    
    // Constructors
    public ReservationResponse() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ReservationResponse(String reservationId, String orderId, ReservationStatus status, String message) {
        this();
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.status = status;
        this.message = message;
    }
    
    // Static factory methods
    public static ReservationResponse success(String reservationId, String orderId, List<ItemReservationResult> itemResults, LocalDateTime expiry) {
        ReservationResponse response = new ReservationResponse(reservationId, orderId, ReservationStatus.RESERVED, "Items reserved successfully");
        response.setItemResults(itemResults);
        response.setReservationExpiry(expiry);
        return response;
    }
    
    public static ReservationResponse partialSuccess(String reservationId, String orderId, List<ItemReservationResult> itemResults, LocalDateTime expiry) {
        ReservationResponse response = new ReservationResponse(reservationId, orderId, ReservationStatus.PARTIAL, "Some items could not be reserved");
        response.setItemResults(itemResults);
        response.setReservationExpiry(expiry);
        return response;
    }
    
    public static ReservationResponse failure(String reservationId, String orderId, String message) {
        return new ReservationResponse(reservationId, orderId, ReservationStatus.FAILED, message);
    }
    
    public static ReservationResponse insufficientStock(String reservationId, String orderId, List<ItemReservationResult> itemResults) {
        ReservationResponse response = new ReservationResponse(reservationId, orderId, ReservationStatus.INSUFFICIENT_STOCK, "Insufficient stock for requested items");
        response.setItemResults(itemResults);
        return response;
    }
    
    // Getters and Setters
    public String getReservationId() {
        return reservationId;
    }
    
    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public ReservationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<ItemReservationResult> getItemResults() {
        return itemResults;
    }
    
    public void setItemResults(List<ItemReservationResult> itemResults) {
        this.itemResults = itemResults;
    }
    
    public LocalDateTime getReservationExpiry() {
        return reservationExpiry;
    }
    
    public void setReservationExpiry(LocalDateTime reservationExpiry) {
        this.reservationExpiry = reservationExpiry;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public boolean isSuccess() {
        return status == ReservationStatus.RESERVED;
    }
    
    public boolean isPartialSuccess() {
        return status == ReservationStatus.PARTIAL;
    }
    
    public boolean hasAnyReservation() {
        return status == ReservationStatus.RESERVED || status == ReservationStatus.PARTIAL;
    }
    
    @Override
    public String toString() {
        return "ReservationResponse{" +
                "reservationId='" + reservationId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", itemResults=" + itemResults +
                ", reservationExpiry=" + reservationExpiry +
                ", createdAt=" + createdAt +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}