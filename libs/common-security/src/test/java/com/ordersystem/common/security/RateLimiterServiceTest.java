package com.ordersystem.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        Map<String, SecurityProperties.TierConfig> tiers = new HashMap<>();
        tiers.put("read", new SecurityProperties.TierConfig(5, Duration.ofMinutes(1)));
        tiers.put("write", new SecurityProperties.TierConfig(2, Duration.ofMinutes(1)));
        tiers.put("admin", new SecurityProperties.TierConfig(100, Duration.ofMinutes(1)));

        rateLimiterService = new RateLimiterService(tiers);
    }

    @Test
    void testTryConsume_WithinLimit() {
        String key = "user:john";
        String tier = "read";

        // Should allow first 5 requests (capacity = 5)
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.tryConsume(key, tier),
                "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void testTryConsume_ExceedsLimit() {
        String key = "user:jane";
        String tier = "read";

        // Consume all tokens (capacity = 5)
        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryConsume(key, tier);
        }

        // Next request should be denied
        assertFalse(rateLimiterService.tryConsume(key, tier),
            "Request exceeding limit should be denied");
    }

    @Test
    void testTryConsume_DifferentTiers() {
        String key = "user:alice";

        // Read tier allows 5 requests
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.tryConsume(key, "read"));
        }
        assertFalse(rateLimiterService.tryConsume(key, "read"));

        // Write tier (different bucket) should still allow requests
        assertTrue(rateLimiterService.tryConsume(key, "write"));
        assertTrue(rateLimiterService.tryConsume(key, "write"));
        assertFalse(rateLimiterService.tryConsume(key, "write"), "Write tier limit exceeded");
    }

    @Test
    void testTryConsume_DifferentKeys() {
        String tier = "read";

        // Each key has its own bucket
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.tryConsume("user:alice", tier));
        }
        assertFalse(rateLimiterService.tryConsume("user:alice", tier));

        // Different key should have full capacity
        assertTrue(rateLimiterService.tryConsume("user:bob", tier));
    }

    @Test
    void testTryConsume_UnknownTier() {
        String key = "user:charlie";
        String tier = "unknown";

        // Should use default tier configuration (read)
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.tryConsume(key, tier));
        }
        assertFalse(rateLimiterService.tryConsume(key, tier));
    }

    @Test
    void testTryConsume_LegacyMethod() {
        String key = "user:legacy";

        // Legacy method should use "read" tier
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.tryConsume(key),
                "Legacy method should work with default tier");
        }
        assertFalse(rateLimiterService.tryConsume(key));
    }

    @Test
    void testGetActiveBucketCount() {
        assertEquals(0, rateLimiterService.getActiveBucketCount(),
            "Should start with no active buckets");

        rateLimiterService.tryConsume("user:alice", "read");
        rateLimiterService.tryConsume("user:bob", "read");
        rateLimiterService.tryConsume("user:alice", "write");

        assertEquals(3, rateLimiterService.getActiveBucketCount(),
            "Should have 3 active buckets (2 users × 1 tier + 1 user × 1 tier)");
    }

    @Test
    void testAdminTier_HighCapacity() {
        String key = "admin:super";
        String tier = "admin";

        // Admin tier has capacity of 100
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimiterService.tryConsume(key, tier));
        }
        assertFalse(rateLimiterService.tryConsume(key, tier));
    }
}
