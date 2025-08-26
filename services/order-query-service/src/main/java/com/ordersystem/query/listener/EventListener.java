package com.ordersystem.query.listener;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ordersystem.query.service.OrderQueryService;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;

@Component
public class EventListener {

    private static final Logger logger = LoggerFactory.getLogger(EventListener.class);

    @Autowired
    private OrderQueryService orderQueryService;

    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        String correlationId = event.getCorrelationId() != null ? event.getCorrelationId() : "N/A";

        // Set correlation ID in MDC for request tracing
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", event.getOrderId());
        MDC.put("eventType", "ORDER_CREATED");

        logger.info(
                "📨 Query service received OrderCreatedEvent: orderId={}, customerId={}, totalAmount={}, itemCount={}, timestamp={}, correlationId={}",
                event.getOrderId(), event.getCustomerId(), event.getTotalAmount(),
                event.getItems() != null ? event.getItems().size() : 0, event.getTimestamp(), correlationId);

        try {
            // Validate event before processing
            if (event == null) {
                logger.error("❌ Received null OrderCreatedEvent, correlationId={}", correlationId);
                return; // Don't retry null events
            }

            if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
                logger.error("❌ Received OrderCreatedEvent with null/empty orderId, correlationId={}", correlationId);
                return; // Don't retry invalid events
            }

            logger.debug("🔧 Processing OrderCreatedEvent: orderId={}, correlationId={}",
                    event.getOrderId(), correlationId);

