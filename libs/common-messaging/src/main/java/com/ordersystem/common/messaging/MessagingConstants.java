package com.ordersystem.common.messaging;

public final class MessagingConstants {
    private MessagingConstants() {
    }

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String INVENTORY_EXCHANGE = "inventory.exchange";
    public static final String ORDER_FANOUT_EXCHANGE = "order.fanout";

    public static final String DLQ_ROUTING_KEY = "failed";

    public static final String PAYMENT_PROCESSING_QUEUE = "payment.processing.queue";
    public static final String PAYMENT_RESULTS_QUEUE = "payment.results.queue";
    public static final String PAYMENT_DLX_EXCHANGE = "payment.dlx";
    public static final String PAYMENT_DLQ_QUEUE = "payment.dlq";

    public static final String INVENTORY_QUEUE = "inventory.queue";
    public static final String INVENTORY_RESERVATION_QUEUE = "inventory.reservation.queue";
    public static final String INVENTORY_CONFIRMATION_QUEUE = "inventory.confirmation.queue";
    public static final String INVENTORY_RELEASE_QUEUE = "inventory.release.queue";
    public static final String INVENTORY_DLX_EXCHANGE = "inventory.dlx";
    public static final String INVENTORY_DLQ_QUEUE = "inventory.dlq";

    public static final String QUERY_DLX_EXCHANGE = "query.dlx";
    public static final String QUERY_DLQ_QUEUE = "query.dlq";

    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_UPDATED_QUEUE = "order.updated.queue";
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.queue";
    public static final String ORDER_AUDIT_QUEUE = "order.audit.queue";
    public static final String PAYMENT_AUDIT_QUEUE = "payment.audit.queue";
    public static final String INVENTORY_AUDIT_QUEUE = "inventory.audit.queue";

    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    public static final String ORDER_STATUS_UPDATED_ROUTING_KEY = "order.status.updated";
    public static final String ORDER_UPDATED_ROUTING_KEY = "order.updated";
    public static final String PAYMENT_PROCESSING_ROUTING_KEY = "payment.processing";
    public static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";
    public static final String PAYMENT_REFUNDED_ROUTING_KEY = "payment.refunded";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String INVENTORY_RESERVATION_ROUTING_KEY = "inventory.reservation";
    public static final String INVENTORY_RESERVED_ROUTING_KEY = "inventory.reserved";
    public static final String INVENTORY_RELEASED_ROUTING_KEY = "inventory.released";
    public static final String INVENTORY_FAILED_ROUTING_KEY = "inventory.failed";
    public static final String INVENTORY_UPDATED_ROUTING_KEY = "inventory.updated";
}
