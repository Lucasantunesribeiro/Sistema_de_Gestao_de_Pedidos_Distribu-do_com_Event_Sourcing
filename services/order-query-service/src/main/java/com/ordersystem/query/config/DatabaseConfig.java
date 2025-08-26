package com.ordersystem.query.config;

import java.time.Duration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Database configuration with HikariCP connection pooling and resilience
 * settings
 * Optimized for read-heavy workloads in the query service
 */
@Configuration
@EnableRetry
@ConfigurationProperties(prefix = "app.database")
@Profile("!local")
public class DatabaseConfig {

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5433/order_query_db}")
    private String databaseUrl;

    @Value("${spring.datasource.username:postgres}")
    private String username;

    @Value("${spring.datasource.password:password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    // Connection pool settings optimized for read operations
    private int maximumPoolSize = 15;
    private int minimumIdle = 8;
    private Duration connectionTimeout = Duration.ofSeconds(20);
    private Duration idleTimeout = Duration.ofMinutes(10);
    private Duration maxLifetime = Duration.ofMinutes(30);
    private Duration leakDetectionThreshold = Duration.ofSeconds(60);
    private Duration validationTimeout = Duration.ofSeconds(5);

    // Connection retry settings
    private int maxRetries = 3;
    private Duration initialDelay = Duration.ofSeconds(1);
    private Duration maxDelay = Duration.ofSeconds(30);
    private double backoffMultiplier = 2.0;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Basic connection settings
        config.setJdbcUrl(databaseUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Connection pool settings
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout.toMillis());
        config.setIdleTimeout(idleTimeout.toMillis());
        config.setMaxLifetime(maxLifetime.toMillis());
        config.setLeakDetectionThreshold(leakDetectionThreshold.toMillis());
        config.setValidationTimeout(validationTimeout.toMillis());

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setConnectionInitSql("SELECT 1");

        // Performance optimizations for read-heavy workloads
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "500");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("defaultFetchSize", "100");

        // Connection pool name for monitoring
        config.setPoolName("QueryServiceHikariCP");

        // Register JMX beans for monitoring
        config.setRegisterMbeans(true);

        return new HikariDataSource(config);
    }

    // Getters and setters for configuration properties
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

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public Duration getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(Duration maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public Duration getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    public void setLeakDetectionThreshold(Duration leakDetectionThreshold) {
        this.leakDetectionThreshold = leakDetectionThreshold;
    }

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
}