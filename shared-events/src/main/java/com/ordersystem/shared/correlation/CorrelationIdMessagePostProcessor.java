package com.ordersystem.shared.correlation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

/**
 * RabbitMQ message post-processor that automatically injects correlation IDs.
 * Use this processor to ensure all outgoing messages include correlation tracking.
 */
public class CorrelationIdMessagePostProcessor implements MessagePostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdMessagePostProcessor.class);
    
    @Override
    public Message postProcessMessage(Message message) throws AmqpException {
        try {
            // Inject correlation ID into message properties
            CorrelationIdUtils.injectIntoMessage(message.getMessageProperties());
            
            String correlationId = CorrelationIdContext.getOrNull();
            if (correlationId != null) {
                log.debug("Injected correlation ID {} into RabbitMQ message", correlationId);
            } else {
                log.warn("No correlation ID found in context for RabbitMQ message");
            }
            
        } catch (Exception e) {
            log.warn("Failed to inject correlation ID into RabbitMQ message: {}", e.getMessage());
            // Don't fail the message - just log the warning
        }
        
        return message;
    }
}