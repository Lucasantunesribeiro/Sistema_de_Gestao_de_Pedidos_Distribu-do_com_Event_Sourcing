package com.ordersystem.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

@RestController
@CrossOrigin(origins = "*")
public class ProxyController {

    private final RestTemplate restTemplate = new RestTemplate();
    
    // Internal service URLs
    private static final String PAYMENT_SERVICE_URL = "http://localhost:8082";
    private static final String INVENTORY_SERVICE_URL = "http://localhost:8083";
    private static final String QUERY_SERVICE_URL = "http://localhost:8084";

    @GetMapping("/api/payments")
    public ResponseEntity<?> getPayments() {
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                PAYMENT_SERVICE_URL + "/api/payments", Object.class);
            return response;
        } catch (Exception e) {
            return ResponseEntity.ok(createMockPayments());
        }
    }

    @GetMapping("/api/inventory")
    public ResponseEntity<?> getInventory() {
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                INVENTORY_SERVICE_URL + "/api/inventory", Object.class);
            return response;
        } catch (Exception e) {
            return ResponseEntity.ok(createMockInventory());
        }
    }

    @GetMapping("/api/dashboard/metrics")
    public ResponseEntity<?> getDashboardMetrics() {
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                QUERY_SERVICE_URL + "/api/orders/dashboard/metrics", Object.class);
            return response;
        } catch (Exception e) {
            return ResponseEntity.ok(createMockDashboardMetrics());
        }
    }

    // Mock data methods for fallback
    private List<Map<String, Object>> createMockPayments() {
        Map<String, Object> payment1 = new HashMap<>();
        payment1.put("id", "payment-001");
        payment1.put("orderId", "ORDER-DEMO-001");
        payment1.put("amount", 1299.99);
        payment1.put("status", "COMPLETED");
        payment1.put("method", "CREDIT_CARD");
        payment1.put("processedAt", "2024-01-15T10:30:00Z");
        payment1.put("transactionId", "txn_demo_001");
        
        Map<String, Object> payment2 = new HashMap<>();
        payment2.put("id", "payment-002");
        payment2.put("orderId", "ORDER-DEMO-002");
        payment2.put("amount", 599.98);
        payment2.put("status", "PENDING");
        payment2.put("method", "PIX");
        payment2.put("processedAt", "2024-01-15T11:45:00Z");
        payment2.put("transactionId", "txn_demo_002");

        Map<String, Object> payment3 = new HashMap<>();
        payment3.put("id", "payment-003");
        payment3.put("orderId", "ORDER-DEMO-003");
        payment3.put("amount", 79.99);
        payment3.put("status", "PROCESSING");
        payment3.put("method", "DEBIT_CARD");
        payment3.put("processedAt", "2024-01-15T12:30:00Z");
        payment3.put("transactionId", "txn_demo_003");

        return Arrays.asList(payment1, payment2, payment3);
    }

    private List<Map<String, Object>> createMockInventory() {
        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", "laptop-001");
        item1.put("name", "Notebook Dell Inspiron 15");
        item1.put("quantity", 25);
        item1.put("price", 1299.99);
        item1.put("category", "Electronics");
        
        Map<String, Object> item2 = new HashMap<>();
        item2.put("id", "mouse-001");
        item2.put("name", "Mouse Gamer Logitech G502");
        item2.put("quantity", 50);
        item2.put("price", 299.99);
        item2.put("category", "Accessories");

        Map<String, Object> item3 = new HashMap<>();
        item3.put("id", "keyboard-001");
        item3.put("name", "Teclado Mecânico RGB");
        item3.put("quantity", 15);
        item3.put("price", 399.99);
        item3.put("category", "Accessories");

        Map<String, Object> item4 = new HashMap<>();
        item4.put("id", "monitor-001");
        item4.put("name", "Monitor 4K Dell UltraSharp");
        item4.put("quantity", 8);
        item4.put("price", 2499.99);
        item4.put("category", "Electronics");

        return Arrays.asList(item1, item2, item3, item4);
    }

    private Map<String, Object> createMockDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalOrders", 156);
        metrics.put("totalRevenue", 45230.50);
        metrics.put("completedOrders", 142);
        metrics.put("pendingOrders", 14);
        metrics.put("averageOrderValue", 290.32);
        
        return metrics;
    }
}