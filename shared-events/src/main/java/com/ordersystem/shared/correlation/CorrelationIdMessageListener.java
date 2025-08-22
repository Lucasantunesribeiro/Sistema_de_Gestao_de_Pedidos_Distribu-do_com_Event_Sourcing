package com.ordersystem.shared.correlation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import com.rabbitmq.client.Channel;

/**
 * Base message listener that automatically extracts correlation IDs from RabbitMQ messages.
 * Extend this class for automatic correlation ID handling in message consumers.
 */
public abstract class CorrelationIdMessageListener implements ChannelAwareMessageListener {
    
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdMessageListener.class);
    
    @Override
    public final void onMessage(Message message, Channel channel) throws Exception {
        try {
            // Extract correlation ID from message
            String correlationId = CorrelationIdUtils.extractFromMessage(message.getMessageProperties());
            
            if (correlationId != null) {
                CorrelationIdContext.set(correlationId);
                log.debug("Extracted correlation ID {} from RabbitMQ message", correlationId);
            } else {
                // Generate new correlation ID for orphaned messages
                correlationId = CorrelationIdUtils.generateCorrelationId();
                CorrelationIdContext.set(correlationId);
                log.debug("Generated new correlation ID {} for message without correlation", correlationId);
            }
            
            // Process the message with correlation context
            processMessage(message, channel);
            
        } catch (Exception e) {
            log.error("Error processing message with correlation ID {}: {}", 
                CorrelationIdContext.getOrNull(), e.getMessage(), e);
            throw e;
        } finally {
            // Always cleanup context
            CorrelationIdContext.clear();
        }
    }
    
    /**
     * Process the message with correlation ID already set in context.
     * Override this method to implement your message processing logic.
     * 
     * @param message the RabbitMQ message
     * @param channel the RabbitMQ channel
     * @throws Exception if message processing fails
     */
    protected abstract void processMessage(Message message, Channel channel) throws Exception;
}