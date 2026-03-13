package com.ordersystem.order.application.port;

import com.ordersystem.order.domain.Order;

import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(Order order);

    Optional<Order> findById(String orderId);
}
