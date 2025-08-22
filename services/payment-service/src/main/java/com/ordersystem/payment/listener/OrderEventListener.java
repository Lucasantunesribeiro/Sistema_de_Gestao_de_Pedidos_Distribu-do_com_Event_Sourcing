package com.ordersystem.payment.listener;

import com.ordersystem.payment.service.PaymentService;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderItem;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class OrderEventListener {

    @Autowired
    private PaymentService paymentService;

    @RabbitListener(queues = "#{paymentQueue.name}")
    public void handleOrderCreated(OrderCreatedEvent orderEvent) {
        System.out.println("Received order created event for order: " + orderEvent.getOrderId());
        
        // Calculate total amount from order items
        BigDecimal totalAmount = orderEvent.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Process payment with default payment method
        paymentService.processPayment(orderEvent.getOrderId(), totalAmount, "CREDIT_CARD");
    }
}