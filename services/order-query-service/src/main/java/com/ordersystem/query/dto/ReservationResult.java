package com.ordersystem.query.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Reservation result DTO with deferred support
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservationResult {
    
    private String status;
    private String message;
    private String reservationId;
    private Boolean isDeferred;
    
    public ReservationResult() {}
    
    public ReservationResult(String status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public static ReservationResult deferred(String message) {
        ReservationResult result = new ReservationResult("DEFERRED", message);
        result.setIsDeferred(true);
        return result;
    }
    
    public static ReservationResult success(String message, String reservationId) {
        ReservationResult result = new ReservationResult("SUCCESS", message);
        result.setReservationId(reservationId);
        return result;
    }
    
    public static ReservationResult failed(String message) {
        return new ReservationResult("FAILED", message);
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getReservationId() {
        return reservationId;
    }
    
    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }
    
    public Boolean getIsDeferred() {
        return isDeferred;
    }
    
    public void setIsDeferred(Boolean isDeferred) {
        this.isDeferred = isDeferred;
    }
}