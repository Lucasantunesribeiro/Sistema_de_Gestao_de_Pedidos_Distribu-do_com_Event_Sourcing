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

    @RabbitListener(queues = "payment.processing.queue", 
                containerFactory = "rabbitListenerContainerFactory")
    public void handlePaymentProcessingCommand(@Payload PaymentProcessingCommand command,
                                             @Header(value = "correlationId", required = false) String correlationId) {
        
        // Set correlation ID for tracking
        if (correlationId == null) {
            correlationId = java.util.UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        
        try {
            logger.info("[{}] Received PaymentProcessingCommand for order {}, customer: {}, amount: {}", 
                       correlationId, command.getOrderId(), command.getCustomerId(), command.getTotalAmount());

            // Validate command
            if (command.getOrderId() == null || command.getTotalAmount() == null) {
                logger.error("[{}] Invalid PaymentProcessingCommand: missing required fields", correlationId);
                return;
            }

            // Check if payment already exists to avoid duplicate processing
            try {
                paymentService.getPaymentByOrderId(command.getOrderId());
                logger.warn("[{}] Payment already exists for order {}, skipping processing", 
                           correlationId, command.getOrderId());
                return;
            } catch (com.ordersystem.payment.exception.PaymentNotFoundException e) {
                // No payment exists, proceed with processing
                logger.debug("[{}] No existing payment found for order {}, proceeding with processing", 
                            correlationId, command.getOrderId());
            }

            // Default payment method for simulation (could be enhanced to get from command)
            String paymentMethod = determinePaymentMethod(command);

            // Process payment asynchronously
            paymentService.processPayment(command.getOrderId(), command.getTotalAmount(), paymentMethod)
                    .thenAccept(payment -> {
                        MDC.put("correlationId", correlationId);
                        logger.info("[{}] PaymentProcessingCommand completed successfully for order {}, payment: {}, status: {}", 
                                   correlationId, command.getOrderId(), payment.getPaymentId(), payment.getStatus());
                        MDC.clear();
                    })
                    .exceptionally(throwable -> {
                        MDC.put("correlationId", correlationId);
                        logger.error("[{}] PaymentProcessingCommand failed for order {}: {}", 
                                    correlationId, command.getOrderId(), throwable.getMessage(), throwable);
                        MDC.clear();
                        return null;
                    });

            logger.debug("[{}] PaymentProcessingCommand handling initiated for order {}", 
                        correlationId, command.getOrderId());

        } catch (Exception e) {
            logger.error("[{}] Error processing PaymentProcessingCommand for order {}: {}", 
                        correlationId, command.getOrderId(), e.getMessage(), e);
            // In a production system, you might want to send to a dead letter queue here
        } finally {
            MDC.clear();
        }
    }

    /**
     * Determines payment method based on command or applies default logic
     */
    private String determinePaymentMethod(PaymentProcessingCommand command) {
        // In a real system, this could come from the command or customer preferences
        // For now, we'll use a default method
        return "CREDIT_CARD";
    }
}