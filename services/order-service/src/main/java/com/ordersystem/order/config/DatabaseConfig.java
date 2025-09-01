package com.ordersystem.order.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Database Configuration for Render PostgreSQL
 * Handles URL format conversion from Render format to JDBC format
 */
@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("🔧 Configuring PostgreSQL DataSource for Render");

        // Convert Render DATABASE_URL format to JDBC format
        String jdbcUrl = databaseUrl;
        if (databaseUrl.startsWith("postgresql://")) {
            jdbcUrl = "jdbc:" + databaseUrl;
            logger.info("🔄 Converted Render DATABASE_URL format: {} -> {}", databaseUrl, jdbcUrl);
        }

        DataSource dataSource = DataSourceBuilder.create()
                .url(jdbcUrl)
                .driverClassName("org.postgresql.Driver")
                .build();

        logger.info("✅ PostgreSQL DataSource configured successfully");
        return dataSource;
    }
}