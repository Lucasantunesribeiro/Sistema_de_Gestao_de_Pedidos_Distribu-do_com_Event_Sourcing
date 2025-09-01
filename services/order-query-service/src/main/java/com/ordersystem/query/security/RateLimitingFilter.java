package com.ordersystem.query.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Enterprise-grade Rate Limiting Filter
 * SECURITY TARGET: 100 requests per minute per user, Redis-based tracking
 * TEMPORARILY DISABLED for production deployment validation
 */
// @Component - DISABLED until Redis configuration is fully validated
public class RateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Rate limiting configuration
    private static final int RATE_LIMIT_PER_USER = 100;
    private static final int RATE_LIMIT_PER_IP = 50; // Lower limit for anonymous users
    private static final int WINDOW_SIZE_MINUTES = 1;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract user identity (prefer user ID over IP)
            Optional<String> userId = extractUserId(httpRequest);
            String clientIp = getClientIp(httpRequest);

            // Create rate limit key with appropriate identifier
            String rateLimitKey;
            int currentLimit;

            if (userId.isPresent()) {
                rateLimitKey = "rate_limit:user:" + userId.get();
                currentLimit = RATE_LIMIT_PER_USER;
            } else {
                rateLimitKey = "rate_limit:ip:" + clientIp;
                currentLimit = RATE_LIMIT_PER_IP;
            }

            // Check and increment rate limit
            RateLimitResult result = checkAndIncrementRateLimit(rateLimitKey, currentLimit);

            // Add rate limit headers
            addRateLimitHeaders(httpResponse, result, currentLimit);

            if (result.isBlocked()) {
                handleRateLimitExceeded(httpResponse, rateLimitKey);
                return;
            }

            // Log successful rate limit check
            logger.debug("Rate limit check passed for key: {} (count: {}/{})",
                    rateLimitKey, result.getCurrentCount(), currentLimit);

        } catch (Exception e) {
            logger.error("Error in rate limiting filter", e);
            // Continue processing on filter error to avoid breaking requests
        }

        chain.doFilter(request, response);
    }

    private Optional<String> extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                if (jwtTokenService.validateAccessToken(token)) {
                    return Optional.of(jwtTokenService.getUserIdFromToken(token));
                }
            } catch (Exception e) {
                logger.debug("Failed to extract user ID from JWT token", e);
            }
        }

        return Optional.empty();
    }

    private String getClientIp(HttpServletRequest request) {
        // Check various headers for real IP (behind proxy/load balancer)
        String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "X-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private RateLimitResult checkAndIncrementRateLimit(String key, int limit) {
        try {
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = 0;

            if (currentCountStr == null) {
                // First request - set initial count with expiration
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(WINDOW_SIZE_MINUTES));
                currentCount = 1;
            } else {
                currentCount = Integer.parseInt(currentCountStr);

                if (currentCount >= limit) {
                    return new RateLimitResult(true, currentCount, getRemainingTtl(key));
                }

                // Increment counter
                currentCount = redisTemplate.opsForValue().increment(key).intValue();
            }

            return new RateLimitResult(false, currentCount, getRemainingTtl(key));

        } catch (Exception e) {
            logger.error("Error checking rate limit for key: " + key, e);
            // On Redis error, allow request to avoid service disruption
            return new RateLimitResult(false, 0, 0);
        }
    }

    private long getRemainingTtl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null ? ttl : WINDOW_SIZE_MINUTES * 60;
        } catch (Exception e) {
            logger.error("Error getting TTL for key: " + key, e);
            return WINDOW_SIZE_MINUTES * 60;
        }
    }

    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result, int limit) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - result.getCurrentCount())));
        response.setHeader("X-RateLimit-Reset",
                String.valueOf(System.currentTimeMillis() + (result.getResetTime() * 1000)));
    }

    private void handleRateLimitExceeded(HttpServletResponse response, String rateLimitKey)
            throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = """
                {
                    "error": "Rate limit exceeded",
                    "message": "Too many requests. Please try again later.",
                    "code": "RATE_LIMIT_EXCEEDED"
                }
                """;

        response.getWriter().write(jsonResponse);

        logger.warn("Rate limit exceeded for key: {}", rateLimitKey);
    }

    /**
     * Rate limit check result
     */
    private static class RateLimitResult {
        private final boolean blocked;
        private final int currentCount;
        private final long resetTime;

        public RateLimitResult(boolean blocked, int currentCount, long resetTime) {
            this.blocked = blocked;
            this.currentCount = currentCount;
            this.resetTime = resetTime;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public int getCurrentCount() {
            return currentCount;
        }

        public long getResetTime() {
            return resetTime;
        }
    }
}