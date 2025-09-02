package com.ordersystem.unified.shared.events;

/**
 * Enumeration of possible order statuses in the system.
 */
public enum OrderStatus {
    PENDING("Order created, awaiting processing"),
    INVENTORY_RESERVED("Inventory has been reserved"),
    PAYMENT_PROCESSING("Payment is being processed"),
    CONFIRMED("Order confirmed and payment successful"),
    CANCELLED("Order has been cancelled"),
    FAILED("Order processing failed");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == CONFIRMED || this == CANCELLED || this == FAILED;
    }

    public boolean isSuccessful() {
        return this == CONFIRMED;
    }
}