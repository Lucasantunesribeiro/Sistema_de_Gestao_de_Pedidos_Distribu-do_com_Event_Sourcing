package com.ordersystem.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class MessageBrokerException extends RuntimeException {

    public MessageBrokerException(String message) {
        super(message);
    }

    public MessageBrokerException(String message, Throwable cause) {
        super(message, cause);
    }
}
