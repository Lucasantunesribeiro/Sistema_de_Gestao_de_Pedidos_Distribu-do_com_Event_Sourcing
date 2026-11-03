package com.ordersystem.unified.domain.events;

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
        return this == CANCELLED || this == FAILED;
    }

    public boolean isSuccessful() {
        return this == CONFIRMED;
    }

    /**
     * Máquina de estados do pedido: única fonte de verdade para transições válidas.
     * Usada pelo agregado event-sourced e por {@code OrderBusinessRules}.
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == null || isTerminal()) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == INVENTORY_RESERVED || target == CANCELLED || target == FAILED;
            case INVENTORY_RESERVED -> target == PAYMENT_PROCESSING || target == CANCELLED || target == FAILED;
            case PAYMENT_PROCESSING -> target == CONFIRMED || target == FAILED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case CANCELLED, FAILED -> false;
        };
    }
}
