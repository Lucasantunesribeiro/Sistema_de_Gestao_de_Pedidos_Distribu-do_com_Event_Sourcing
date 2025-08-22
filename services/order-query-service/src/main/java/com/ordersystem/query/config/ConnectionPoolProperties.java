package com.ordersystem.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration Properties para monitoramento do Connection Pool - Query Service
 * Otimizado para read-heavy workload
 */
@Component
@ConfigurationProperties(prefix = "app.database")
public class ConnectionPoolProperties {

    private int maximumPoolSize = 25;
    private int minimumIdle = 12;
    private String connectionTimeout = "10s";
    private String idleTimeout = "10m";
    private String maxLifetime = "30m";
    private String leakDetectionThreshold = "60s";
    private String queryTimeout = "30s";
    private double readOnlyRatio = 0.8;
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

    public String getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(String queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public double getReadOnlyRatio() {
        return readOnlyRatio;
    }

    public void setReadOnlyRatio(double readOnlyRatio) {
        this.readOnlyRatio = readOnlyRatio;
    }

    public String getConnectionValidationInterval() {
        return connectionValidationInterval;
    }

    public void setConnectionValidationInterval(String connectionValidationInterval) {
        this.connectionValidationInterval = connectionValidationInterval;
    }
}