package com.ordersystem.order.exception;

import com.ordersystem.shared.events.OrderStatus;

public class InvalidOrderStateException extends RuntimeException {
    
    private final String orderId;
    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;

    public InvalidOrderStateException(String orderId, OrderStatus currentStatus, OrderStatus targetStatus) {
        super(String.format("Não é possível alterar o status do pedido %s de %s para %s", 
                          orderId, currentStatus, targetStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public InvalidOrderStateException(String orderId, OrderStatus currentStatus, String message) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.targetStatus = null;
    }

    public String getOrderId() { return orderId; }
    public OrderStatus getCurrentStatus() { return currentStatus; }
    public OrderStatus getTargetStatus() { return targetStatus; }
}