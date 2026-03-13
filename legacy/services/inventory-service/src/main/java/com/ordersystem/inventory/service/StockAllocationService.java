package com.ordersystem.inventory.service;

import com.ordersystem.inventory.model.InventoryItem;
import com.ordersystem.inventory.model.StockMovement;
import com.ordersystem.inventory.repository.InventoryRepository;
import com.ordersystem.shared.events.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for stock allocation algorithms and business logic
 */
@Service
public class StockAllocationService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockAllocationService.class);
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    /**
     * FIFO allocation algorithm - allocates stock in order of availability
     */
    public AllocationResult allocateStock(List<OrderItem> items, String orderId, AllocationStrategy strategy) {
        logger.info("Allocating stock for order {} using strategy {}", orderId, strategy);
        
        List<StockMovement> movements = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        boolean success = true;
        
        // First pass: Check availability for all items
        for (OrderItem item : items) {
            InventoryItem inventoryItem = inventoryRepository.findByProductId(item.getProductId());
            
            if (inventoryItem == null) {
                failures.add("Product not found: " + item.getProductId());
                success = false;
                continue;
            }
            
            if (!inventoryItem.canReserve(item.getQuantity())) {
                failures.add(String.format("Insufficient stock for %s: requested %d, available %d", 
                    item.getProductId(), item.getQuantity(), inventoryItem.getAvailableQuantity()));
                success = false;
            }
        }
        
        // If all items can be allocated, proceed with reservation
        if (success) {
            for (OrderItem item : items) {
                InventoryItem inventoryItem = inventoryRepository.findByProductId(item.getProductId());
                
                int prevAvailable = inventoryItem.getAvailableQuantity();
                int prevReserved = inventoryItem.getReservedQuantity();
                
                // Atomic reservation
                synchronized (inventoryItem) {
                    inventoryItem.reserve(item.getQuantity());
                }
                
                // Create movement record
                StockMovement movement = new StockMovement(
                    UUID.randomUUID().toString(),
                    item.getProductId(),
                    StockMovement.MovementType.RESERVE,
                    item.getQuantity(),
                    orderId,
                    "Order reservation",
                    prevAvailable,
                    prevReserved,
                    inventoryItem.getAvailableQuantity(),
                    inventoryItem.getReservedQuantity()
                );
                
                movements.add(movement);
                
                logger.info("Reserved {} units of {} for order {}", 
                    item.getQuantity(), item.getProductId(), orderId);
                
                // Check for low stock alert
                if (inventoryItem.getAvailableQuantity() < getLowStockThreshold(item.getProductId())) {
                    logger.warn("Low stock alert for product {}: {} units remaining", 
                        item.getProductId(), inventoryItem.getAvailableQuantity());
                }
            }
        }
        
        return new AllocationResult(success, movements, failures);
    }
    
    /**
     * Release stock allocation (compensation action)
     */
    public void releaseStock(List<OrderItem> items, String orderId, String reason) {
        logger.info("Releasing stock for order {} - reason: {}", orderId, reason);
        
        for (OrderItem item : items) {
            InventoryItem inventoryItem = inventoryRepository.findByProductId(item.getProductId());
            
            if (inventoryItem != null) {
                int prevAvailable = inventoryItem.getAvailableQuantity();
                int prevReserved = inventoryItem.getReservedQuantity();
                
                // Atomic release
                synchronized (inventoryItem) {
                    inventoryItem.release(item.getQuantity());
                }
                
                logger.info("Released {} units of {} for order {}", 
                    item.getQuantity(), item.getProductId(), orderId);
            }
        }
    }
    
    /**
     * Confirm stock allocation (final commitment)
     */
    public void confirmStock(List<OrderItem> items, String orderId) {
        logger.info("Confirming stock allocation for order {}", orderId);
        
        for (OrderItem item : items) {
            InventoryItem inventoryItem = inventoryRepository.findByProductId(item.getProductId());
            
            if (inventoryItem != null) {
                int prevReserved = inventoryItem.getReservedQuantity();
                
                // Atomic confirmation
                synchronized (inventoryItem) {
                    inventoryItem.confirm(item.getQuantity());
                }
                
                logger.info("Confirmed {} units of {} for order {}", 
                    item.getQuantity(), item.getProductId(), orderId);
            }
        }
    }
    
    /**
     * Check stock availability without allocation
     */
    public boolean checkAvailability(List<OrderItem> items) {
        for (OrderItem item : items) {
            InventoryItem inventoryItem = inventoryRepository.findByProductId(item.getProductId());
            
            if (inventoryItem == null || !inventoryItem.canReserve(item.getQuantity())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Restock inventory
     */
    public void restock(String productId, int quantity, String reason) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(productId);
        
        if (inventoryItem != null) {
            synchronized (inventoryItem) {
                inventoryItem.setAvailableQuantity(inventoryItem.getAvailableQuantity() + quantity);
            }
            
            logger.info("Restocked {} units of {} - reason: {}", quantity, productId, reason);
        }
    }
    
    private int getLowStockThreshold(String productId) {
        // Business rule: different thresholds for different product types
        return 5; // Default threshold
    }
    
    public enum AllocationStrategy {
        FIFO,           // First In, First Out
        LIFO,           // Last In, First Out
        CLOSEST_EXPIRY, // Allocate items closest to expiry first
        MULTI_LOCATION  // Allocate from multiple warehouse locations
    }
    
    public static class AllocationResult {
        private final boolean success;
        private final List<StockMovement> movements;
        private final List<String> failures;
        
        public AllocationResult(boolean success, List<StockMovement> movements, List<String> failures) {
            this.success = success;
            this.movements = movements;
            this.failures = failures;
        }
        
        public boolean isSuccess() { return success; }
        public List<StockMovement> getMovements() { return movements; }
        public List<String> getFailures() { return failures; }
    }
}