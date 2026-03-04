package com.ordersystem.payment.service;

import com.ordersystem.common.messaging.MessagingConstants;
import com.ordersystem.payment.exception.MessageBrokerException;
import com.ordersystem.shared.events.PaymentFailedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @CircuitBreaker(name = "messageBroker", fallbackMethod = "publishPaymentProcessedEventFallback")
    @Retry(name = "messageBroker")
    public void publishPaymentProcessedEvent(PaymentProcessedEvent event) {
        String correlationId = MDC.get("correlationId");

        try {
            logger.info("Publishing PaymentProcessedEvent for order {}, correlationId: {}",
                    event.getOrderId(), correlationId);

            rabbitTemplate.convertAndSend(
                    MessagingConstants.ORDER_EXCHANGE,
                    MessagingConstants.PAYMENT_PROCESSED_ROUTING_KEY,
                    event
            );

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

            rabbitTemplate.convertAndSend(
                    MessagingConstants.ORDER_EXCHANGE,
                    MessagingConstants.PAYMENT_FAILED_ROUTING_KEY,
                    event
            );

        } catch (AmqpException e) {
            logger.error("Failed to publish PaymentFailedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage());
            throw new MessageBrokerException("Failed to publish payment failed event", e);
        }
    }

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
