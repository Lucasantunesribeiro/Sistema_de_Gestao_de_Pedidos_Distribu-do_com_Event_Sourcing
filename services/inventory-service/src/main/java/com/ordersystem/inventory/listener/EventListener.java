package com.ordersystem.inventory.listener;

import com.ordersystem.inventory.service.InventoryService;
import com.ordersystem.shared.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Event listener for inventory-related commands and events
 */
@Component
public class EventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(EventListener.class);

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Handle inventory reservation commands from Order Service
     */
    @RabbitListener(queues = "inventory.reservation.queue")
    public void handleInventoryReservationCommand(String commandJson) {
        try {
            logger.info("Received inventory reservation command: {}", commandJson);
            InventoryReservationCommand command = objectMapper.readValue(commandJson, InventoryReservationCommand.class);
            inventoryService.reserveInventory(command);
        } catch (Exception e) {
            logger.error("Error processing inventory reservation command: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle inventory confirmation commands from Payment Service
     */
    @RabbitListener(queues = "inventory.confirmation.queue")
    public void handleInventoryConfirmationCommand(String commandJson) {
        try {
            logger.info("Received inventory confirmation command: {}", commandJson);
            InventoryConfirmationCommand command = objectMapper.readValue(commandJson, InventoryConfirmationCommand.class);
            inventoryService.confirmReservation(command);
        } catch (Exception e) {
            logger.error("Error processing inventory confirmation command: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle inventory release commands (compensation actions)
     */
    @RabbitListener(queues = "inventory.release.queue")
    public void handleInventoryReleaseCommand(String commandJson) {
        try {
            logger.info("Received inventory release command: {}", commandJson);
            InventoryReleaseCommand command = objectMapper.readValue(commandJson, InventoryReleaseCommand.class);
            inventoryService.releaseReservation(command);
        } catch (Exception e) {
            logger.error("Error processing inventory release command: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle order cancelled events for automatic stock release
     */
    @RabbitListener(queues = "order.cancelled.queue")
    public void handleOrderCancelled(String eventJson) {
        try {
            logger.info("Received order cancelled event: {}", eventJson);
            OrderCancelledEvent event = objectMapper.readValue(eventJson, OrderCancelledEvent.class);
            
            // Create release command for cancelled order
            InventoryReleaseCommand releaseCommand = new InventoryReleaseCommand();
            releaseCommand.setOrderId(event.getOrderId());
            releaseCommand.setReason("Order cancelled by customer");
            
            inventoryService.releaseReservation(releaseCommand);
            
        } catch (Exception e) {
            logger.error("Error processing order cancelled event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle payment failed events for automatic stock release
     */
    @RabbitListener(queues = "payment.failed.queue")
    public void handlePaymentFailed(String eventJson) {
        try {
            logger.info("Received payment failed event: {}", eventJson);
            PaymentFailedEvent event = objectMapper.readValue(eventJson, PaymentFailedEvent.class);
            
            // Create release command for failed payment
            InventoryReleaseCommand releaseCommand = new InventoryReleaseCommand();
            releaseCommand.setOrderId(event.getOrderId());
            releaseCommand.setReason("Payment failed: " + event.getReason());
            
            inventoryService.releaseReservation(releaseCommand);
            
        } catch (Exception e) {
            logger.error("Error processing payment failed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle legacy events using the new command-based approach
     */
    @RabbitListener(queues = "#{inventoryQueue.name}")
    public void handleOrderCreated(OrderCreatedEvent orderEvent) {
        logger.info("Received legacy order created event for order: {}", orderEvent.getOrderId());
        
        // Convert to new command format
        InventoryReservationCommand command = new InventoryReservationCommand(
            orderEvent.getOrderId(),
            orderEvent.getCustomerId(),
            orderEvent.getItems()
        );
        
        inventoryService.reserveInventory(command);
    }

    @RabbitListener(queues = "#{inventoryQueue.name}")
    public void handlePaymentProcessed(PaymentProcessedEvent paymentEvent) {
        logger.info("Received legacy payment processed event for order: {}", paymentEvent.getOrderId());
        
        if ("APPROVED".equals(paymentEvent.getPaymentStatus())) {
            // Convert to confirmation command
            InventoryConfirmationCommand command = new InventoryConfirmationCommand();
            command.setOrderId(paymentEvent.getOrderId());
            
            inventoryService.confirmReservation(command);
            
        } else {
            // Convert to release command
            InventoryReleaseCommand releaseCommand = new InventoryReleaseCommand();
            releaseCommand.setOrderId(paymentEvent.getOrderId());
            releaseCommand.setReason("Payment declined: " + paymentEvent.getPaymentStatus());
            
            inventoryService.releaseReservation(releaseCommand);
        }
    }
}