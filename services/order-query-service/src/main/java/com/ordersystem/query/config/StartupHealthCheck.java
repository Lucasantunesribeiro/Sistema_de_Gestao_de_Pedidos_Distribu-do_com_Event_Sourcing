package com.ordersystem.query.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Startup coordination and health verification for Query Service
 */
@Component
public class StartupHealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(StartupHealthCheck.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private org.springframework.amqp.rabbit.connection.ConnectionFactory rabbitConnectionFactory;

    @Autowired
    private ResilienceConfig resilienceConfig;

    private boolean isReady = false;
    private Instant startupTime;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Query Service application ready event received, starting dependency verification...");
        startupTime = Instant.now();

        try {
            verifyAllDependencies();
            isReady = true;

            long startupDuration = java.time.Duration.between(startupTime, Instant.now()).toMillis();
            logger.info("Query Service startup completed successfully in {}ms", startupDuration);
            logger.info("Query Service is ready to serve queries");

            logConfigurationSummary();

        } catch (Exception e) {
            logger.error("Query Service startup failed during dependency verification", e);
            isReady = false;
            throw new RuntimeException("Query Service startup failed", e);
        }
    }

    @Retryable(value = { Exception.class }, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    private void verifyAllDependencies() throws Exception {
        logger.info("Verifying Query Service dependencies...");

        // Verify database connectivity
        verifyDatabaseConnection();

        // TODO: Re-enable RabbitMQ verification when RabbitMQ is available in
        // production
        // verifyRabbitMQConnection();
        logger.info("⚠️ RabbitMQ verification temporarily disabled for H2 validation phase");

        logger.info("All Query Service dependencies verified successfully");
    }

    private void verifyDatabaseConnection() throws SQLException {
        logger.info("Verifying query database connection...");

        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                logger.info("Query database connection verified successfully");
                logger.debug("Database URL: {}", connection.getMetaData().getURL());
                logger.debug("Database Product: {} {}",
                        connection.getMetaData().getDatabaseProductName(),
                        connection.getMetaData().getDatabaseProductVersion());
            } else {
                throw new SQLException("Query database connection is not valid");
            }
        }
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
        logger.info("=== Query Service Configuration Summary ===");
        logger.info("Service: Query Service (CQRS Read Side)");
        logger.info("Port: 8084");
        logger.info("Profile: {}", System.getProperty("spring.profiles.active", "default"));
        logger.info("Max Retries: {}", resilienceConfig.getMaxRetries());
        logger.info("Initial Delay: {}", resilienceConfig.getInitialDelay());
        logger.info("Max Delay: {}", resilienceConfig.getMaxDelay());
        logger.info("Query Timeout: {}", resilienceConfig.getQueryTimeout());
        logger.info("Startup Time: {}", startupTime);
        logger.info("Ready: {}", isReady);
        logger.info("==========================================");
    }

    public boolean isReady() {
        return isReady;
    }

    public Instant getStartupTime() {
        return startupTime;
    }
}