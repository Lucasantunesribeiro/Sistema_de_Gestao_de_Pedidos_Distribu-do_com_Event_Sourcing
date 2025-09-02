package com.ordersystem.unified.shared.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when there is insufficient inventory to fulfill an order.
 */
public class InsufficientInventoryException extends OrderSystemException {
    
    public InsufficientInventoryException(String productId, int requested, int available) {
        super(String.format("Insufficient inventory for product %s: requested %d, available %d", 
                           productId, requested, available),
              "INSUFFICIENT_INVENTORY", 
              HttpStatus.BAD_REQUEST, 
              productId, requested, available);
    }

    public InsufficientInventoryException(String productId) {
        super(String.format("Insufficient inventory for product: %s", productId),
              "INSUFFICIENT_INVENTORY", 
              HttpStatus.BAD_REQUEST, 
              productId);
    }
}