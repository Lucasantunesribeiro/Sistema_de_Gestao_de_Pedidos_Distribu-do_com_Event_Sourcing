package com.ordersystem.order.application.usecase;

import com.ordersystem.order.domain.OrderItem;

import java.util.List;

public final class CreateOrderCommand {
    private final String customerId;
    private final List<OrderItem> items;

    public CreateOrderCommand(String customerId, List<OrderItem> items) {
        this.customerId = customerId;
        this.items = List.copyOf(items);
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
