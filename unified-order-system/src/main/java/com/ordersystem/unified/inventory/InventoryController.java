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

    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
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
        
        return ResponseEntity.ok(products);
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