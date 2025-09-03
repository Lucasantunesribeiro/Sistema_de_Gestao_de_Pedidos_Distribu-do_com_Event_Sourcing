package com.ordersystem.unified.inventory.dto;

/**
 * Reservation status enumeration
 */
public enum ReservationStatus {
    RESERVED("Items successfully reserved"),
    PARTIAL("Some items reserved, others unavailable"),
    FAILED("Reservation failed"),
    INSUFFICIENT_STOCK("Insufficient stock available"),
    EXPIRED("Reservation has expired"),
    RELEASED("Reservation has been released"),
    CONFIRMED("Reservation confirmed and stock committed"),
    CANCELLED("Reservation was cancelled");
    
    private final String description;
    
    ReservationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == RESERVED || this == PARTIAL;
    }
    
    public boolean isTerminal() {
        return this == EXPIRED || this == RELEASED || this == CONFIRMED || this == CANCELLED || this == FAILED;
    }
    
    public boolean canBeReleased() {
        return this == RESERVED || this == PARTIAL;
    }
    
    public boolean canBeConfirmed() {
        return this == RESERVED || this == PARTIAL;
    }
    
    @Override
    public String toString() {
        return name() + ": " + description;
    }
}