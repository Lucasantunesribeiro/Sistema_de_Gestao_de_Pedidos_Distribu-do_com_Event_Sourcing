package com.ordersystem.order.service;

import com.ordersystem.order.controller.OrderController.CreateOrderRequest;
import com.ordersystem.order.controller.OrderController.OrderItemRequest;
import com.ordersystem.order.model.Order;
import com.ordersystem.order.repository.OrderRepository;
import com.ordersystem.shared.events.OrderCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    public Order createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        List<com.ordersystem.order.model.OrderItem> items = request.getItems().stream()
            .map(item -> new com.ordersystem.order.model.OrderItem(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
            ))
            .collect(Collectors.toList());

        Order order = new Order(orderId, request.getCustomerId(), items);
        Order savedOrder = orderRepository.save(order);

        // Publish event
        publishOrderCreatedEvent(savedOrder);

        return savedOrder;
    }

    public void cancelOrder(String orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(com.ordersystem.order.model.OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
    }

    private void publishOrderCreatedEvent(Order order) {
        try {
            List<com.ordersystem.shared.events.OrderItem> eventItems = order.getItems().stream()
                .map(item -> new com.ordersystem.shared.events.OrderItem(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getPrice()
                ))
                .collect(Collectors.toList());

            OrderCreatedEvent event = new OrderCreatedEvent(
                order.getOrderId(),
                order.getCustomerId(),
                eventItems,
                order.getTotalAmount(),
                LocalDateTime.now()
            );

            rabbitTemplate.convertAndSend("order.fanout", "", event);
        } catch (Exception e) {
            // Log error but don't fail the order creation
            System.err.println("Failed to publish OrderCreatedEvent: " + e.getMessage());
        }
    }
}