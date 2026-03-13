package com.ordersystem.inventory.exception;

/**
 * Base exception for inventory-related errors
 */
public class InventoryException extends RuntimeException {
    
    private final String errorCode;
    
    public InventoryException(String message) {
        super(message);
        this.errorCode = "INVENTORY_ERROR";
    }
    
    public InventoryException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public InventoryException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVENTORY_ERROR";
    }
    
    public InventoryException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}