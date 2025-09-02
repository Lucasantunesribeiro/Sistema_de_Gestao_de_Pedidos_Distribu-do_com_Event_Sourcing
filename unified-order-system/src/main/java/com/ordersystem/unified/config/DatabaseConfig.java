package com.ordersystem.unified.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Database configuration that handles Render.com PostgreSQL URL format conversion.
 */
@Configuration
public class DatabaseConfig {

    @Value("${DATABASE_URL:jdbc:h2:mem:devdb}")
    private String databaseUrl;

    @Value("${DATABASE_USERNAME:sa}")
    private String username;

    @Value("${DATABASE_PASSWORD:}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        // Convert Render.com PostgreSQL URL format to JDBC format
        String jdbcUrl = convertToJdbcUrl(databaseUrl);
        
        return DataSourceBuilder.create()
            .url(jdbcUrl)
            .username(username)
            .password(password)
            .driverClassName(getDriverClassName(jdbcUrl))
            .build();
    }

    private String convertToJdbcUrl(String url) {
        if (url.startsWith("postgresql://")) {
            // Convert postgresql:// to jdbc:postgresql://
            return "jdbc:" + url;
        } else if (url.startsWith("postgres://")) {
            // Convert postgres:// to jdbc:postgresql://
            return url.replace("postgres://", "jdbc:postgresql://");
        }
        // Already in JDBC format or H2
        return url;
    }

    private String getDriverClassName(String url) {
        if (url.contains("postgresql")) {
            return "org.postgresql.Driver";
        } else if (url.contains("h2")) {
            return "org.h2.Driver";
        }
        return "org.postgresql.Driver"; // Default
    }
}