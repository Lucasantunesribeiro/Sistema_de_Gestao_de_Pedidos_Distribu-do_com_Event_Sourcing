package com.ordersystem.payment.listener;

import com.ordersystem.payment.service.PaymentService;
import com.ordersystem.shared.events.PaymentProcessingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentCommandListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCommandListener.class);

    @Autowired
    private PaymentService paymentService;

    @RabbitListener(queues = "payment.process.queue")
    public void handlePaymentProcessingCommand(@Payload PaymentProcessingCommand command,
                                             @Header(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        
        try {
            logger.info("Processing PaymentProcessingCommand for order {}, amount: {}, correlationId: {}", 
                       command.getOrderId(), command.getTotalAmount(), correlationId);

            // Get amount as BigDecimal
            BigDecimal amount = command.getTotalAmount();
            
            // Default payment method for simulation
            String paymentMethod = "CREDIT_CARD";

            // Process payment asynchronously
            paymentService.processPayment(command.getOrderId(), amount, paymentMethod)
                    .thenAccept(payment -> {
                        logger.info("PaymentProcessingCommand completed for order {}, payment: {}", 
                                   command.getOrderId(), payment.getPaymentId());
                    })
                    .exceptionally(throwable -> {
                        logger.error("PaymentProcessingCommand failed for order {}: {}", 
                                    command.getOrderId(), throwable.getMessage(), throwable);
                        return null;
                    });

        } catch (Exception e) {
            logger.error("Error processing PaymentProcessingCommand for order {}: {}", 
                        command.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}