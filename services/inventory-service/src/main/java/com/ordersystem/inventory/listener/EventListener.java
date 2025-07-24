package com.ordersystem.inventory.listener;

import com.ordersystem.inventory.service.InventoryService;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventListener {

    @Autowired
    private InventoryService inventoryService;

    @RabbitListener(queues = "#{inventoryQueue.name}")
    public void handleOrderCreated(OrderCreatedEvent orderEvent) {
        System.out.println("Received order created event for order: " + orderEvent.getOrderId());
        inventoryService.reserveInventory(orderEvent);
    }

    @RabbitListener(queues = "#{inventoryQueue.name}")
    public void handlePaymentProcessed(PaymentProcessedEvent paymentEvent) {
        System.out.println("Received payment processed event for order: " + paymentEvent.getOrderId());
        inventoryService.processPaymentResult(paymentEvent);
    }
}