package com.ordersystem.common.events;

public final class RabbitMqNaming {

    public static final String EVENT_EXCHANGE = "ordersystem.events.exchange";
    public static final String COMMAND_EXCHANGE = "ordersystem.commands.exchange";

    private RabbitMqNaming() {
    }

    public static String queueFor(String service, String purpose) {
        return String.format("orderservice.%s.%s.queue", service, purpose).toLowerCase();
    }

    public static String deadLetterQueueFor(String service, String purpose) {
        return String.format("%s.dlq", queueFor(service, purpose));
    }

    public static String retryRoutingKey(String service, String eventType) {
        return String.format("%s.%s.retry", service, eventType);
    }
}
