package com.ordersystem.order.application.usecase;

import com.ordersystem.order.domain.Order;

public interface CreateOrderUseCase {
    Order execute(CreateOrderCommand command);
}
