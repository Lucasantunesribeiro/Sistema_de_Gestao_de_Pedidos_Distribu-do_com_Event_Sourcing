package com.ordersystem.order.exception;

public class OrderNotFoundException extends RuntimeException {
    
    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Pedido não encontrado: " + orderId);
        this.orderId = orderId;
    }

    public OrderNotFoundException(String orderId, String message) {
        super(message);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}