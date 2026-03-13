package com.ordersystem.payment.listener;

import com.ordersystem.common.messaging.CorrelationId;
import com.ordersystem.common.messaging.MessagingConstants;
import com.ordersystem.payment.service.PaymentService;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderItem;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderEventListener {

    private final PaymentService paymentService;

    public OrderEventListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = MessagingConstants.PAYMENT_PROCESSING_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent orderEvent,
                                   @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                   @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> {
            if (orderEvent == null || orderEvent.getOrderId() == null || orderEvent.getOrderId().isBlank()) {
                throw new IllegalArgumentException("OrderCreatedEvent missing orderId");
            }

            BigDecimal totalAmount = orderEvent.getItems().stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            paymentService.processPayment(orderEvent.getOrderId(), totalAmount, "CREDIT_CARD").join();
        });
    }
}
