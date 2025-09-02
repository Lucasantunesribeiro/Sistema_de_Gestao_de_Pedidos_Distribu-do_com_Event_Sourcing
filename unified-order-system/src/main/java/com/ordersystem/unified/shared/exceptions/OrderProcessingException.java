package com.ordersystem.unified.shared.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when order processing fails due to business logic errors.
 */
public class OrderProcessingException extends OrderSystemException {
    
    public OrderProcessingException(String orderId, String reason) {
        super(String.format("Order processing failed for order %s: %s", orderId, reason),
              "ORDER_PROCESSING_FAILED", 
              HttpStatus.UNPROCESSABLE_ENTITY, 
              orderId, reason);
    }

    public OrderProcessingException(String orderId, String reason, Throwable cause) {
        super(String.format("Order processing failed for order %s: %s", orderId, reason),
              "ORDER_PROCESSING_FAILED", 
              HttpStatus.UNPROCESSABLE_ENTITY, 
              cause, 
              orderId, reason);
    }

    public OrderProcessingException(String reason) {
        super(String.format("Order processing failed: %s", reason),
              "ORDER_PROCESSING_FAILED", 
              HttpStatus.UNPROCESSABLE_ENTITY, 
              reason);
    }
}