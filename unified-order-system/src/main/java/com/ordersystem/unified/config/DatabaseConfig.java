package com.ordersystem.unified.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.beans.factory.annotation.Value;

/**
 * Database configuration for the unified order system.
 * Handles different database configurations for different environments.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.ordersystem.unified")
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    /**
     * Primary DataSource configuration.
     * Uses environment-specific database settings.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.url")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    /**
     * Development profile specific configuration for H2 database.
     */
    @Configuration
    @Profile("dev")
    static class DevelopmentDatabaseConfig {
        // H2 specific configurations if needed
    }

    /**
     * Production profile specific configuration for PostgreSQL.
     */
    @Configuration
    @Profile("production")
    static class ProductionDatabaseConfig {
        // PostgreSQL specific configurations if needed
    }
}