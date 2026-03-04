package com.ordersystem.unified.shared.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when an order is cancelled.
 * Used for event sourcing and audit trail.
 */
public class OrderCancelledEvent extends BaseEvent {

    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private String cancellationReason;
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    private String reservationId;
    private String paymentId;
    private boolean paymentRefunded;
    private boolean inventoryReleased;

    // Default constructor for serialization
    public OrderCancelledEvent() {
        super();
    }

    public OrderCancelledEvent(String orderId, String customerId, BigDecimal totalAmount,
                              String cancellationReason, String correlationId) {
        super(correlationId);
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = LocalDateTime.now();
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String orderId;
        private String customerId;
        private BigDecimal totalAmount;
        private String cancellationReason;
        private String correlationId;
        private String cancelledBy;
        private String reservationId;
        private String paymentId;
        private boolean paymentRefunded;
        private boolean inventoryReleased;

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder cancellationReason(String cancellationReason) {
            this.cancellationReason = cancellationReason;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder cancelledBy(String cancelledBy) {
            this.cancelledBy = cancelledBy;
            return this;
        }

        public Builder reservationId(String reservationId) {
            this.reservationId = reservationId;
            return this;
        }

        public Builder paymentId(String paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder paymentRefunded(boolean paymentRefunded) {
            this.paymentRefunded = paymentRefunded;
            return this;
        }

        public Builder inventoryReleased(boolean inventoryReleased) {
            this.inventoryReleased = inventoryReleased;
            return this;
        }

        public OrderCancelledEvent build() {
            OrderCancelledEvent event = new OrderCancelledEvent(
                orderId, customerId, totalAmount, cancellationReason, correlationId
            );
            event.cancelledBy = this.cancelledBy;
            event.reservationId = this.reservationId;
            event.paymentId = this.paymentId;
            event.paymentRefunded = this.paymentRefunded;
            event.inventoryReleased = this.inventoryReleased;
            return event;
        }
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public boolean isPaymentRefunded() {
        return paymentRefunded;
    }

    public void setPaymentRefunded(boolean paymentRefunded) {
        this.paymentRefunded = paymentRefunded;
    }

    public boolean isInventoryReleased() {
        return inventoryReleased;
    }

    public void setInventoryReleased(boolean inventoryReleased) {
        this.inventoryReleased = inventoryReleased;
    }

    @Override
    public String toString() {
        return String.format(
            "OrderCancelledEvent{orderId='%s', customerId='%s', amount=%s, reason='%s', refunded=%b, inventoryReleased=%b}",
            orderId, customerId, totalAmount, cancellationReason, paymentRefunded, inventoryReleased
        );
    }
}
