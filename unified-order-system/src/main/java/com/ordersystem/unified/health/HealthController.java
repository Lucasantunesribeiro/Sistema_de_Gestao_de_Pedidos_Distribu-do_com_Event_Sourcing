package com.ordersystem.unified.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for the unified order system.
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "System health check operations")
public class HealthController {

    public HealthController() {
        System.out.println("HealthController initialized");
    }

    @Autowired
    private DataSource dataSource;

    @GetMapping
    @Operation(summary = "Health check", description = "Returns the health status of the application")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> healthStatus = new HashMap<>();
        
        try {
            // Check database connectivity
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                healthStatus.put("status", isValid ? "UP" : "DOWN");
                healthStatus.put("database", isValid ? "UP" : "DOWN");
            }
            
            healthStatus.put("service", "unified-order-system");
            healthStatus.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            healthStatus.put("status", "DOWN");
            healthStatus.put("database", "DOWN");
            healthStatus.put("error", e.getMessage());
            healthStatus.put("service", "unified-order-system");
            healthStatus.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(healthStatus);
        }
    }
}