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
            logger.info("Parsing Render DATABASE_URL for PostgreSQL connection");
            
            URI dbUri = new URI(databaseUrl);
            
            // Extract components with validation
            String host = dbUri.getHost();
            int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort(); // Default PostgreSQL port
            String path = dbUri.getPath();
            String database = (path != null && path.length() > 1) ? path.substring(1) : "postgres";
            
            // Validate required components
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("Database host is required in DATABASE_URL");
            }
            
            // Extract credentials from userInfo with validation
            String userInfo = dbUri.getUserInfo();
            String username = "";
            String password = "";
            
            if (userInfo != null && userInfo.contains(":")) {
                String[] credentials = userInfo.split(":", 2);
                username = credentials[0];
                password = credentials.length > 1 ? credentials[1] : "";
            } else if (userInfo != null) {
                username = userInfo;
            }
            
            // Validate credentials
            if (username.trim().isEmpty()) {
                throw new IllegalArgumentException("Database username is required in DATABASE_URL");
            }
            
            // Build JDBC URL with proper formatting
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            
            logger.info("PostgreSQL Connection Details:");
            logger.info("  Host: {}", host);
            logger.info("  Port: {}", port);
            logger.info("  Database: {}", database);
            logger.info("  Username: {}", username);
            logger.info("  JDBC URL: {}", jdbcUrl);
            
            // Create DataSource with explicit configuration
            DataSource dataSource = DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
                
            logger.info("PostgreSQL DataSource created successfully");
            return dataSource;
                
        } catch (URISyntaxException e) {
            logger.error("Failed to parse DATABASE_URL as URI: {}", databaseUrl, e);
            throw new RuntimeException("Invalid DATABASE_URL format. Expected: postgresql://user:pass@host:port/db", e);
        } catch (Exception e) {
            logger.error("Failed to create PostgreSQL DataSource", e);
            throw new RuntimeException("Database configuration error: " + e.getMessage(), e);
        }
    }
}