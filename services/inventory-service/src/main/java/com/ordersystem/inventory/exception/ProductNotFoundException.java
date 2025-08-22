package com.ordersystem.inventory.exception;

/**
 * Exception thrown when a product is not found in inventory
 */
public class ProductNotFoundException extends InventoryException {
    
    private final String productId;
    
    public ProductNotFoundException(String productId) {
        super(String.format("Product not found in inventory: %s", productId), "PRODUCT_NOT_FOUND");
        this.productId = productId;
    }
    
    public String getProductId() {
        return productId;
    }
}