package com.ordersystem.payment.service;

import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final Random random = new Random();

    public void processPayment(OrderCreatedEvent orderEvent) {
        // Simulate payment processing delay
        try {
            Thread.sleep(2000 + random.nextInt(3000)); // 2-5 seconds delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate payment success/failure (80% success rate)
        String paymentStatus = random.nextDouble() < 0.8 ? "APPROVED" : "DECLINED";
        
        String paymentId = UUID.randomUUID().toString();
        
        PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(
            orderEvent.getOrderId(),
            paymentId,
            paymentStatus,
            orderEvent.getTotalAmount().doubleValue(),
            LocalDateTime.now()
        );

        // Publish payment processed event
        rabbitTemplate.convertAndSend("order.fanout", "payment.processed", paymentEvent);
        
        System.out.println("Payment processed for order: " + orderEvent.getOrderId() + 
                         " with status: " + paymentStatus);
    }
}