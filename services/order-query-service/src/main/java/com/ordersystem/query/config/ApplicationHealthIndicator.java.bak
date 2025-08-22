package com.ordersystem.query.config;

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
 * Comprehensive application health indicator for Query Service
 */
@Component("application")
public class ApplicationHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationHealthIndicator.class);

    @Autowired
    private StartupHealthCheck startupHealthCheck;

    @Autowired
    private DatabaseHealthIndicator databaseHealthIndicator;

    @Autowired
    private RabbitMQHealthIndicator rabbitMQHealthIndicator;

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // Service information
            details.put("service", "Query Service");
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
            
            // Check individual components
            Health dbHealth = databaseHealthIndicator.health();
            Health rabbitHealth = rabbitMQHealthIndicator.health();
            
            details.put("database", dbHealth.getStatus().getCode());
            details.put("rabbitmq", rabbitHealth.getStatus().getCode());
            
            // Determine overall health
            boolean isHealthy = startupHealthCheck.isReady() && 
                               dbHealth.getStatus().getCode().equals("UP") &&
                               rabbitHealth.getStatus().getCode().equals("UP");
            
            if (isHealthy) {
                details.put("status", "HEALTHY");
                details.put("message", "Query Service is fully operational");
                logger.debug("Query Service health check: HEALTHY");
                return Health.up().withDetails(details).build();
            } else {
                details.put("status", "UNHEALTHY");
                details.put("message", "One or more dependencies are not available");
                logger.warn("Query Service health check: UNHEALTHY - Database: {}, RabbitMQ: {}, Startup: {}", 
                    dbHealth.getStatus().getCode(), 
                    rabbitHealth.getStatus().getCode(),
                    startupHealthCheck.isReady() ? "READY" : "NOT_READY");
                return Health.down().withDetails(details).build();
            }
            
        } catch (Exception e) {
            logger.error("Query Service application health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now())
                    .withDetail("service", "Query Service")
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