            long startTime = System.currentTimeMillis();
            orderQueryService.handleOrderCreated(event);
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info("✅ Successfully processed OrderCreatedEvent: orderId={}, processingTime={}ms, correlationId={}",
                    event.getOrderId(), processingTime, correlationId);

        } catch (IllegalArgumentException e) {
            logger.error(
                    "❌ Invalid OrderCreatedEvent data: orderId={}, customerId={}, error={}, correlationId={}",
                    event != null ? event.getOrderId() : "null",
                    event != null ? event.getCustomerId() : "null",
                    e.getMessage(), correlationId, e);
            // Don't re-throw IllegalArgumentException - these are data validation errors
            // that won't be fixed by retry
        } catch (Exception e) {
            logger.error(
                    "❌ Failed to process OrderCreatedEvent: orderId={}, customerId={}, totalAmount={}, error={}, correlationId={}",
                    event != null ? event.getOrderId() : "null",
                    event != null ? event.getCustomerId() : "null",
                    event != null ? event.getTotalAmount() : "null",
                    e.getMessage(), correlationId, e);
            throw e; // Re-throw to trigger retry mechanism for transient errors
        } finally {
            MDC.clear();
        }
    }

    @RabbitListener(queues = "order.updated.queue")
    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        String correlationId = event.getCorrelationId() != null ? event.getCorrelationId() : "N/A";

        // Set correlation ID in MDC for request tracing
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", event.getOrderId());
        MDC.put("eventType", "ORDER_STATUS_UPDATED");

        logger.info(
                "📨 Query service received OrderStatusUpdatedEvent: orderId={}, customerId={}, {} -> {}, timestamp={}, correlationId={}",
                event.getOrderId(), event.getCustomerId(), event.getOldStatus(), event.getNewStatus(),
                event.getTimestamp(), correlationId);

        try {
            // Validate event before processing
            if (event == null) {
                logger.error("❌ Received null OrderStatusUpdatedEvent, correlationId={}", correlationId);
                return; // Don't retry null events
            }

            if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
                logger.error("❌ Received OrderStatusUpdatedEvent with null/empty orderId, correlationId={}",
                        correlationId);
                return; // Don't retry invalid events
            }

            logger.debug("🔧 Processing OrderStatusUpdatedEvent: orderId={}, statusChange={} -> {}, correlationId={}",
                    event.getOrderId(), event.getOldStatus(), event.getNewStatus(), correlationId);

            long startTime = System.currentTimeMillis();
            orderQueryService.handleOrderStatusUpdated(event);
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info(
                    "✅ Successfully processed OrderStatusUpdatedEvent: orderId={}, {} -> {}, processingTime={}ms, correlationId={}",
                    event.getOrderId(), event.getOldStatus(), event.getNewStatus(), processingTime, correlationId);

        } catch (IllegalArgumentException e) {
            logger.error(
                    "❌ Invalid OrderStatusUpdatedEvent data: orderId={}, {} -> {}, error={}, correlationId={}",
                    event != null ? event.getOrderId() : "null",
                    event != null ? event.getOldStatus() : "null",
                    event != null ? event.getNewStatus() : "null",
                    e.getMessage(), correlationId, e);
            // Don't re-throw IllegalArgumentException - these are data validation errors
            // that won't be fixed by retry
        } catch (Exception e) {
            logger.error(
                    "❌ Failed to process OrderStatusUpdatedEvent: orderId={}, {} -> {}, error={}, correlationId={}",
                    event != null ? event.getOrderId() : "null",
                    event != null ? event.getOldStatus() : "null",
                    event != null ? event.getNewStatus() : "null",
                    e.getMessage(), correlationId, e);
            throw e; // Re-throw to trigger retry mechanism for transient errors
        } finally {
            MDC.clear();
        }
    }

    @RabbitListener(queues = "payment.processing.queue")
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        // Note: PaymentProcessedEvent might not have correlationId, so we'll handle it
        // gracefully
        String correlationId = "N/A"; // Default since PaymentProcessedEvent doesn't have correlationId method

        // Set correlation ID in MDC for request tracing
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", event != null ? event.getOrderId() : "null");
        MDC.put("eventType", "PAYMENT_PROCESSED");

        logger.info(
                "📨 Query service received PaymentProcessedEvent: orderId={}, paymentId={}, status={}, amount={}, correlationId={}",
                event != null ? event.getOrderId() : "null",
                event != null ? event.getPaymentId() : "null",
                event != null ? event.getPaymentStatus() : "null",
                event != null ? event.getAmount() : 0.0, correlationId);

        try {
            // Validate event before processing
            if (event == null) {
                logger.error("❌ Received null PaymentProcessedEvent, correlationId={}", correlationId);
                return; // Don't retry null events
            }

            if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
                logger.error("❌ Received PaymentProcessedEvent with null/empty orderId, correlationId={}",
                        correlationId);
                return; // Don't retry invalid events
            }

            logger.debug("🔧 Processing PaymentProcessedEvent: orderId={}, paymentId={}, status={}, correlationId={}",
                    event.getOrderId(), event.getPaymentId(), event.getPaymentStatus(), correlationId);

            long startTime = System.currentTimeMillis();
            orderQueryService.handlePaymentProcessed(event);
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info(
                    "✅ Successfully processed PaymentProcessedEvent: orderId={}, paymentId={}, status={}, processingTime={}ms, correlationId={}",
                    event.getOrderId(), event.getPaymentId(), event.getPaymentStatus(), processingTime, correlationId);

        } catch (IllegalArgumentException e) {
            logger.error(
                    "❌ Invalid PaymentProcessedEvent data: orderId={}, paymentId={}, status={}, error={}, correlationId={}",
                    event != null ? event.getOrderId() : "null",
                    event != null ? event.getPaymentId() : "null",
                    event != null ? event.getPaymentStatus() : "null",
                    e.getMessage(), correlationId, e);
            // Don't re-throw IllegalArgumentException - these are data validation errors
            // that won't be fixed by retry
        } catch (Exception e) {
            logger.error(
                    "❌ Failed to process PaymentProcessedEvent: orderId={}, paymentId={}, status={}, error={}, correlationId={}",
                    event != null ? event.getOrderId() : "null",
                    event != null ? event.getPaymentId() : "null",
                    event != null ? event.getPaymentStatus() : "null",
                    e.getMessage(), correlationId, e);
            throw e; // Re-throw to trigger retry mechanism for transient errors
        } finally {
            MDC.clear();
        }
    }
}