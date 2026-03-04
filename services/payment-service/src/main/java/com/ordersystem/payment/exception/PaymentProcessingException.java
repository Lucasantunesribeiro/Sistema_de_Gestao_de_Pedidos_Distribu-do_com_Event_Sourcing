package com.ordersystem.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PaymentProcessingException extends RuntimeException {
    
    private final String orderId;
    private final String errorCode;

    public PaymentProcessingException(String orderId, String message) {
        super(message);
        this.orderId = orderId;
        this.errorCode = "PAYMENT_PROCESSING_ERROR";
    }

    public PaymentProcessingException(String orderId, String message, String errorCode) {
        super(message);
        this.orderId = orderId;
        this.errorCode = errorCode;
    }

    public PaymentProcessingException(String orderId, String message, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
        this.errorCode = "PAYMENT_PROCESSING_ERROR";
    }

    public String getOrderId() { return orderId; }
    public String getErrorCode() { return errorCode; }
}
