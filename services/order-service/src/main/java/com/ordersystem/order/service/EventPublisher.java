package com.ordersystem.order.service;

import com.ordersystem.order.exception.MessageBrokerException;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.InventoryReservationCommand;
import com.ordersystem.shared.events.PaymentProcessingCommand;
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
public class EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @CircuitBreaker(name = "messageBroker", fallbackMethod = "publishOrderCreatedEventFallback")
    @Retry(name = "messageBroker")
    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Publishing OrderCreatedEvent for order {}, correlationId: {}", 
                       event.getOrderId(), correlationId);

            rabbitTemplate.convertAndSend("order.exchange", "order.created", event, message -> {
                message.getMessageProperties().setCorrelationId(correlationId);
                message.getMessageProperties().setHeader("eventType", "OrderCreatedEvent");
                return message;
            });

            logger.debug("OrderCreatedEvent published successfully for order {}", event.getOrderId());

        } catch (AmqpException e) {
            logger.error("Failed to publish OrderCreatedEvent for order {}: {}", 
                        event.getOrderId(), e.getMessage());
            throw new MessageBrokerException("Failed to publish order created event", e);
        }
    }

    @CircuitBreaker(name = "messageBroker", fallbackMethod = "publishInventoryReservationCommandFallback")
    @Retry(name = "messageBroker")
    public void publishInventoryReservationCommand(InventoryReservationCommand command) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Publishing InventoryReservationCommand for order {}, correlationId: {}", 
                       command.getOrderId(), correlationId);

            rabbitTemplate.convertAndSend("inventory.exchange", "inventory.reserve", command, message -> {
                message.getMessageProperties().setCorrelationId(correlationId);
                message.getMessageProperties().setHeader("commandType", "InventoryReservationCommand");
                return message;
            });

            logger.debug("InventoryReservationCommand published successfully for order {}", command.getOrderId());

        } catch (AmqpException e) {
            logger.error("Failed to publish InventoryReservationCommand for order {}: {}", 
                        command.getOrderId(), e.getMessage());
            throw new MessageBrokerException("Failed to publish inventory reservation command", e);
        }
    }

    @CircuitBreaker(name = "messageBroker", fallbackMethod = "publishPaymentProcessingCommandFallback")
    @Retry(name = "messageBroker")
    public void publishPaymentProcessingCommand(PaymentProcessingCommand command) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Publishing PaymentProcessingCommand for order {}, correlationId: {}", 
                       command.getOrderId(), correlationId);

            rabbitTemplate.convertAndSend("payment.exchange", "payment.process", command, message -> {
                message.getMessageProperties().setCorrelationId(correlationId);
                message.getMessageProperties().setHeader("commandType", "PaymentProcessingCommand");
                return message;
            });

            logger.debug("PaymentProcessingCommand published successfully for order {}", command.getOrderId());

        } catch (AmqpException e) {
            logger.error("Failed to publish PaymentProcessingCommand for order {}: {}", 
                        command.getOrderId(), e.getMessage());
            throw new MessageBrokerException("Failed to publish payment processing command", e);
        }
    }

    // Fallback methods
    public void publishOrderCreatedEventFallback(OrderCreatedEvent event, Exception ex) {
        String correlationId = MDC.get("correlationId");
        logger.error("Circuit breaker activated for publishOrderCreatedEvent, order {}, correlationId: {}, error: {}", 
                    event.getOrderId(), correlationId, ex.getMessage());
        
        // In a real system, you might want to store the event for later retry
        // or use a dead letter queue
        throw new MessageBrokerException("Message broker service is currently unavailable", ex);
    }

    public void publishInventoryReservationCommandFallback(InventoryReservationCommand command, Exception ex) {
        String correlationId = MDC.get("correlationId");
        logger.error("Circuit breaker activated for publishInventoryReservationCommand, order {}, correlationId: {}, error: {}", 
                    command.getOrderId(), correlationId, ex.getMessage());
        
        throw new MessageBrokerException("Message broker service is currently unavailable", ex);
    }

    public void publishPaymentProcessingCommandFallback(PaymentProcessingCommand command, Exception ex) {
        String correlationId = MDC.get("correlationId");
        logger.error("Circuit breaker activated for publishPaymentProcessingCommand, order {}, correlationId: {}, error: {}", 
                    command.getOrderId(), correlationId, ex.getMessage());
        
        throw new MessageBrokerException("Message broker service is currently unavailable", ex);
    }
}