package com.ordersystem.unified.shared.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an order contains invalid data or violates business rules.
 */
public class InvalidOrderException extends OrderSystemException {
    
    public InvalidOrderException(String message) {
        super(message, "INVALID_ORDER", HttpStatus.BAD_REQUEST);
    }

    public InvalidOrderException(String message, Throwable cause) {
        super(message, "INVALID_ORDER", HttpStatus.BAD_REQUEST, cause);
    }
}