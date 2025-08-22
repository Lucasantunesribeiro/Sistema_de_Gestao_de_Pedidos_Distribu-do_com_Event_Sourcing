package com.ordersystem.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration Properties para monitoramento do Connection Pool
 * Permite configuração dinâmica via application.yml
 */
@Component
@ConfigurationProperties(prefix = "app.database")
public class ConnectionPoolProperties {

    private int maximumPoolSize = 25;
    private int minimumIdle = 10;
    private String connectionTimeout = "10s";
    private String idleTimeout = "5m";
    private String maxLifetime = "20m";
    private String leakDetectionThreshold = "60s";
    private int poolUsageThreshold = 70;
    private int connectionAcquireIncrement = 2;
    private String connectionValidationInterval = "30s";

    // Getters and Setters
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public void setMinimumIdle(int minimumIdle) {
        this.minimumIdle = minimumIdle;
    }

    public String getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(String idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public String getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(String maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public String getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    public void setLeakDetectionThreshold(String leakDetectionThreshold) {
        this.leakDetectionThreshold = leakDetectionThreshold;
    }

    public int getPoolUsageThreshold() {
        return poolUsageThreshold;
    }

    public void setPoolUsageThreshold(int poolUsageThreshold) {
        this.poolUsageThreshold = poolUsageThreshold;
    }

    public int getConnectionAcquireIncrement() {
        return connectionAcquireIncrement;
    }

    public void setConnectionAcquireIncrement(int connectionAcquireIncrement) {
        this.connectionAcquireIncrement = connectionAcquireIncrement;
    }

    public String getConnectionValidationInterval() {
        return connectionValidationInterval;
    }

    public void setConnectionValidationInterval(String connectionValidationInterval) {
        this.connectionValidationInterval = connectionValidationInterval;
    }
}