package com.ordersystem.common.observability;

public final class RabbitMqHeaders {
    private RabbitMqHeaders() {
    }

    public static final String CORRELATION_ID = "x-correlation-id";
    public static final String CAUSATION_ID = "x-causation-id";
    public static final String EVENT_ID = "x-event-id";
}
