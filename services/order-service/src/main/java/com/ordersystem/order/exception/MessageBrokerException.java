package com.ordersystem.order.exception;

public class MessageBrokerException extends RuntimeException {

    public MessageBrokerException(String message) {
        super(message);
    }

    public MessageBrokerException(String message, Throwable cause) {
        super(message, cause);
    }
}