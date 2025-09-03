package com.ordersystem.unified.inventory.dto;

/**
 * Result of individual item reservation
 */
public class ItemReservationResult {
    
    private String productId;
    private Integer requestedQuantity;
    private Integer reservedQuantity;
    private Integer availableStock;
    private boolean success;
    private String message;
    private String warehouseId;
    
    // Constructors
    public ItemReservationResult() {}
    
    public ItemReservationResult(String productId, Integer requestedQuantity, Integer reservedQuantity, boolean success, String message) {
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.reservedQuantity = reservedQuantity;
        this.success = success;
        this.message = message;
    }
    
    // Static factory methods
    public static ItemReservationResult success(String productId, Integer quantity, Integer availableStock, String warehouseId) {
        ItemReservationResult result = new ItemReservationResult(productId, quantity, quantity, true, "Successfully reserved");
        result.setAvailableStock(availableStock);
        result.setWarehouseId(warehouseId);
        return result;
    }
    
    public static ItemReservationResult partial(String productId, Integer requested, Integer reserved, Integer availableStock, String warehouseId) {
        ItemReservationResult result = new ItemReservationResult(productId, requested, reserved, false, 
            String.format("Partially reserved: %d of %d requested", reserved, requested));
        result.setAvailableStock(availableStock);
        result.setWarehouseId(warehouseId);
        return result;
    }
    
    public static ItemReservationResult failure(String productId, Integer requested, String reason) {
        return new ItemReservationResult(productId, requested, 0, false, reason);
    }
    
    public static ItemReservationResult insufficientStock(String productId, Integer requested, Integer available) {
        return new ItemReservationResult(productId, requested, 0, false, 
            String.format("Insufficient stock: requested %d, available %d", requested, available));
    }
    
    // Getters and Setters
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }
    
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    public Integer getAvailableStock() {
        return availableStock;
    }
    
    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    public boolean isPartialReservation() {
        return reservedQuantity > 0 && reservedQuantity < requestedQuantity;
    }
    
    public boolean isFullReservation() {
        return success && reservedQuantity.equals(requestedQuantity);
    }
    
    @Override
    public String toString() {
        return "ItemReservationResult{" +
                "productId='" + productId + '\'' +
                ", requestedQuantity=" + requestedQuantity +
                ", reservedQuantity=" + reservedQuantity +
                ", availableStock=" + availableStock +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", warehouseId='" + warehouseId + '\'' +
                '}';
    }
}