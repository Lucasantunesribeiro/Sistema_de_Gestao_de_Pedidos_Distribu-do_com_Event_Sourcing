package com.ordersystem.query.listener;

import com.ordersystem.query.service.OrderQueryService;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventListener {

    @Autowired
    private OrderQueryService orderQueryService;

    @RabbitListener(queues = "#{queryQueue.name}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        System.out.println("Query service received order created event: " + event.getOrderId());
        orderQueryService.handleOrderCreated(event);
    }

    @RabbitListener(queues = "#{queryQueue.name}")
    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        System.out.println("Query service received order status updated event: " + event.getOrderId());
        orderQueryService.handleOrderStatusUpdated(event);
    }

    @RabbitListener(queues = "#{queryQueue.name}")
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        System.out.println("Query service received payment processed event: " + event.getOrderId());
        orderQueryService.handlePaymentProcessed(event);
    }
}