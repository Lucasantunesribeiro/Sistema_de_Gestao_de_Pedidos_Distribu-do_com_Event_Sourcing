package com.ordersystem.unified.inventory;

import com.ordersystem.unified.inventory.model.Inventory;
import com.ordersystem.unified.inventory.repository.InventoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Inventory operations in the unified order system.
 */
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory management operations")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    public InventoryController() {
        logger.info("InventoryController initialized");
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    @GetMapping
    @Operation(summary = "Get all inventory items", description = "Retrieves all inventory items in the system")
    public ResponseEntity<List<Inventory>> getAllInventoryItems() {
        logger.debug("Getting all inventory items");
        try {
            List<Inventory> items = inventoryRepository.findAll();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            logger.error("Error getting inventory items: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory by product ID", description = "Retrieves inventory information for a specific product")
    public ResponseEntity<Inventory> getInventoryItem(@PathVariable String productId) {
        logger.debug("Getting inventory for product: {}", productId);
        Optional<Inventory> item = inventoryRepository.findById(productId);
        return item.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    @Operation(summary = "Get available inventory", description = "Retrieves all inventory items that are currently available")
    public ResponseEntity<List<Inventory>> getAvailableInventory() {
        logger.debug("Getting available inventory items");
        try {
            List<Inventory> availableItems = inventoryRepository.findByAvailableQuantityGreaterThan(0);
            return ResponseEntity.ok(availableItems);
        } catch (Exception e) {
            logger.error("Error getting available inventory: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Inventory service status", description = "Returns the status of the inventory service")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "inventory");
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }
}