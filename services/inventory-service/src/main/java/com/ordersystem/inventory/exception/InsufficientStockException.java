package com.ordersystem.inventory.exception;

/**
 * Exception thrown when there's insufficient stock for a reservation
 */
public class InsufficientStockException extends InventoryException {
    
    private final String productId;
    private final int requestedQuantity;
    private final int availableQuantity;
    
    public InsufficientStockException(String productId, int requestedQuantity, int availableQuantity) {
        super(String.format("Insufficient stock for product %s: requested %d, available %d", 
              productId, requestedQuantity, availableQuantity), "INSUFFICIENT_STOCK");
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public int getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public int getAvailableQuantity() {
        return availableQuantity;
    }
}