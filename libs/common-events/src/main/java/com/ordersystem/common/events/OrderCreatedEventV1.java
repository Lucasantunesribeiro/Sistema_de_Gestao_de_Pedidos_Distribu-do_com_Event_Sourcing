package com.ordersystem.common.events;

import java.time.OffsetDateTime;
import java.util.List;

public final class OrderCreatedEventV1 implements VersionedEvent {

    private final String eventType = "OrderCreatedEvent";
    private final String schemaVersion = "v1";
    private final String orderId;
    private final List<String> itemIds;
    private final OffsetDateTime createdAt;

    public OrderCreatedEventV1(String orderId, List<String> itemIds, OffsetDateTime createdAt) {
        this.orderId = orderId;
        this.itemIds = itemIds;
        this.createdAt = createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public List<String> getItemIds() {
        return List.copyOf(itemIds);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String getSchemaVersion() {
        return schemaVersion;
    }

    @Override
    public String getEventType() {
        return eventType;
    }
}
