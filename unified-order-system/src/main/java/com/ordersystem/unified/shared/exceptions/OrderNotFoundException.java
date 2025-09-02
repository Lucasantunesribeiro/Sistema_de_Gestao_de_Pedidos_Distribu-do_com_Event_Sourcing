package com.ordersystem.unified.shared.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested order cannot be found.
 */
public class OrderNotFoundException extends OrderSystemException {
    
    public OrderNotFoundException(String orderId) {
        super(String.format("Order not found with ID: %s", orderId), 
              "ORDER_NOT_FOUND", 
              HttpStatus.NOT_FOUND, 
              orderId);
    }
}