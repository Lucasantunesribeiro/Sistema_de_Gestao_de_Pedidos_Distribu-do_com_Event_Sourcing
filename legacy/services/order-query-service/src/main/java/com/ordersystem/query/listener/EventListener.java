package com.ordersystem.query.listener;

import com.ordersystem.common.messaging.CorrelationId;
import com.ordersystem.common.messaging.MessagingConstants;
import com.ordersystem.query.service.OrderQueryService;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class EventListener {

    private static final Logger logger = LoggerFactory.getLogger(EventListener.class);

    private final OrderQueryService orderQueryService;

    public EventListener(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @RabbitListener(queues = MessagingConstants.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event,
                                   @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                   @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> {
            validateOrder(event);
            logger.info("Processing OrderCreatedEvent for order {}", event.getOrderId());
            orderQueryService.handleOrderCreated(event);
        });
    }

    @RabbitListener(queues = MessagingConstants.ORDER_UPDATED_QUEUE)
    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event,
                                         @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                         @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> {
            validateOrder(event);
            logger.info("Processing OrderStatusUpdatedEvent for order {}", event.getOrderId());
            orderQueryService.handleOrderStatusUpdated(event);
        });
    }

    @RabbitListener(queues = MessagingConstants.PAYMENT_PROCESSING_QUEUE)
    public void handlePaymentProcessed(PaymentProcessedEvent event,
                                       @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                       @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> {
            if (event == null || event.getOrderId() == null || event.getOrderId().isBlank()) {
                throw new IllegalArgumentException("PaymentProcessedEvent missing orderId");
            }
            logger.info("Processing PaymentProcessedEvent for order {}", event.getOrderId());
            orderQueryService.handlePaymentProcessed(event);
        });
    }

    private void validateOrder(OrderCreatedEvent event) {
        if (event == null || event.getOrderId() == null || event.getOrderId().isBlank()) {
            throw new IllegalArgumentException("OrderCreatedEvent missing orderId");
        }
    }

    private void validateOrder(OrderStatusUpdatedEvent event) {
        if (event == null || event.getOrderId() == null || event.getOrderId().isBlank()) {
            throw new IllegalArgumentException("OrderStatusUpdatedEvent missing orderId");
        }
    }
}
