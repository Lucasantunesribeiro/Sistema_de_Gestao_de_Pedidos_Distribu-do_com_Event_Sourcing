package com.ordersystem.order.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

@RestController
public class ApiGatewayController {

    private final RestTemplate restTemplate = new RestTemplate();
    
    // Internal service URLs
    private static final String PAYMENT_SERVICE_URL = "http://localhost:8082";
    private static final String INVENTORY_SERVICE_URL = "http://localhost:8083";
    private static final String QUERY_SERVICE_URL = "http://localhost:8084";

    @GetMapping("/api/payments")
    public ResponseEntity<?> getPayments() {
        try {
            // Try to call payment service, fallback to mock data
            ResponseEntity<Object> response = restTemplate.getForEntity(
                PAYMENT_SERVICE_URL + "/api/payments", Object.class);
            return response;
        } catch (Exception e) {
            // Return mock data when service is not available
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
                QUERY_SERVICE_URL + "/api/dashboard/metrics", Object.class);
            return response;
        } catch (Exception e) {
            return ResponseEntity.ok(createMockDashboardMetrics());
        }
    }

    // Proxy payment-related requests
    @RequestMapping(value = "/api/payments/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyPayments(HttpServletRequest request, @RequestBody(required = false) Object body) {
        String path = request.getRequestURI().substring("/api/payments".length());
        String url = PAYMENT_SERVICE_URL + "/api/payments" + path;
        
        try {
            switch (request.getMethod()) {
                case "GET":
                    return restTemplate.getForEntity(url, Object.class);
                case "POST":
                    return restTemplate.postForEntity(url, body, Object.class);
                case "PUT":
                    restTemplate.put(url, body);
                    return ResponseEntity.ok().build();
                case "DELETE":
                    restTemplate.delete(url);
                    return ResponseEntity.ok().build();
                default:
                    return ResponseEntity.method NotAllowed().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(503).body("Service Unavailable");
        }
    }

    // Proxy inventory-related requests  
    @RequestMapping(value = "/api/inventory/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyInventory(HttpServletRequest request, @RequestBody(required = false) Object body) {
        String path = request.getRequestURI().substring("/api/inventory".length());
        String url = INVENTORY_SERVICE_URL + "/api/inventory" + path;
        
        try {
            switch (request.getMethod()) {
                case "GET":
                    return restTemplate.getForEntity(url, Object.class);
                case "POST":
                    return restTemplate.postForEntity(url, body, Object.class);
                case "PUT":
                    restTemplate.put(url, body);
                    return ResponseEntity.ok().build();
                case "DELETE":
                    restTemplate.delete(url);
                    return ResponseEntity.ok().build();
                default:
                    return ResponseEntity.status(405).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(503).body("Service Unavailable");
        }
    }

    // Mock data methods
    private List<Map<String, Object>> createMockPayments() {
        Map<String, Object> payment1 = new HashMap<>();
        payment1.put("id", "payment-001");
        payment1.put("orderId", "ORDER-DEMO-001");
        payment1.put("amount", 1299.99);
        payment1.put("status", "COMPLETED");
        payment1.put("method", "CREDIT_CARD");
        payment1.put("processedAt", "2024-01-15T10:30:00Z");
        
        Map<String, Object> payment2 = new HashMap<>();
        payment2.put("id", "payment-002");
        payment2.put("orderId", "ORDER-DEMO-002");
        payment2.put("amount", 599.98);
        payment2.put("status", "PENDING");
        payment2.put("method", "PIX");
        payment2.put("processedAt", "2024-01-15T11:45:00Z");

        return Arrays.asList(payment1, payment2);
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

        return Arrays.asList(item1, item2);
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