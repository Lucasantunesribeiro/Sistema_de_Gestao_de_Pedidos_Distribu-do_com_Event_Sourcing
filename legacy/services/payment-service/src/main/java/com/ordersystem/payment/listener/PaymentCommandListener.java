package com.ordersystem.payment.listener;

import com.ordersystem.common.messaging.CorrelationId;
import com.ordersystem.common.messaging.MessagingConstants;
import com.ordersystem.payment.exception.PaymentNotFoundException;
import com.ordersystem.payment.service.PaymentService;
import com.ordersystem.shared.events.PaymentProcessingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class PaymentCommandListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCommandListener.class);

    private final PaymentService paymentService;

    public PaymentCommandListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = MessagingConstants.PAYMENT_PROCESSING_QUEUE)
    public void handlePaymentProcessingCommand(@Payload PaymentProcessingCommand command,
                                               @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                               @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> {
            if (command == null || command.getOrderId() == null || command.getTotalAmount() == null) {
                throw new IllegalArgumentException("PaymentProcessingCommand missing required fields");
            }

            try {
                paymentService.getPaymentByOrderId(command.getOrderId());
                logger.info("Payment already exists for order {}, skipping", command.getOrderId());
                return;
            } catch (PaymentNotFoundException ignored) {
                // proceed
            }

            String paymentMethod = determinePaymentMethod(command);
            paymentService.processPayment(command.getOrderId(), command.getTotalAmount(), paymentMethod).join();
            logger.info("Payment processed for order {}", command.getOrderId());
        });
    }

    private String determinePaymentMethod(PaymentProcessingCommand command) {
        return "CREDIT_CARD";
    }
}
