package com.ordersystem.unified.shared.events;

/**
 * Enumeration of possible inventory statuses in the system.
 */
public enum InventoryStatus {
    AVAILABLE("Product is available in stock"),
    RESERVED("Product quantity has been reserved"),
    OUT_OF_STOCK("Product is out of stock"),
    INSUFFICIENT("Insufficient quantity available"),
    RELEASED("Reserved quantity has been released back to available stock");

    private final String description;

    InventoryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    public boolean isReserved() {
        return this == RESERVED;
    }
}