package com.ordersystem.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive application health indicator for Inventory Service
 */
@Component("application")
public class ApplicationHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationHealthIndicator.class);

    @Autowired
    private StartupHealthCheck startupHealthCheck;

    @Autowired
    private RabbitMQHealthIndicator rabbitMQHealthIndicator;

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // Service information
            details.put("service", "Inventory Service");
            details.put("version", "1.0.0");
            details.put("timestamp", Instant.now());
            
            // Startup information
            if (startupHealthCheck.isReady()) {
                details.put("startup", "READY");
                if (startupHealthCheck.getStartupTime() != null) {
                    Duration uptime = Duration.between(startupHealthCheck.getStartupTime(), Instant.now());
                    details.put("uptime", formatDuration(uptime));
                    details.put("startupTime", startupHealthCheck.getStartupTime());
                }
            } else {
                details.put("startup", "NOT_READY");
            }
            
            // Check RabbitMQ
            Health rabbitHealth = rabbitMQHealthIndicator.health();
            details.put("rabbitmq", rabbitHealth.getStatus().getCode());
            
            // Determine overall health
            boolean isHealthy = startupHealthCheck.isReady() && 
                               rabbitHealth.getStatus().getCode().equals("UP");
            
            if (isHealthy) {
                details.put("status", "HEALTHY");
                details.put("message", "Inventory Service is fully operational");
                logger.debug("Inventory Service health check: HEALTHY");
                return Health.up().withDetails(details).build();
            } else {
                details.put("status", "UNHEALTHY");
                details.put("message", "One or more dependencies are not available");
                logger.warn("Inventory Service health check: UNHEALTHY - RabbitMQ: {}, Startup: {}", 
                    rabbitHealth.getStatus().getCode(),
                    startupHealthCheck.isReady() ? "READY" : "NOT_READY");
                return Health.down().withDetails(details).build();
            }
            
        } catch (Exception e) {
            logger.error("Inventory Service application health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now())
                    .withDetail("service", "Inventory Service")
                    .build();
        }
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}