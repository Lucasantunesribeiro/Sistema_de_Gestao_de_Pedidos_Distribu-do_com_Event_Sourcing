package com.ordersystem.inventory.exception;

import java.time.LocalDateTime;

/**
 * Exception thrown when trying to operate on an expired reservation
 */
public class ReservationExpiredException extends InventoryException {
    
    private final String reservationId;
    private final String orderId;
    private final LocalDateTime expiredAt;
    
    public ReservationExpiredException(String reservationId, String orderId, LocalDateTime expiredAt) {
        super(String.format("Reservation %s for order %s expired at %s", 
              reservationId, orderId, expiredAt), "RESERVATION_EXPIRED");
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.expiredAt = expiredAt;
    }
    
    public String getReservationId() {
        return reservationId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }
}