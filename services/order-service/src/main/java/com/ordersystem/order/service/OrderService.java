package com.ordersystem.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.order.dto.CreateOrderRequest;
import com.ordersystem.order.entity.OrderEvent;
import com.ordersystem.order.model.OrderAggregate;
import com.ordersystem.order.repository.OrderEventRepository;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public String createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        List<OrderCreatedEvent.OrderItem> items = request.getItems().stream()
            .map(item -> new OrderCreatedEvent.OrderItem(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
            ))
            .collect(Collectors.toList());

        double totalAmount = items.stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();

        OrderCreatedEvent event = new OrderCreatedEvent(
            orderId,
            request.getCustomerId(),
            items,
            totalAmount,
            LocalDateTime.now()
        );

        saveEvent(orderId, "OrderCreatedEvent", event, 1L);
        publishEvent("order.events", event);

        return orderId;
    }

    public void updateOrderStatus(String orderId, String newStatus) {
        OrderAggregate aggregate = getOrderAggregate(orderId);
        if (aggregate == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
            orderId,
            aggregate.getStatus(),
            newStatus,
            LocalDateTime.now()
        );

        saveEvent(orderId, "OrderStatusUpdatedEvent", event, aggregate.getVersion() + 1);
        publishEvent("order.events", event);
    }

    public OrderAggregate getOrderAggregate(String orderId) {
        List<OrderEvent> events = orderEventRepository.findByOrderIdOrderByVersionAsc(orderId);
        if (events.isEmpty()) {
            return null;
        }

        OrderAggregate aggregate = new OrderAggregate();
        for (OrderEvent event : events) {
            applyEvent(aggregate, event);
        }
        return aggregate;
    }

    private void saveEvent(String orderId, String eventType, Object event, Long version) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            OrderEvent orderEvent = new OrderEvent(
                orderId,
                eventType,
                eventData,
                LocalDateTime.now(),
                version
            );
            orderEventRepository.save(orderEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing event", e);
        }
    }

    private void publishEvent(String routingKey, Object event) {
        rabbitTemplate.convertAndSend("order.fanout", routingKey, event);
    }

    private void applyEvent(OrderAggregate aggregate, OrderEvent event) {
        try {
            switch (event.getEventType()) {
                case "OrderCreatedEvent":
                    OrderCreatedEvent createdEvent = objectMapper.readValue(
                        event.getEventData(), OrderCreatedEvent.class);
                    aggregate.apply(createdEvent);
                    break;
                case "OrderStatusUpdatedEvent":
                    OrderStatusUpdatedEvent statusEvent = objectMapper.readValue(
                        event.getEventData(), OrderStatusUpdatedEvent.class);
                    aggregate.apply(statusEvent);
                    break;
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing event", e);
        }
    }
}