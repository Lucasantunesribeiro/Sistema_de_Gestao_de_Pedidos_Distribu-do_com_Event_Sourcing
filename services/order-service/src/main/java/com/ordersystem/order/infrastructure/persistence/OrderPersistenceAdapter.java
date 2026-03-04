package com.ordersystem.order.infrastructure.persistence;

import com.ordersystem.common.security.SafeEnumParser;
import com.ordersystem.order.application.port.OrderRepositoryPort;
import com.ordersystem.order.domain.Order;
import com.ordersystem.order.domain.OrderItem;
import com.ordersystem.order.domain.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository JpaRepository;

    public OrderPersistenceAdapter(OrderJpaRepository JpaRepository) {
        this.JpaRepository = JpaRepository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity saved = JpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return JpaRepository.findById(orderId).map(this::toDomain);
    }

    private OrderEntity toEntity(Order order) {
        List<OrderEntity.OrderItemEmbeddable> items = order.getItems().stream()
                .map(i -> new OrderEntity.OrderItemEmbeddable(i.getProductId(), i.getQuantity(), i.getPrice()))
                .toList();
        return new OrderEntity(order.getId(), order.getCustomerId(), order.getStatus().name(), order.getCreatedAt(), items);
    }

    private Order toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(e -> new OrderItem(e.getProductId(), e.getQuantity(), e.getPrice()))
                .toList();
        return Order.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .status(SafeEnumParser.parseEnumOrThrow(OrderStatus.class, entity.getStatus(), "status"))
                .createdAt(entity.getCreatedAt())
                .items(items)
                .build();
    }
}
