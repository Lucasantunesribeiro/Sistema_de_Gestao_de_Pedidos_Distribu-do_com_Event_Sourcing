package com.ordersystem.inventory.controller;

import com.ordersystem.inventory.model.InventoryItem;
import com.ordersystem.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
            "message", "Inventory Service is running!",
            "service", "Inventory Service",
            "version", "1.0.0",
            "status", "UP"
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "service", "Inventory Service"
        );
    }

    @GetMapping("/items")
    public List<InventoryItem> getAllItems() {
        return inventoryService.getAllItems();
    }

    @GetMapping("/items/{productId}")
    public ResponseEntity<InventoryItem> getItem(@PathVariable String productId) {
        InventoryItem item = inventoryService.getItem(productId);
        return item != null ? ResponseEntity.ok(item) : ResponseEntity.notFound().build();
    }

    @PostMapping("/items")
    public InventoryItem createItem(@RequestBody InventoryItem item) {
        return inventoryService.createItem(item);
    }

    @PutMapping("/items/{productId}/stock")
    public ResponseEntity<InventoryItem> updateStock(
            @PathVariable String productId, 
            @RequestParam int quantity) {
        InventoryItem updated = inventoryService.updateStock(productId, quantity);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PostMapping("/items/{productId}/reserve")
    public ResponseEntity<Map<String, Object>> reserveStock(
            @PathVariable String productId, 
            @RequestParam int quantity,
            @RequestParam String orderId) {
        boolean reserved = inventoryService.reserveStock(productId, quantity, orderId);
        return ResponseEntity.ok(Map.of(
            "reserved", reserved,
            "productId", productId,
            "quantity", quantity,
            "orderId", orderId
        ));
    }

    @PostMapping("/items/{productId}/release")
    public ResponseEntity<Map<String, Object>> releaseStock(
            @PathVariable String productId, 
            @RequestParam int quantity,
            @RequestParam String orderId) {
        inventoryService.releaseStock(productId, quantity, orderId);
        return ResponseEntity.ok(Map.of(
            "released", true,
            "productId", productId,
            "quantity", quantity,
            "orderId", orderId
        ));
    }
}