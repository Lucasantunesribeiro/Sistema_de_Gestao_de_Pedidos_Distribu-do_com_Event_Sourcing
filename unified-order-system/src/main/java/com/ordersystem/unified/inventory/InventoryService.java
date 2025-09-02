package com.ordersystem.unified.inventory;

import com.ordersystem.unified.inventory.model.Inventory;
import com.ordersystem.unified.inventory.repository.InventoryRepository;
import com.ordersystem.unified.shared.events.InventoryReservedEvent;
import com.ordersystem.unified.shared.events.OrderItem;
import com.ordersystem.unified.shared.exceptions.InventoryReservationException;
import com.ordersystem.unified.shared.exceptions.InsufficientInventoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing inventory in the unified system.
 * Handles inventory reservation, release, and stock management with database persistence.
 */
@Service
@Transactional
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private InventoryRepository inventoryRepository;

    /**
     * Reserves inventory items for an order with proper concurrency handling.
     */
    public InventoryResult reserveItems(List<OrderItem> items) {
        return reserveItems(items, null);
    }

    /**
     * Reserves inventory items for an order with correlation ID for tracing.
     */
    public InventoryResult reserveItems(List<OrderItem> items, String correlationId) {
        logger.info("Reserving inventory for {} items, correlationId: {}", items.size(), correlationId);

        try {
            // Get product IDs for locking
            List<String> productIds = items.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());

            // Fetch inventory items with pessimistic locking to prevent concurrent modifications
            List<Inventory> inventoryItems = inventoryRepository.findByProductIdInWithLock(productIds);

            // Check if all products exist
            if (inventoryItems.size() != productIds.size()) {
                List<String> foundIds = inventoryItems.stream()
                    .map(Inventory::getProductId)
                    .collect(Collectors.toList());
                List<String> missingIds = productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
                
                logger.warn("Products not found in inventory: {}", missingIds);
                return InventoryResult.insufficientStock(missingIds.get(0), 0, 0);
            }

            // Check availability for all items first (fail fast)
            for (OrderItem item : items) {
                Inventory inventory = inventoryItems.stream()
                    .filter(inv -> inv.getProductId().equals(item.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Inventory item not found: " + item.getProductId()));

                if (!inventory.canReserve(item.getQuantity())) {
                    logger.warn("Insufficient inventory for product: {}, requested: {}, available: {}", 
                               item.getProductId(), item.getQuantity(), inventory.getAvailableQuantity());
                    throw new InsufficientInventoryException(item.getProductId(), 
                                                           item.getQuantity(), inventory.getAvailableQuantity());
                }
            }

            // Reserve all items (all checks passed)
            List<Inventory> updatedInventory = new ArrayList<>();
            for (OrderItem item : items) {
                Inventory inventory = inventoryItems.stream()
                    .filter(inv -> inv.getProductId().equals(item.getProductId()))
                    .findFirst()
                    .orElseThrow();

                inventory.reserve(item.getQuantity());
                updatedInventory.add(inventory);
                
                logger.debug("Reserved {} units of product: {}, remaining available: {}", 
                           item.getQuantity(), item.getProductId(), inventory.getAvailableQuantity());
            }

            // Save all updated inventory items
            inventoryRepository.saveAll(updatedInventory);

            // Publish inventory reserved event
            String reservationId = UUID.randomUUID().toString();
            publishInventoryReservedEvent(items, reservationId, correlationId);

            logger.info("Successfully reserved inventory for {} items, reservationId: {}, correlationId: {}", 
                       items.size(), reservationId, correlationId);
            
            return InventoryResult.success("multiple", items.size());

        } catch (InsufficientInventoryException e) {
            logger.warn("Inventory reservation failed: {}", e.getMessage());
            return InventoryResult.insufficientStock(e.getParameters()[0].toString(), 
                                                   (Integer) e.getParameters()[1], 
                                                   (Integer) e.getParameters()[2]);
        } catch (Exception e) {
            logger.error("Error reserving inventory, correlationId: {}", correlationId, e);
            throw new InventoryReservationException("multiple", "Inventory reservation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Releases reserved inventory items back to available stock.
     */
    public InventoryResult releaseItems(List<OrderItem> items) {
        return releaseItems(items, null);
    }

    /**
     * Releases reserved inventory items with correlation ID for tracing.
     */
    public InventoryResult releaseItems(List<OrderItem> items, String correlationId) {
        logger.info("Releasing inventory for {} items, correlationId: {}", items.size(), correlationId);

        try {
            // Get product IDs for locking
            List<String> productIds = items.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());

            // Fetch inventory items with pessimistic locking
            List<Inventory> inventoryItems = inventoryRepository.findByProductIdInWithLock(productIds);

            // Release all items back to inventory
            List<Inventory> updatedInventory = new ArrayList<>();
            for (OrderItem item : items) {
                Optional<Inventory> inventoryOpt = inventoryItems.stream()
                    .filter(inv -> inv.getProductId().equals(item.getProductId()))
                    .findFirst();

                if (inventoryOpt.isPresent()) {
                    Inventory inventory = inventoryOpt.get();
                    inventory.release(item.getQuantity());
                    updatedInventory.add(inventory);
                    
                    logger.debug("Released {} units of product: {}, new available: {}", 
                               item.getQuantity(), item.getProductId(), inventory.getAvailableQuantity());
                } else {
                    logger.warn("Product not found for release: {}", item.getProductId());
                }
            }

            // Save all updated inventory items
            inventoryRepository.saveAll(updatedInventory);

            logger.info("Successfully released inventory for {} items, correlationId: {}", 
                       items.size(), correlationId);
            
            return InventoryResult.released("multiple", items.size());

        } catch (Exception e) {
            logger.error("Error releasing inventory, correlationId: {}", correlationId, e);
            throw new InventoryReservationException("multiple", "Inventory release failed: " + e.getMessage(), e);
        }
    }

    /**
     * Confirms reservation by removing items from reserved stock (items are sold).
     */
    public InventoryResult confirmReservation(List<OrderItem> items, String correlationId) {
        logger.info("Confirming reservation for {} items, correlationId: {}", items.size(), correlationId);

        try {
            // Get product IDs for locking
            List<String> productIds = items.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());

            // Fetch inventory items with pessimistic locking
            List<Inventory> inventoryItems = inventoryRepository.findByProductIdInWithLock(productIds);

            // Confirm reservation for all items
            List<Inventory> updatedInventory = new ArrayList<>();
            for (OrderItem item : items) {
                Optional<Inventory> inventoryOpt = inventoryItems.stream()
                    .filter(inv -> inv.getProductId().equals(item.getProductId()))
                    .findFirst();

                if (inventoryOpt.isPresent()) {
                    Inventory inventory = inventoryOpt.get();
                    inventory.confirmReservation(item.getQuantity());
                    updatedInventory.add(inventory);
                    
                    logger.debug("Confirmed reservation for {} units of product: {}, remaining reserved: {}", 
                               item.getQuantity(), item.getProductId(), inventory.getReservedQuantity());
                } else {
                    logger.warn("Product not found for confirmation: {}", item.getProductId());
                }
            }

            // Save all updated inventory items
            inventoryRepository.saveAll(updatedInventory);

            logger.info("Successfully confirmed reservation for {} items, correlationId: {}", 
                       items.size(), correlationId);
            
            return InventoryResult.success("multiple", items.size());

        } catch (Exception e) {
            logger.error("Error confirming reservation, correlationId: {}", correlationId, e);
            throw new InventoryReservationException("multiple", "Reservation confirmation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets available quantity for a specific product.
     */
    public Integer getAvailableQuantity(String productId) {
        return inventoryRepository.getAvailableQuantity(productId).orElse(0);
    }

    /**
     * Gets inventory information for a specific product.
     */
    public Optional<Inventory> getInventory(String productId) {
        return inventoryRepository.findById(productId);
    }

    /**
     * Checks if sufficient quantity is available for a product.
     */
    public boolean hasSufficientQuantity(String productId, Integer requiredQuantity) {
        Boolean result = inventoryRepository.hasSufficientQuantity(productId, requiredQuantity);
        return result != null && result;
    }

    /**
     * Gets all products with low stock levels.
     */
    public List<Inventory> getLowStockProducts() {
        return inventoryRepository.findLowStockProducts();
    }

    /**
     * Gets all products that are out of stock.
     */
    public List<Inventory> getOutOfStockProducts() {
        return inventoryRepository.findOutOfStockProducts();
    }

    /**
     * Adds new product to inventory or updates existing product stock.
     */
    public Inventory addOrUpdateProduct(String productId, String productName, Integer quantity) {
        Optional<Inventory> existingInventory = inventoryRepository.findById(productId);
        
        if (existingInventory.isPresent()) {
            Inventory inventory = existingInventory.get();
            inventory.addStock(quantity);
            return inventoryRepository.save(inventory);
        } else {
            Inventory newInventory = new Inventory(productId, productName, quantity);
            return inventoryRepository.save(newInventory);
        }
    }

    // Private helper methods

    private void publishInventoryReservedEvent(List<OrderItem> items, String reservationId, String correlationId) {
        InventoryReservedEvent event = new InventoryReservedEvent(
            null, // orderId - would need to be passed
            null, // customerId - would need to be passed
            items,
            reservationId,
            correlationId,
            null
        );

        logger.debug("Inventory reserved event: {}", event);
        // In a real implementation, this could be published to an event bus
        // For now, it's just logged for internal tracking
    }
}