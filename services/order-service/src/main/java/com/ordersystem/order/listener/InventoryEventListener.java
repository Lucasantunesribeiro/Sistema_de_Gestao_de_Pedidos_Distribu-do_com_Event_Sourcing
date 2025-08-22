package com.ordersystem.order.listener;

import com.ordersystem.order.exception.OrderNotFoundException;
import com.ordersystem.order.model.Order;
import com.ordersystem.order.repository.OrderEventStore;
import com.ordersystem.order.service.EventPublisher;
import com.ordersystem.shared.events.InventoryReservedEvent;
import com.ordersystem.shared.events.InventoryReservationFailedEvent;
import com.ordersystem.shared.events.PaymentProcessingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class InventoryEventListener {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventListener.class);

    @Autowired
    private OrderEventStore eventStore;

    @Autowired
    private EventPublisher eventPublisher;

    @RabbitListener(queues = "inventory.reserved.queue")
    @Transactional
    public void handleInventoryReserved(@Payload InventoryReservedEvent event,
                                      @Header(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        
        try {
            logger.info("Processing InventoryReservedEvent for order {}, correlationId: {}", 
                       event.getOrderId(), correlationId);

            Order order = eventStore.getOrder(event.getOrderId())
                    .orElseThrow(() -> new OrderNotFoundException(event.getOrderId()));

            // Apply the inventory reserved event
            order.applyEvent(event);
            eventStore.saveOrder(order);

            // Trigger payment processing
            PaymentProcessingCommand paymentCommand = new PaymentProcessingCommand(
                order.getOrderId(),
                order.getTotalAmount().doubleValue(),
                LocalDateTime.now()
            );

            eventPublisher.publishPaymentProcessingCommand(paymentCommand);

            logger.info("InventoryReservedEvent processed successfully for order {}, payment command sent", 
                       event.getOrderId());

        } catch (Exception e) {
            logger.error("Error processing InventoryReservedEvent for order {}: {}", 
                        event.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @RabbitListener(queues = "inventory.reservation.failed.queue")
    @Transactional
    public void handleInventoryReservationFailed(@Payload InventoryReservationFailedEvent event,
                                               @Header(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        
        try {
            logger.info("Processing InventoryReservationFailedEvent for order {}, reason: {}, correlationId: {}", 
                       event.getOrderId(), event.getReason(), correlationId);

            Order order = eventStore.getOrder(event.getOrderId())
                    .orElseThrow(() -> new OrderNotFoundException(event.getOrderId()));

            // Apply the inventory reservation failed event
            order.applyEvent(event);

            // Cancel the order due to inventory failure
            order.cancel("Falha na reserva de estoque: " + event.getReason());

            eventStore.saveOrder(order);

            logger.info("InventoryReservationFailedEvent processed successfully for order {}", event.getOrderId());

        } catch (Exception e) {
            logger.error("Error processing InventoryReservationFailedEvent for order {}: {}", 
                        event.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}