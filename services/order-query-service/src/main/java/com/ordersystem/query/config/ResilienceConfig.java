package com.ordersystem.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.time.Duration;

/**
 * Resilience configuration for database and external service connections
 * Optimized for query service read operations
 */
@Configuration
@EnableRetry
@ConfigurationProperties(prefix = "app.resilience")
public class ResilienceConfig {

    // Retry configuration
    private int maxRetries = 3;
    private Duration initialDelay = Duration.ofSeconds(1);
    private Duration maxDelay = Duration.ofSeconds(30);
    private double backoffMultiplier = 2.0;

    // Circuit breaker configuration
    private int failureThreshold = 5;
    private Duration circuitBreakerTimeout = Duration.ofMinutes(1);
    private int circuitBreakerResetTimeout = 60;

    // Connection timeout configuration
    private Duration databaseConnectionTimeout = Duration.ofSeconds(20);
    private Duration rabbitmqConnectionTimeout = Duration.ofSeconds(30);
    private Duration httpConnectionTimeout = Duration.ofSeconds(10);

    // Health check configuration
    private Duration healthCheckInterval = Duration.ofSeconds(30);
    private int healthCheckFailureThreshold = 3;

    // Query-specific configuration
    private Duration queryTimeout = Duration.ofSeconds(30);
    private int maxQueryRetries = 2;

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(Duration initialDelay) {
        this.initialDelay = initialDelay;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(Duration maxDelay) {
        this.maxDelay = maxDelay;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public Duration getCircuitBreakerTimeout() {
        return circuitBreakerTimeout;
    }

    public void setCircuitBreakerTimeout(Duration circuitBreakerTimeout) {
        this.circuitBreakerTimeout = circuitBreakerTimeout;
    }

    public int getCircuitBreakerResetTimeout() {
        return circuitBreakerResetTimeout;
    }

    public void setCircuitBreakerResetTimeout(int circuitBreakerResetTimeout) {
        this.circuitBreakerResetTimeout = circuitBreakerResetTimeout;
    }

    public Duration getDatabaseConnectionTimeout() {
        return databaseConnectionTimeout;
    }

    public void setDatabaseConnectionTimeout(Duration databaseConnectionTimeout) {
        this.databaseConnectionTimeout = databaseConnectionTimeout;
    }

    public Duration getRabbitmqConnectionTimeout() {
        return rabbitmqConnectionTimeout;
    }

    public void setRabbitmqConnectionTimeout(Duration rabbitmqConnectionTimeout) {
        this.rabbitmqConnectionTimeout = rabbitmqConnectionTimeout;
    }

    public Duration getHttpConnectionTimeout() {
        return httpConnectionTimeout;
    }

    public void setHttpConnectionTimeout(Duration httpConnectionTimeout) {
        this.httpConnectionTimeout = httpConnectionTimeout;
    }

    public Duration getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(Duration healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public int getHealthCheckFailureThreshold() {
        return healthCheckFailureThreshold;
    }

    public void setHealthCheckFailureThreshold(int healthCheckFailureThreshold) {
        this.healthCheckFailureThreshold = healthCheckFailureThreshold;
    }

    public Duration getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(Duration queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public int getMaxQueryRetries() {
        return maxQueryRetries;
    }

    public void setMaxQueryRetries(int maxQueryRetries) {
        this.maxQueryRetries = maxQueryRetries;
    }
}