package com.ordersystem.order.application;

import com.ordersystem.order.application.port.OrderRepositoryPort;
import com.ordersystem.order.application.usecase.CreateOrderCommand;
import com.ordersystem.order.application.usecase.CreateOrderUseCase;
import com.ordersystem.order.domain.Order;
import com.ordersystem.order.domain.OrderItem;
import com.ordersystem.order.domain.OrderStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderApplicationService implements CreateOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public OrderApplicationService(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    @Override
    public Order execute(CreateOrderCommand command) {
        Order order = Order.builder()
                .customerId(command.getCustomerId())
                .items(command.getItems())
                .status(OrderStatus.PENDING)
                .build();
        return orderRepositoryPort.save(order);
    }
}
