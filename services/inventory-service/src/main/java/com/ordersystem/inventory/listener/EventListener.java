package com.ordersystem.inventory.listener;

import com.ordersystem.common.messaging.CorrelationId;
import com.ordersystem.common.messaging.MessagingConstants;
import com.ordersystem.inventory.service.InventoryService;
import com.ordersystem.shared.events.InventoryConfirmationCommand;
import com.ordersystem.shared.events.InventoryReservationCommand;
import com.ordersystem.shared.events.InventoryReleaseCommand;
import com.ordersystem.shared.events.OrderCancelledEvent;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.PaymentFailedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class EventListener {

    private final InventoryService inventoryService;

    public EventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @RabbitListener(queues = MessagingConstants.INVENTORY_RESERVATION_QUEUE)
    public void handleInventoryReservationCommand(InventoryReservationCommand command,
                                                  @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                                  @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> inventoryService.reserveInventory(command));
    }

    @RabbitListener(queues = MessagingConstants.INVENTORY_CONFIRMATION_QUEUE)
    public void handleInventoryConfirmationCommand(InventoryConfirmationCommand command,
                                                   @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                                   @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> inventoryService.confirmReservation(command));
    }

    @RabbitListener(queues = MessagingConstants.INVENTORY_RELEASE_QUEUE)
    public void handleInventoryReleaseCommand(InventoryReleaseCommand command,
                                              @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                              @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> inventoryService.releaseReservation(command));
    }

    @RabbitListener(queues = MessagingConstants.ORDER_CANCELLED_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event,
                                     @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                     @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> {
            InventoryReleaseCommand releaseCommand = new InventoryReleaseCommand();
            releaseCommand.setOrderId(event.getOrderId());
            releaseCommand.setReason("Order cancelled by customer");
            inventoryService.releaseReservation(releaseCommand);
        });
    }

    @RabbitListener(queues = MessagingConstants.PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(PaymentFailedEvent event,
                                    @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                    @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> {
            InventoryReleaseCommand releaseCommand = new InventoryReleaseCommand();
            releaseCommand.setOrderId(event.getOrderId());
            releaseCommand.setReason("Payment failed: " + event.getReason());
            inventoryService.releaseReservation(releaseCommand);
        });
    }

    @RabbitListener(queues = MessagingConstants.INVENTORY_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent orderEvent,
                                   @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                   @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> {
            InventoryReservationCommand command = new InventoryReservationCommand(
                    orderEvent.getOrderId(),
                    orderEvent.getCustomerId(),
                    orderEvent.getItems()
            );
            inventoryService.reserveInventory(command);
        });
    }

    @RabbitListener(queues = MessagingConstants.INVENTORY_QUEUE)
    public void handlePaymentProcessed(PaymentProcessedEvent paymentEvent,
                                       @Header(value = MessagingConstants.CORRELATION_ID_HEADER, required = false) String correlationId,
                                       @Header(value = AmqpHeaders.CORRELATION_ID, required = false) String amqpCorrelationId) {
        String resolved = CorrelationId.resolve(
                correlationId != null && !correlationId.isBlank() ? correlationId : amqpCorrelationId
        );
        CorrelationId.withMdc(resolved, () -> {
            if ("APPROVED".equals(paymentEvent.getPaymentStatus())) {
                InventoryConfirmationCommand command = new InventoryConfirmationCommand();
                command.setOrderId(paymentEvent.getOrderId());
                inventoryService.confirmReservation(command);
                return;
            }
            InventoryReleaseCommand releaseCommand = new InventoryReleaseCommand();
            releaseCommand.setOrderId(paymentEvent.getOrderId());
            releaseCommand.setReason("Payment declined: " + paymentEvent.getPaymentStatus());
            inventoryService.releaseReservation(releaseCommand);
        });
    }
}
