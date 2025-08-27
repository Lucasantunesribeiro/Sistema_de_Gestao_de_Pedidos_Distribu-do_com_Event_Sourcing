package com.ordersystem.query.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Rate Limiting Security Tests - Enterprise-grade per-user limits
 * TARGET: 100 req/min per user, Redis-based tracking
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.redis.host=${SPRING_REDIS_HOST:127.0.0.1}",
    "spring.redis.port=${SPRING_REDIS_PORT:6379}"
})
class RateLimitingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Clear Redis for clean test state
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void shouldAllowRequestsWithinRateLimit() throws Exception {
        String userId = "user123";
        String jwtToken = generateValidJwtToken(userId, "CUSTOMER");
        
        // Make 5 requests within limit (100/min)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/orders")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldBlockRequestsExceedingUserRateLimit() throws Exception {
        String userId = "user123";
        String jwtToken = generateValidJwtToken(userId, "CUSTOMER");
        
        // Simulate 101 requests in the same minute window
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/orders")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk());
        }
        
        // 101st request should be rate limited
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded"));
    }

    @Test
    void shouldTrackDifferentUsersIndependently() throws Exception {
        String user1 = "user1";
        String user2 = "user2";
        String jwtToken1 = generateValidJwtToken(user1, "CUSTOMER");
        String jwtToken2 = generateValidJwtToken(user2, "CUSTOMER");
        
        // User1 exceeds limit
        for (int i = 0; i < 101; i++) {
            mockMvc.perform(get("/api/orders")
                    .header("Authorization", "Bearer " + jwtToken1));
        }
        
        // User2 should still be allowed
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + jwtToken2))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFallbackToIpBasedRateLimitingForAnonymousUsers() throws Exception {
        String clientIp = "192.168.1.100";
        
        // Make requests without JWT token
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/orders")
                    .header("X-Forwarded-For", clientIp))
                    .andExpect(status().isUnauthorized()); // Will be unauthorized but rate limiting still applies
        }
        
        // Should be rate limited by IP
        mockMvc.perform(get("/api/orders")
                .header("X-Forwarded-For", clientIp))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldRespectRedisExpirationForRateLimit() throws Exception {
        String userId = "user123";
        String rateLimitKey = "rate_limit:user:" + userId;
        
        // Manually set a key that expires in 1 second
        redisTemplate.opsForValue().set(rateLimitKey, "99");
        redisTemplate.expire(rateLimitKey, java.time.Duration.ofSeconds(1));
        
        // Wait for expiration
        Thread.sleep(1100);
        
        // Should allow new requests after expiration
        String jwtToken = generateValidJwtToken(userId, "CUSTOMER");
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldIncludeRateLimitHeaders() throws Exception {
        String userId = "user123";
        String jwtToken = generateValidJwtToken(userId, "CUSTOMER");
        
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    private String generateValidJwtToken(String userId, String role) {
        // Mock JWT generation - will be implemented in actual JWT service
        return "mock.jwt.token." + userId + "." + role;
    }
}