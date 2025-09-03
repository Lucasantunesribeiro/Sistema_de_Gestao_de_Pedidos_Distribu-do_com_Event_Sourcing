package com.ordersystem.unified.inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * REST controller for inventory operations.
 * Spring Boot compatible version with proper annotations.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getInventory() {
        List<Map<String, Object>> items = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", "PROD-" + i);
            item.put("productName", "Product " + i);
            item.put("availableQuantity", 40 + i);
            item.put("reservedQuantity", 5);
            item.put("totalQuantity", 45 + i);
            item.put("lastUpdated", System.currentTimeMillis());
            items.add(item);
        }

        return ResponseEntity.ok(items);
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Object>> checkStock(@PathVariable String productId, 
                                                         @RequestParam int quantity) {
        Map<String, Object> result = inventoryService.checkAvailability(productId, quantity);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveStock(@RequestParam String orderId, 
                                                           @RequestBody Map<String, Object> items) {
        Map<String, Object> result = inventoryService.reserveItems(orderId, items);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/release/{reservationId}")
    public ResponseEntity<Map<String, Object>> releaseStock(@PathVariable String reservationId) {
        Map<String, Object> result = inventoryService.releaseReservation(reservationId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getInventoryStatus() {
        Map<String, Object> status = inventoryService.getInventoryStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "inventory-service");
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}