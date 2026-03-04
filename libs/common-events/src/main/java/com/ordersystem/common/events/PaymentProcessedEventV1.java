package com.ordersystem.common.events;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class PaymentProcessedEventV1 implements VersionedEvent {

    private final String eventType = "PaymentProcessedEvent";
    private final String schemaVersion = "v1";
    private final String orderId;
    private final BigDecimal amount;
    private final OffsetDateTime processedAt;

    public PaymentProcessedEventV1(String orderId, BigDecimal amount, OffsetDateTime processedAt) {
        this.orderId = orderId;
        this.amount = amount;
        this.processedAt = processedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
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
