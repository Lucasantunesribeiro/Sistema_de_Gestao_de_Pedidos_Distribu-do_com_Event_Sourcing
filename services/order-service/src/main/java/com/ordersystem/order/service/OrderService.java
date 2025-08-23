package com.ordersystem.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.order.model.Order;
import com.ordersystem.order.model.OrderEvent;
import com.ordersystem.order.model.OrderStatus;
import com.ordersystem.order.repository.OrderEventRepository;
import com.ordersystem.order.repository.OrderRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderEventRepository eventRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String ORDER_EXCHANGE = "order.exchange";
    private static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    private static final String ORDER_UPDATED_ROUTING_KEY = "order.updated";
    
    public Order createOrder(String customerId, Double totalAmount, List<String> productIds) {
        String orderId = UUID.randomUUID().toString();
        
        // Create order aggregate
        Order order = new Order(customerId, totalAmount);
        order.setId(orderId);
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // Create and store event
        publishOrderCreatedEvent(savedOrder);
        
        return savedOrder;
    }
    
    public Optional<Order> findById(String id) {
        return orderRepository.findById(id);
    }
    
    public List<Order> findByCustomerId(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
    
    public List<Order> findAll() {
        return orderRepository.findAll();
    }
    
    public Order updateOrderStatus(String orderId, String status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            String previousStatus = order.getStatus().name();
            
            // Update status
            try {
                order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + status);
            }
            
            Order savedOrder = orderRepository.save(order);
            
            // Publish status updated event
            publishOrderStatusUpdatedEvent(savedOrder, previousStatus, status);
            
            return savedOrder;
        }
        
        throw new RuntimeException("Order not found: " + orderId);
    }
    
    private void publishOrderCreatedEvent(Order order) {
        try {
            OrderEvent event = new OrderEvent(
                order.getId(),
                "ORDER_CREATED",
                objectMapper.writeValueAsString(order)
            );
            
            eventRepository.save(event);
            
            // Publish to RabbitMQ
            rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_CREATED_ROUTING_KEY, order);
            
        } catch (Exception e) {
            System.err.println("Error publishing order created event: " + e.getMessage());
        }
    }
    
    private void publishOrderStatusUpdatedEvent(Order order, String previousStatus, String newStatus) {
        try {
            String eventData = String.format(
                "{\"orderId\":\"%s\",\"previousStatus\":\"%s\",\"newStatus\":\"%s\",\"customerId\":\"%s\"}",
                order.getId(), previousStatus, newStatus, order.getCustomerId()
            );
            
            OrderEvent event = new OrderEvent(
                order.getId(),
                "ORDER_STATUS_UPDATED",
                eventData
            );
            
            eventRepository.save(event);
            
            // Publish to RabbitMQ
            rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_UPDATED_ROUTING_KEY, eventData);
            
        } catch (Exception e) {
            System.err.println("Error publishing order status updated event: " + e.getMessage());
        }
    }
    
    public List<OrderEvent> getOrderEvents(String orderId) {
        return eventRepository.findByAggregateIdOrderByOccurredAtAsc(orderId);
    }
}