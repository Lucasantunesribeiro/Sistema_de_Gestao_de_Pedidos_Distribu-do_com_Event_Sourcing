package com.ordersystem.unified.domain.events;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when inventory reservation is released.
 * Used for event sourcing and compensation logic.
 */
public class InventoryReleasedEvent extends BaseEvent {

    private String reservationId;
    private String orderId;
    private List<OrderItem> items;
    private String releaseReason;
    private LocalDateTime releasedAt;
    private boolean autoReleased; // true if released by timeout

    // Default constructor
    public InventoryReleasedEvent() {
        super();
    }

    public InventoryReleasedEvent(String reservationId, String orderId, List<OrderItem> items,
                                 String releaseReason, String correlationId) {
        super(correlationId);
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.items = items;
        this.releaseReason = releaseReason;
        this.releasedAt = LocalDateTime.now();
        this.autoReleased = false;
    }

    // Getters and Setters
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getReleaseReason() {
        return releaseReason;
    }

    public void setReleaseReason(String releaseReason) {
        this.releaseReason = releaseReason;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public boolean isAutoReleased() {
        return autoReleased;
    }

    public void setAutoReleased(boolean autoReleased) {
        this.autoReleased = autoReleased;
    }

    @Override
    public String toString() {
        return String.format(
            "InventoryReleasedEvent{reservationId='%s', orderId='%s', itemCount=%d, autoReleased=%b}",
            reservationId, orderId, items != null ? items.size() : 0, autoReleased
        );
    }
}

