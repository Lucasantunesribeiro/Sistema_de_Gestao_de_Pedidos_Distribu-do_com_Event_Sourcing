package com.ordersystem.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom health indicator for database connectivity
 * Provides detailed health information about database connection status
 */
@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    private static final String HEALTH_CHECK_QUERY = "SELECT 1 as health_check, current_timestamp as check_time";

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now())
                    .withDetail("status", "Database connection failed")
                    .build();
        }
    }

    @Retryable(value = {SQLException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    private Health checkDatabaseHealth() throws SQLException {
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(HEALTH_CHECK_QUERY);
             ResultSet resultSet = statement.executeQuery()) {
            
            Map<String, Object> details = new HashMap<>();
            
            if (resultSet.next()) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                details.put("status", "UP");
                details.put("database", "PostgreSQL");
                details.put("responseTime", responseTime + "ms");
                details.put("timestamp", Instant.now());
                details.put("connection", "Active");
                details.put("autoCommit", connection.getAutoCommit());
                details.put("readOnly", connection.isReadOnly());
                details.put("transactionIsolation", getTransactionIsolationName(connection.getTransactionIsolation()));
                
                // Add connection pool information if available
                if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                    com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) dataSource;
                    com.zaxxer.hikari.HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();
                    
                    if (poolBean != null) {
                        details.put("pool.active", poolBean.getActiveConnections());
                        details.put("pool.idle", poolBean.getIdleConnections());
                        details.put("pool.total", poolBean.getTotalConnections());
                        details.put("pool.threadsAwaitingConnection", poolBean.getThreadsAwaitingConnection());
                    }
                }
                
                logger.debug("Database health check successful in {}ms", responseTime);
                
                return Health.up().withDetails(details).build();
            } else {
                return Health.down()
                        .withDetail("error", "Health check query returned no results")
                        .withDetail("timestamp", Instant.now())
                        .build();
            }
            
        } catch (SQLException e) {
            logger.warn("Database health check failed, will retry: {}", e.getMessage());
            throw e;
        }
    }

    private String getTransactionIsolationName(int level) {
        switch (level) {
            case Connection.TRANSACTION_NONE:
                return "NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return "READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED:
                return "READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ:
                return "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE:
                return "SERIALIZABLE";
            default:
                return "UNKNOWN(" + level + ")";
        }
    }
}