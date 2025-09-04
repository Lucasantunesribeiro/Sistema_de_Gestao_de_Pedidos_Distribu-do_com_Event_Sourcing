package com.ordersystem.order.config;

import java.net.URI;
import java.net.URISyntaxException;

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

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("🔧 Configuring PostgreSQL DataSource for Render");

        String jdbcUrl = databaseUrl;
        String user = username;
        String pass = password;

        try {
            if (databaseUrl.startsWith("postgresql://")) {
                URI uri = new URI(databaseUrl);
                String hostPort = uri.getHost();
                if (uri.getPort() != -1) {
                    hostPort += ":" + uri.getPort();
                }
                jdbcUrl = "jdbc:postgresql://" + hostPort + uri.getPath();

                if ((user == null || user.isBlank()) && uri.getUserInfo() != null) {
                    String[] parts = uri.getUserInfo().split(":", 2);
                    user = parts[0];
                    if (parts.length > 1) {
                        pass = parts[1];
                    }
                }
                logger.info("🔄 Converted Render DATABASE_URL format: {} -> {}", databaseUrl, jdbcUrl);
            }
        } catch (URISyntaxException e) {
            logger.error("❌ Invalid DATABASE_URL format: {}", databaseUrl, e);
        }

        DataSource dataSource = DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(user)
                .password(pass)
                .driverClassName("org.postgresql.Driver")
                .build();

        logger.info("✅ PostgreSQL DataSource configured successfully");
        return dataSource;
    }
}