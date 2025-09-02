package com.ordersystem.unified.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Database configuration that handles Render.com PostgreSQL URL format conversion.
 * Converts URLs like: postgresql://user:pass@host:port/db
 * To JDBC format: jdbc:postgresql://host:port/db
 */
@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL:jdbc:h2:mem:devdb}")
    private String databaseUrl;

    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("Configuring DataSource with DATABASE_URL: {}", 
            databaseUrl.replaceAll("://[^@]+@", "://***:***@")); // Hide credentials in logs
        
        if (databaseUrl.startsWith("postgresql://") || databaseUrl.startsWith("postgres://")) {
            return createPostgreSQLDataSource(databaseUrl);
        } else if (databaseUrl.startsWith("jdbc:postgresql://")) {
            // Already in JDBC format, use as-is
            return DataSourceBuilder.create()
                .url(databaseUrl)
                .driverClassName("org.postgresql.Driver")
                .build();
        } else {
            // Default H2 for development
            return DataSourceBuilder.create()
                .url("jdbc:h2:mem:devdb")
                .username("sa")
                .password("")
                .driverClassName("org.h2.Driver")
                .build();
        }
    }

    private DataSource createPostgreSQLDataSource(String databaseUrl) {
        try {
            URI dbUri = new URI(databaseUrl);
            
            // Extract components
            String host = dbUri.getHost();
            int port = dbUri.getPort();
            String database = dbUri.getPath().substring(1); // Remove leading '/'
            
            // Extract credentials from userInfo
            String userInfo = dbUri.getUserInfo();
            String username = "";
            String password = "";
            
            if (userInfo != null && userInfo.contains(":")) {
                String[] credentials = userInfo.split(":", 2);
                username = credentials[0];
                password = credentials[1];
            }
            
            // Build JDBC URL
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            
            logger.info("Connecting to PostgreSQL at {}:{}/{} as user: {}", host, port, database, username);
            
            return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
                
        } catch (URISyntaxException e) {
            logger.error("Failed to parse DATABASE_URL: {}", databaseUrl, e);
            throw new RuntimeException("Invalid DATABASE_URL format: " + databaseUrl, e);
        }
    }
}