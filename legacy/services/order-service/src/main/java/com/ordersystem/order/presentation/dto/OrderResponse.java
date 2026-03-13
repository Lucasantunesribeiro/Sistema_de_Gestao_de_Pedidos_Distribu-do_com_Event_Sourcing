package com.ordersystem.order.presentation.dto;

import com.ordersystem.order.domain.OrderItem;
import com.ordersystem.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        String id,
        String customerId,
        List<OrderLine> items,
        OrderStatus status,
        OffsetDateTime createdAt
) {
    public record OrderLine(
            String productId,
            int quantity,
            BigDecimal price
    ) {}

    public static OrderResponse fromDomain(com.ordersystem.order.domain.Order order) {
        List<OrderLine> lines = order.getItems().stream()
                .map(item -> new OrderLine(item.getProductId(), item.getQuantity(), item.getPrice()))
                .toList();
        return new OrderResponse(order.getId(), order.getCustomerId(), lines, order.getStatus(), order.getCreatedAt());
    }
}
