package com.ordersystem.query.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom health indicator for RabbitMQ connectivity in Query Service
 */
@Component("rabbitmq")
public class RabbitMQHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQHealthIndicator.class);

    @Autowired
    private ConnectionFactory connectionFactory;

    @Override
    public Health health() {
        try {
            return checkRabbitMQHealth();
        } catch (Exception e) {
            logger.error("Query Service RabbitMQ health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now())
                    .withDetail("status", "Query Service RabbitMQ connection failed")
                    .build();
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    private Health checkRabbitMQHealth() throws Exception {
        long startTime = System.currentTimeMillis();
        
        try (com.rabbitmq.client.Connection connection = connectionFactory.createConnection().getDelegate()) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("service", "Query Service");
            details.put("broker", "RabbitMQ");
            details.put("responseTime", responseTime + "ms");
            details.put("timestamp", Instant.now());
            details.put("connection", "Active");
            details.put("host", connection.getAddress().getHostAddress());
            details.put("port", connection.getPort());
            details.put("virtualHost", connection.getServerProperties().get("virtual_host"));
            details.put("serverVersion", connection.getServerProperties().get("version"));
            
            logger.debug("Query Service RabbitMQ health check successful in {}ms", responseTime);
            
            return Health.up().withDetails(details).build();
            
        } catch (Exception e) {
            logger.warn("Query Service RabbitMQ health check failed, will retry: {}", e.getMessage());
            throw e;
        }
    }
}