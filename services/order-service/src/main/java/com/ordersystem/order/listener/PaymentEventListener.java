package com.ordersystem.order.listener;

import com.ordersystem.order.exception.OrderNotFoundException;
import com.ordersystem.order.model.Order;
import com.ordersystem.order.repository.OrderEventStore;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import com.ordersystem.shared.events.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);

    @Autowired
    private OrderEventStore eventStore;

    @RabbitListener(queues = "payment.processed.queue")
    @Transactional
    public void handlePaymentProcessed(@Payload PaymentProcessedEvent event,
                                     @Header(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        
        try {
            logger.info("Processing PaymentProcessedEvent for order {}, status: {}, correlationId: {}", 
                       event.getOrderId(), event.getPaymentStatus(), correlationId);

            Order order = eventStore.getOrder(event.getOrderId())
                    .orElseThrow(() -> new OrderNotFoundException(event.getOrderId()));

            // Apply the payment processed event
            order.applyEvent(event);

            // If payment was approved, complete the order
            if ("APPROVED".equals(event.getPaymentStatus())) {
                order.complete();
            }

            eventStore.saveOrder(order);

            logger.info("PaymentProcessedEvent processed successfully for order {}", event.getOrderId());

        } catch (Exception e) {
            logger.error("Error processing PaymentProcessedEvent for order {}: {}", 
                        event.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @RabbitListener(queues = "payment.failed.queue")
    @Transactional
    public void handlePaymentFailed(@Payload PaymentFailedEvent event,
                                  @Header(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        
        try {
            logger.info("Processing PaymentFailedEvent for order {}, reason: {}, correlationId: {}", 
                       event.getOrderId(), event.getReason(), correlationId);

            Order order = eventStore.getOrder(event.getOrderId())
                    .orElseThrow(() -> new OrderNotFoundException(event.getOrderId()));

            // Apply the payment failed event
            order.applyEvent(event);

            // Cancel the order due to payment failure
            order.cancel("Pagamento falhou: " + event.getReason());

            eventStore.saveOrder(order);

            logger.info("PaymentFailedEvent processed successfully for order {}", event.getOrderId());

        } catch (Exception e) {
            logger.error("Error processing PaymentFailedEvent for order {}: {}", 
                        event.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}