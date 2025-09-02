package com.ordersystem.unified.shared.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when payment processing fails.
 */
public class PaymentProcessingException extends OrderSystemException {
    
    public PaymentProcessingException(String reason) {
        super(String.format("Payment processing failed: %s", reason),
              "PAYMENT_FAILED", 
              HttpStatus.PAYMENT_REQUIRED, 
              reason);
    }

    public PaymentProcessingException(String reason, Throwable cause) {
        super(String.format("Payment processing failed: %s", reason),
              "PAYMENT_FAILED", 
              HttpStatus.PAYMENT_REQUIRED, 
              cause, 
              reason);
    }
}