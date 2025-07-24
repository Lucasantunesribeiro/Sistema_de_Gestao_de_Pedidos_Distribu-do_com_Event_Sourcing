package com.ordersystem.inventory.controller;

import com.ordersystem.inventory.model.InventoryItem;
import com.ordersystem.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryItem> getInventoryItem(@PathVariable String productId) {
        InventoryItem item = inventoryService.getInventoryItem(productId);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, InventoryItem>> getAllInventory() {
        return ResponseEntity.ok(InventoryItem.getInventory());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Inventory Service is healthy");
    }
}