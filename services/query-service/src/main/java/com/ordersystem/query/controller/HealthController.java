package com.ordersystem.query.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class HealthController implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    @Autowired
    private DataSource dataSource;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Health check requested for Query Service");
        
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("service", "query-service");
        healthStatus.put("status", "UP");
        healthStatus.put("timestamp", System.currentTimeMillis());
        
        // Check database connectivity
        try (Connection connection = dataSource.getConnection()) {
            healthStatus.put("database", "UP");
            logger.debug("Database connection successful");
        } catch (Exception e) {
            healthStatus.put("database", "DOWN");
            healthStatus.put("status", "DOWN");
            healthStatus.put("error", e.getMessage());
            logger.error("Database connection failed", e);
            return ResponseEntity.status(503).body(healthStatus);
        }
        
        logger.debug("Query Service health check completed successfully");
        return ResponseEntity.ok(healthStatus);
    }
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            return Health.up()
                    .withDetail("service", "query-service")
                    .withDetail("database", "UP")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "query-service")
                    .withDetail("database", "DOWN")
                    .withException(e)
                    .build();
        }
    }
}