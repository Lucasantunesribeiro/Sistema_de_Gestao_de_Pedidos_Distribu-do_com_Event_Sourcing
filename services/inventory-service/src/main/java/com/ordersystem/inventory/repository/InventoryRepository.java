package com.ordersystem.inventory.repository;

import com.ordersystem.inventory.model.InventoryItem;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Repository for inventory items with thread-safe operations
 */
@Repository
public class InventoryRepository {
    
    private final ConcurrentMap<String, InventoryItem> inventory = new ConcurrentHashMap<>();
    
    public InventoryRepository() {
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        // Initialize with more comprehensive sample data
        inventory.put("product-1", new InventoryItem("product-1", "Dell XPS 13 Laptop", 15, 0));
        inventory.put("product-2", new InventoryItem("product-2", "Logitech MX Master 3 Mouse", 75, 0));
        inventory.put("product-3", new InventoryItem("product-3", "Mechanical Keyboard RGB", 30, 0));
        inventory.put("product-4", new InventoryItem("product-4", "4K Monitor 27 inch", 8, 0));
        inventory.put("product-5", new InventoryItem("product-5", "Webcam HD 1080p", 25, 0));
        inventory.put("product-6", new InventoryItem("product-6", "Headphones Noise Cancelling", 20, 0));
        inventory.put("product-7", new InventoryItem("product-7", "External SSD 1TB", 40, 0));
        inventory.put("product-8", new InventoryItem("product-8", "Gaming Chair Ergonomic", 5, 0));
        inventory.put("product-9", new InventoryItem("product-9", "Desk Lamp LED", 18, 0));
        inventory.put("product-10", new InventoryItem("product-10", "Smartphone Case", 60, 0));
    }
    
    public InventoryItem findByProductId(String productId) {
        return inventory.get(productId);
    }
    
    public Collection<InventoryItem> findAll() {
        return inventory.values();
    }
    
    public void save(InventoryItem item) {
        inventory.put(item.getProductId(), item);
    }
    
    public boolean exists(String productId) {
        return inventory.containsKey(productId);
    }
    
    public void delete(String productId) {
        inventory.remove(productId);
    }
    
    public int getTotalProducts() {
        return inventory.size();
    }
    
    public long getTotalAvailableStock() {
        return inventory.values().stream()
                .mapToLong(InventoryItem::getAvailableQuantity)
                .sum();
    }
    
    public long getTotalReservedStock() {
        return inventory.values().stream()
                .mapToLong(InventoryItem::getReservedQuantity)
                .sum();
    }
}