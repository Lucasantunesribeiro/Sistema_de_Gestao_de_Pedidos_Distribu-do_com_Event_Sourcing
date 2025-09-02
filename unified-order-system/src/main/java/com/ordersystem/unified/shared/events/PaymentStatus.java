package com.ordersystem.unified.shared.events;

/**
 * Enumeration of possible payment statuses in the system.
 */
public enum PaymentStatus {
    PENDING("Payment is pending processing"),
    PROCESSING("Payment is being processed"),
    COMPLETED("Payment completed successfully"),
    FAILED("Payment processing failed"),
    CANCELLED("Payment was cancelled"),
    REFUNDED("Payment has been refunded");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REFUNDED;
    }

    public boolean isSuccessful() {
        return this == COMPLETED;
    }
}