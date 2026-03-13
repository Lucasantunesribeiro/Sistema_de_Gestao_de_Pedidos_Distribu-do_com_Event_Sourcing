package com.ordersystem.order.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty String customerId,
        @NotEmpty List<OrderLine> items
) {
    public record OrderLine(
            @NotEmpty String productId,
            @NotNull @Min(1) Integer quantity,
            @NotNull BigDecimal price
    ) {}
}
