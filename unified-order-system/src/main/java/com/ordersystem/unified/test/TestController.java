package com.ordersystem.unified.test;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple test controller to verify Spring Boot is working correctly.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Test endpoint is working");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments")
    public ResponseEntity<Map<String, Object>> testPayments() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "payments");
        response.put("status", "OK");
        response.put("message", "Payment service test endpoint");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> testInventory() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "inventory");
        response.put("status", "OK");
        response.put("message", "Inventory service test endpoint");
        return ResponseEntity.ok(response);
    }
}