package com.ordersystem.inventory.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InventoryItem {
    private String productId;
    private String productName;
    private int availableQuantity;
    private int reservedQuantity;

    // In-memory storage for demo purposes
    private static final ConcurrentMap<String, InventoryItem> inventory = new ConcurrentHashMap<>();

    static {
        // Initialize some sample inventory
        inventory.put("product-1", new InventoryItem("product-1", "Laptop", 10, 0));
        inventory.put("product-2", new InventoryItem("product-2", "Mouse", 50, 0));
        inventory.put("product-3", new InventoryItem("product-3", "Keyboard", 25, 0));
    }

    public InventoryItem() {}

    public InventoryItem(String productId, String productName, int availableQuantity, int reservedQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    public static ConcurrentMap<String, InventoryItem> getInventory() {
        return inventory;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }

    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public boolean canReserve(int quantity) {
        return availableQuantity >= quantity;
    }

    public synchronized void reserve(int quantity) {
        if (canReserve(quantity)) {
            availableQuantity -= quantity;
            reservedQuantity += quantity;
        }
    }

    public synchronized void release(int quantity) {
        if (reservedQuantity >= quantity) {
            reservedQuantity -= quantity;
            availableQuantity += quantity;
        }
    }

    public synchronized void confirm(int quantity) {
        if (reservedQuantity >= quantity) {
            reservedQuantity -= quantity;
        }
    }
}