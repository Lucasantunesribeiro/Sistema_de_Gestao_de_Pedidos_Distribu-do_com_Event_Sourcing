package com.ordersystem.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
public final class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);
    private static final Duration BUCKET_STALE_THRESHOLD = Duration.ofMinutes(30);

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, SecurityProperties.TierConfig> tierConfigs;
    private final SecurityProperties.TierConfig defaultConfig;

    public RateLimiterService(Map<String, SecurityProperties.TierConfig> tierConfigs) {
        this.tierConfigs = tierConfigs;
        this.defaultConfig = tierConfigs.getOrDefault("read",
            new SecurityProperties.TierConfig(100, Duration.ofMinutes(1)));
    }

    /**
     * Attempts to consume a token for the given key and tier.
     *
     * @param key  the rate limiting key (e.g., "user:john" or "ip:192.168.1.1")
     * @param tier the rate limit tier (e.g., "read", "write", "admin")
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean tryConsume(String key, String tier) {
        SecurityProperties.TierConfig config = tierConfigs.getOrDefault(tier, defaultConfig);
        String bucketKey = key + ":" + tier;

        return buckets.computeIfAbsent(bucketKey, k -> new TokenBucket(config.getCapacity(), config.getRefillInterval()))
            .tryConsume();
    }

    /**
     * Legacy method for backward compatibility.
     * Uses the default "read" tier.
     */
    public boolean tryConsume(String key) {
        return tryConsume(key, "read");
    }

    /**
     * Scheduled cleanup of stale buckets to prevent memory leaks.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupStaleBuckets() {
        Instant threshold = Instant.now().minus(BUCKET_STALE_THRESHOLD);
        int removed = 0;

        Iterator<Map.Entry<String, TokenBucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TokenBucket> entry = iterator.next();
            if (entry.getValue().isStale(threshold)) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            logger.info("Cleaned up {} stale rate limit buckets", removed);
        }
    }

    /**
     * Returns the current number of active buckets.
     * Useful for monitoring.
     */
    public int getActiveBucketCount() {
        return buckets.size();
    }

    private static final class TokenBucket {
        private final long capacity;
        private final Duration refillInterval;
        private long tokens;
        private Instant lastRefill;
        private Instant lastAccess;

        private TokenBucket(long capacity, Duration refillInterval) {
            this.capacity = capacity;
            this.refillInterval = refillInterval;
            this.tokens = capacity;
            this.lastRefill = Instant.now();
            this.lastAccess = Instant.now();
        }

        synchronized boolean tryConsume() {
            lastAccess = Instant.now();
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        synchronized boolean isStale(Instant threshold) {
            return lastAccess.isBefore(threshold);
        }

        private void refill() {
            Instant now = Instant.now();
            if (!now.isAfter(lastRefill)) {
                return;
            }
            long intervals = Duration.between(lastRefill, now).dividedBy(refillInterval);
            if (intervals > 0) {
                long refillTokens = Math.min(capacity, tokens + intervals);
                tokens = refillTokens;
                lastRefill = lastRefill.plus(refillInterval.multipliedBy(intervals));
            }
        }
    }
}
