package com.ordersystem.unified.inventory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Inventory controller for stock operations.
 * Minimal version for deployment compatibility.
 */
public class InventoryController {

    private InventoryService inventoryService = new InventoryService();

    public Map<String, Object> checkStock(String productId, int quantity) {
        return inventoryService.checkAvailability(productId, quantity);
    }

    public Map<String, Object> reserveStock(String orderId, Map<String, Object> items) {
        return inventoryService.reserveItems(orderId, items);
    }

    public Map<String, Object> releaseStock(String reservationId) {
        return inventoryService.releaseReservation(reservationId);
    }

    public Map<String, Object> getInventoryStatus() {
        return inventoryService.getInventoryStatus();
    }

    public List<Map<String, Object>> getAllProducts() {
        List<Map<String, Object>> products = new ArrayList<>();
        
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> product = new HashMap<>();
            product.put("productId", "PROD-" + i);
            product.put("name", "Product " + i);
            product.put("stock", 50 + i);
            product.put("price", 10.0 * i);
            product.put("timestamp", System.currentTimeMillis());
            products.add(product);
        }
        
        return products;
    }

    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "inventory-service");
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
}