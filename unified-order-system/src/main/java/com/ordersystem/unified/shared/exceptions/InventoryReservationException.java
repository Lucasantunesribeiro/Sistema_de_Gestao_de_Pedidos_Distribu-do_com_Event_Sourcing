package com.ordersystem.unified.shared.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when inventory reservation fails.
 */
public class InventoryReservationException extends OrderSystemException {
    
    public InventoryReservationException(String orderId, String reason) {
        super(String.format("Failed to reserve inventory for order %s: %s", orderId, reason),
              "INVENTORY_RESERVATION_FAILED", 
              HttpStatus.CONFLICT, 
              orderId, reason);
    }

    public InventoryReservationException(String orderId, String reason, Throwable cause) {
        super(String.format("Failed to reserve inventory for order %s: %s", orderId, reason),
              "INVENTORY_RESERVATION_FAILED", 
              HttpStatus.CONFLICT, 
              cause, 
              orderId, reason);
    }
}