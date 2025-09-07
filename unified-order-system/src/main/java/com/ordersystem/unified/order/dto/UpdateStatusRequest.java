package com.ordersystem.unified.order.dto;

import jakarta.validation.constraints.NotNull;
import com.ordersystem.unified.shared.events.OrderStatus;

public class UpdateStatusRequest {
    @NotNull
    private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
