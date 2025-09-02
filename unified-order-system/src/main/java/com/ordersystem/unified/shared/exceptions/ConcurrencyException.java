package com.ordersystem.unified.shared.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when concurrent access conflicts occur.
 */
public class ConcurrencyException extends OrderSystemException {
    
    public ConcurrencyException(String resource, String operation) {
        super(String.format("Concurrency conflict on %s during %s operation", resource, operation),
              "CONCURRENCY_CONFLICT", 
              HttpStatus.CONFLICT, 
              resource, operation);
    }

    public ConcurrencyException(String resource, String operation, Throwable cause) {
        super(String.format("Concurrency conflict on %s during %s operation", resource, operation),
              "CONCURRENCY_CONFLICT", 
              HttpStatus.CONFLICT, 
              cause, 
              resource, operation);
    }
}