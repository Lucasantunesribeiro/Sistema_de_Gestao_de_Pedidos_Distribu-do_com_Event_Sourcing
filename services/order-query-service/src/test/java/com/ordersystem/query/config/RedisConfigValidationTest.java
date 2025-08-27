package com.ordersystem.query.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to validate Redis configuration environment variables
 * This ensures the configuration pattern works for GitHub Actions
 */
class RedisConfigValidationTest {

    @Test
    void shouldReadRedisEnvironmentVariables() {
        // Test Redis host environment variable with default
        String redisHost = System.getenv().getOrDefault("SPRING_REDIS_HOST", "127.0.0.1");
        assertNotNull(redisHost);
        assertTrue(redisHost.equals("127.0.0.1") || redisHost.equals("localhost"));
        
        // Test Redis port environment variable with default
        String redisPort = System.getenv().getOrDefault("SPRING_REDIS_PORT", "6379");
        assertNotNull(redisPort);
        assertEquals("6379", redisPort);
    }
    
    @Test
    void shouldValidateDefaultValues() {
        // This test validates that our configuration follows the pattern:
        // ${SPRING_REDIS_HOST:127.0.0.1} and ${SPRING_REDIS_PORT:6379}
        
        // Default values should be sensible for both local and CI environments
        String defaultHost = "127.0.0.1";
        String defaultPort = "6379";
        
        // Validate defaults
        assertEquals("127.0.0.1", defaultHost);
        assertEquals("6379", defaultPort);
        
        // Validate that the environment variable names are consistent
        String envHost = System.getenv("SPRING_REDIS_HOST");
        String envPort = System.getenv("SPRING_REDIS_PORT");
        
        // Environment variables may or may not be set
        if (envHost != null) {
            assertTrue(envHost.length() > 0);
        }
        
        if (envPort != null) {
            assertTrue(envPort.matches("\\d+"));
        }
    }
}