package com.ordersystem.unified.inventory.dto;

/**
 * Enumeration of inventory reservation statuses
 */
public enum ReservationStatus {
    RESERVED("Reserved"),
    PARTIAL("Partially Reserved"),
    FAILED("Failed"),
    INSUFFICIENT_STOCK("Insufficient Stock"),
    EXPIRED("Expired"),
    RELEASED("Released"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == RESERVED || this == PARTIAL;
    }

    public boolean canBeReleased() {
        return this == RESERVED || this == PARTIAL;
    }

    public boolean canBeConfirmed() {
        return this == RESERVED || this == PARTIAL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}