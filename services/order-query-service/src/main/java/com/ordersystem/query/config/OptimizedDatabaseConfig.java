package com.ordersystem.query.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Optimized Database Configuration for High Performance
 * Focuses on connection pooling and query optimization
 */
@Configuration
public class OptimizedDatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedDatabaseConfig.class);

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username:}")
    private String databaseUsername;

    @Value("${spring.datasource.password:}")
    private String databasePassword;

    @Value("${app.database.pool.minimum-idle:5}")
    private int minimumIdle;

    @Value("${app.database.pool.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${app.database.pool.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${app.database.pool.idle-timeout:600000}")
    private int idleTimeout;

    @Value("${app.database.pool.max-lifetime:1800000}")
    private int maxLifetime;

    @Bean
    @Primary
    public DataSource optimizedDataSource() {
        logger.info("🔧 Configuring optimized HikariCP connection pool");

        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(databaseUrl);
        if (databaseUsername != null && !databaseUsername.isEmpty()) {
            config.setUsername(databaseUsername);
        }
        if (databasePassword != null && !databasePassword.isEmpty()) {
            config.setPassword(databasePassword);
        }

        // Performance optimizations for PostgreSQL
        config.setDriverClassName("org.postgresql.Driver");
        
        // Pool size optimization for container environment
        config.setMinimumIdle(minimumIdle);
        config.setMaximumPoolSize(maximumPoolSize);
        
        // Connection timing optimizations
        config.setConnectionTimeout(connectionTimeout); // 30 seconds
        config.setIdleTimeout(idleTimeout); // 10 minutes
        config.setMaxLifetime(maxLifetime); // 30 minutes
        
        // Validation and reliability
        config.setValidationTimeout(5000); // 5 seconds
        config.setConnectionTestQuery("SELECT 1");
        config.setInitializationFailTimeout(30000); // 30 seconds
        
        // Pool name for monitoring
        config.setPoolName("OrderQueryPool");
        
        // Performance-oriented connection properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // PostgreSQL-specific optimizations
        config.addDataSourceProperty("defaultRowFetchSize", "100");
        config.addDataSourceProperty("logUnclosedConnections", "true");
        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("loginTimeout", "10");
        
        // Memory and performance
        config.addDataSourceProperty("stringtype", "unspecified");
        config.addDataSourceProperty("prepareThreshold", "5");
        config.addDataSourceProperty("binaryTransfer", "true");
        
        HikariDataSource dataSource = new HikariDataSource(config);
        
        logger.info("✅ HikariCP connection pool configured: minIdle={}, maxPool={}, connectionTimeout={}ms", 
                minimumIdle, maximumPoolSize, connectionTimeout);
        
        return dataSource;
    }

    /**
     * Connection pool health monitoring
     */
    @Bean
    public ConnectionPoolHealthService connectionPoolHealthService() {
        return new ConnectionPoolHealthService();
    }

    public static class ConnectionPoolHealthService {
        private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolHealthService.class);

        public boolean isPoolHealthy(HikariDataSource dataSource) {
            try {
                if (dataSource == null || dataSource.isClosed()) {
                    return false;
                }

                // Check pool metrics
                int activeConnections = dataSource.getHikariPoolMXBean().getActiveConnections();
                int totalConnections = dataSource.getHikariPoolMXBean().getTotalConnections();
                int idleConnections = dataSource.getHikariPoolMXBean().getIdleConnections();

                // Pool is healthy if:
                // 1. Has connections available
                // 2. Not all connections are active (some idle available)
                // 3. No thread starvation
                boolean healthy = totalConnections > 0 && 
                                idleConnections >= 0 && 
                                activeConnections < totalConnections;

                if (!healthy) {
                    logger.warn("Connection pool unhealthy: active={}, total={}, idle={}", 
                            activeConnections, totalConnections, idleConnections);
                }

                return healthy;

            } catch (Exception e) {
                logger.error("Error checking connection pool health", e);
                return false;
            }
        }

        public ConnectionPoolMetrics getPoolMetrics(HikariDataSource dataSource) {
            try {
                if (dataSource == null || dataSource.isClosed()) {
                    return new ConnectionPoolMetrics(false, 0, 0, 0, 0);
                }

                int active = dataSource.getHikariPoolMXBean().getActiveConnections();
                int total = dataSource.getHikariPoolMXBean().getTotalConnections();
                int idle = dataSource.getHikariPoolMXBean().getIdleConnections();
                int threadsAwaiting = dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();

                return new ConnectionPoolMetrics(true, active, total, idle, threadsAwaiting);

            } catch (Exception e) {
                logger.error("Error getting pool metrics", e);
                return new ConnectionPoolMetrics(false, 0, 0, 0, 0);
            }
        }
    }

    public static class ConnectionPoolMetrics {
        private final boolean healthy;
        private final int activeConnections;
        private final int totalConnections;
        private final int idleConnections;
        private final int threadsAwaiting;

        public ConnectionPoolMetrics(boolean healthy, int active, int total, int idle, int waiting) {
            this.healthy = healthy;
            this.activeConnections = active;
            this.totalConnections = total;
            this.idleConnections = idle;
            this.threadsAwaiting = waiting;
        }

        // Getters
        public boolean isHealthy() { return healthy; }
        public int getActiveConnections() { return activeConnections; }
        public int getTotalConnections() { return totalConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getThreadsAwaiting() { return threadsAwaiting; }
        
        public double getUtilizationPercent() {
            return totalConnections > 0 ? (double) activeConnections / totalConnections * 100 : 0;
        }
    }
}