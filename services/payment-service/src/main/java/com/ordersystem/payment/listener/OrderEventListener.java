package com.ordersystem.payment.listener;

import com.ordersystem.payment.service.PaymentService;
import com.ordersystem.shared.events.OrderCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    @Autowired
    private PaymentService paymentService;

    @RabbitListener(queues = "#{paymentQueue.name}")
    public void handleOrderCreated(OrderCreatedEvent orderEvent) {
        System.out.println("Received order created event for order: " + orderEvent.getOrderId());
        paymentService.processPayment(orderEvent);
    }
}