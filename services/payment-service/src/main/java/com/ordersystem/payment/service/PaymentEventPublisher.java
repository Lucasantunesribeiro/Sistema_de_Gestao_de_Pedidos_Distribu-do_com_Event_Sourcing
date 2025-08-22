package com.ordersystem.payment.service;

import com.ordersystem.payment.exception.MessageBrokerException;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import com.ordersystem.shared.events.PaymentFailedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @CircuitBreaker(name = "messageBroker", fallbackMethod = "publishPaymentProcessedEventFallback")
    @Retry(name = "messageBroker")
    public void publishPaymentProcessedEvent(PaymentProcessedEvent event) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Publishing PaymentProcessedEvent for order {}, correlationId: {}", 
                       event.getOrderId(), correlationId);

            rabbitTemplate.convertAndSend("payment.exchange", "payment.processed", event, message -> {
                message.getMessageProperties().setCorrelationId(correlationId);
                message.getMessageProperties().setHeader("eventType", "PaymentProcessedEvent");
                return message;
            });

            logger.debug("PaymentProcessedEvent published successfully for order {}", event.getOrderId());

        } catch (AmqpException e) {
            logger.error("Failed to publish PaymentProcessedEvent for order {}: {}", 
                        event.getOrderId(), e.getMessage());
            throw new MessageBrokerException("Failed to publish payment processed event", e);
        }
    }

    @CircuitBreaker(name = "messageBroker", fallbackMethod = "publishPaymentFailedEventFallback")
    @Retry(name = "messageBroker")
    public void publishPaymentFailedEvent(PaymentFailedEvent event) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Publishing PaymentFailedEvent for order {}, correlationId: {}", 
                       event.getOrderId(), correlationId);

            rabbitTemplate.convertAndSend("payment.exchange", "payment.failed", event, message -> {
                message.getMessageProperties().setCorrelationId(correlationId);
                message.getMessageProperties().setHeader("eventType", "PaymentFailedEvent");
                return message;
            });

            logger.debug("PaymentFailedEvent published successfully for order {}", event.getOrderId());

        } catch (AmqpException e) {
            logger.error("Failed to publish PaymentFailedEvent for order {}: {}", 
                        event.getOrderId(), e.getMessage());
            throw new MessageBrokerException("Failed to publish payment failed event", e);
        }
    }

    // Fallback methods
    public void publishPaymentProcessedEventFallback(PaymentProcessedEvent event, Exception ex) {
        String correlationId = MDC.get("correlationId");
        logger.error("Circuit breaker activated for publishPaymentProcessedEvent, order {}, correlationId: {}, error: {}", 
                    event.getOrderId(), correlationId, ex.getMessage());
        
        throw new MessageBrokerException("Message broker service is currently unavailable", ex);
    }

    public void publishPaymentFailedEventFallback(PaymentFailedEvent event, Exception ex) {
        String correlationId = MDC.get("correlationId");
        logger.error("Circuit breaker activated for publishPaymentFailedEvent, order {}, correlationId: {}, error: {}", 
                    event.getOrderId(), correlationId, ex.getMessage());
        
        throw new MessageBrokerException("Message broker service is currently unavailable", ex);
    }
}