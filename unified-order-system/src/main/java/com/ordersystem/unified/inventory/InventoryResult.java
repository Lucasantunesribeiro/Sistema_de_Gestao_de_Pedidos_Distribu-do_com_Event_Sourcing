package com.ordersystem.unified.inventory;

import com.ordersystem.unified.shared.events.InventoryStatus;

/**
 * Result of an inventory operation.
 */
public class InventoryResult {
    
    private final boolean success;
    private final InventoryStatus status;
    private final String message;
    private final String productId;
    private final Integer requestedQuantity;
    private final Integer availableQuantity;

    private InventoryResult(boolean success, InventoryStatus status, String message, 
                          String productId, Integer requestedQuantity, Integer availableQuantity) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public static InventoryResult success(String productId, Integer quantity) {
        return new InventoryResult(true, InventoryStatus.RESERVED, 
                "Inventory reserved successfully", productId, quantity, null);
    }

    public static InventoryResult insufficientStock(String productId, Integer requested, Integer available) {
        return new InventoryResult(false, InventoryStatus.INSUFFICIENT, 
                String.format("Insufficient stock for product %s: requested %d, available %d", 
                             productId, requested, available),
                productId, requested, available);
    }

    public static InventoryResult outOfStock(String productId) {
        return new InventoryResult(false, InventoryStatus.OUT_OF_STOCK, 
                "Product is out of stock", productId, null, 0);
    }

    public static InventoryResult released(String productId, Integer quantity) {
        return new InventoryResult(true, InventoryStatus.RELEASED, 
                "Inventory released successfully", productId, quantity, null);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public InventoryStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public String getProductId() { return productId; }
    public Integer getRequestedQuantity() { return requestedQuantity; }
    public Integer getAvailableQuantity() { return availableQuantity; }

    @Override
    public String toString() {
        return String.format("InventoryResult{success=%s, status=%s, productId='%s', message='%s'}",
                success, status, productId, message);
    }
}