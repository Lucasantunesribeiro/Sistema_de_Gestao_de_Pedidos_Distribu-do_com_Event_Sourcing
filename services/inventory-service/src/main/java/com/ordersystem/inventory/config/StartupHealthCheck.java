package com.ordersystem.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Startup coordination and health verification for Inventory Service
 */
@Component
public class StartupHealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(StartupHealthCheck.class);

    @Autowired
    private org.springframework.amqp.rabbit.connection.ConnectionFactory rabbitConnectionFactory;

    private boolean isReady = false;
    private Instant startupTime;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Inventory Service application ready event received, starting dependency verification...");
        startupTime = Instant.now();
        
        try {
            verifyAllDependencies();
            isReady = true;
            
            long startupDuration = java.time.Duration.between(startupTime, Instant.now()).toMillis();
            logger.info("Inventory Service startup completed successfully in {}ms", startupDuration);
            logger.info("Inventory Service is ready to manage stock");
            
            logConfigurationSummary();
            
        } catch (Exception e) {
            logger.error("Inventory Service startup failed during dependency verification", e);
            isReady = false;
            throw new RuntimeException("Inventory Service startup failed", e);
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    private void verifyAllDependencies() throws Exception {
        logger.info("Verifying Inventory Service dependencies...");
        
        // Verify RabbitMQ connectivity
        verifyRabbitMQConnection();
        
        logger.info("All Inventory Service dependencies verified successfully");
    }

    private void verifyRabbitMQConnection() throws Exception {
        logger.info("Verifying RabbitMQ connection...");
        
        try (com.rabbitmq.client.Connection connection = rabbitConnectionFactory.createConnection().getDelegate()) {
            if (connection.isOpen()) {
                logger.info("RabbitMQ connection verified successfully");
                logger.debug("RabbitMQ Host: {}:{}", connection.getAddress(), connection.getPort());
                logger.debug("RabbitMQ Server Version: {}", connection.getServerProperties().get("version"));
            } else {
                throw new Exception("RabbitMQ connection is not open");
            }
        }
    }

    private void logConfigurationSummary() {
        logger.info("=== Inventory Service Configuration Summary ===");
        logger.info("Service: Inventory Service");
        logger.info("Port: 8083");
        logger.info("Profile: {}", System.getProperty("spring.profiles.active", "default"));
        logger.info("Startup Time: {}", startupTime);
        logger.info("Ready: {}", isReady);
        logger.info("==============================================");
    }

    public boolean isReady() {
        return isReady;
    }

    public Instant getStartupTime() {
        return startupTime;
    }
}