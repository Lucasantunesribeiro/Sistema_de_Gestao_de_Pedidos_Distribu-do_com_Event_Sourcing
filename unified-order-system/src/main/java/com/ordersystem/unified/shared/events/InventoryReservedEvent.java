package com.ordersystem.unified.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Event published when inventory is reserved for an order.
 * Used for internal communication between modules.
 */
public class InventoryReservedEvent extends BaseEvent {
    
    @JsonProperty("orderId")
    private final String orderId;
    
    @JsonProperty("customerId")
    private final String customerId;
    
    @JsonProperty("reservedItems")
    private final List<OrderItem> reservedItems;
    
    @JsonProperty("reservationId")
    private final String reservationId;

    @JsonCreator
    public InventoryReservedEvent(@JsonProperty("orderId") String orderId,
                                @JsonProperty("customerId") String customerId,
                                @JsonProperty("reservedItems") List<OrderItem> reservedItems,
                                @JsonProperty("reservationId") String reservationId,
                                @JsonProperty("correlationId") String correlationId,
                                @JsonProperty("causationId") String causationId) {
        super(correlationId, causationId, "INVENTORY_RESERVED");
        this.orderId = orderId;
        this.customerId = customerId;
        this.reservedItems = reservedItems;
        this.reservationId = reservationId;
    }

    // Convenience constructor
    public InventoryReservedEvent(String orderId, String customerId, 
                                List<OrderItem> reservedItems, String reservationId) {
        this(orderId, customerId, reservedItems, reservationId, null, null);
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getReservedItems() { return reservedItems; }
    public String getReservationId() { return reservationId; }

    @Override
    public String toString() {
        return String.format("InventoryReservedEvent{orderId='%s', reservationId='%s', itemCount=%d}",
                orderId, reservationId, reservedItems != null ? reservedItems.size() : 0);
    }
}