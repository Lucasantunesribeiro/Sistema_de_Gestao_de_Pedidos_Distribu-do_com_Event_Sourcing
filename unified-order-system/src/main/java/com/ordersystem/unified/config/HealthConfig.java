package com.ordersystem.unified.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the unified order system.
 */
@Component("healthConfig")
public class HealthConfig implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
            .withDetail("service", "unified-order-system")
            .withDetail("status", "operational")
            .withDetail("version", "1.0.0")
            .build();
    }
}