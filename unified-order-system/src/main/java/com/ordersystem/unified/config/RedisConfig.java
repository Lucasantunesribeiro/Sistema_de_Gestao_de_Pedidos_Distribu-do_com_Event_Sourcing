package com.ordersystem.unified.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Explicit Redis configuration that parses REDIS_URL and creates a
 * LettuceConnectionFactory. This avoids the costly Redis auto-configuration
 * that previously slowed application startup and caused repository conflicts.
 */
@Configuration
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${REDIS_URL:}")
    private String redisUrl;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        if (redisUrl == null || redisUrl.isBlank()) {
            throw new IllegalStateException("REDIS_URL must be provided");
        }
        try {
            URI uri = new URI(redisUrl);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 6379 : uri.getPort();
            String password = null;
            if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
                password = uri.getUserInfo().split(":",2)[1];
            }
            logger.info("Configuring Redis connection: host={}, port={}", host, port);
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(host);
            config.setPort(port);
            if (password != null && !password.isBlank()) {
                config.setPassword(password);
            }
            return new LettuceConnectionFactory(config);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid REDIS_URL format", e);
        }
    }
}
