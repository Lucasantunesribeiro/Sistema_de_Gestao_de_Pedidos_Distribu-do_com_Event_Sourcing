package com.ordersystem.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that applies rate limiting to incoming HTTP requests.
 * Uses multi-dimensional rate limiting with IP-based and user-based keys.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimiterService rateLimiterService;
    private final SecurityProperties securityProperties;

    public RateLimitingFilter(RateLimiterService rateLimiterService,
                              SecurityProperties securityProperties) {
        this.rateLimiterService = rateLimiterService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        // Skip if rate limiting is disabled
        if (!securityProperties.getRateLimiting().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for health checks and public paths
        String requestPath = request.getRequestURI();
        if (shouldSkipRateLimiting(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildRateLimitKey(request);
        String tier = determineRateLimitTier(request);

        if (!rateLimiterService.tryConsume(key, tier)) {
            logger.warn("Rate limit exceeded for key: {}, tier: {}, path: {}", key, tier, requestPath);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Builds a rate limiting key based on authentication status.
     * - Authenticated users: "user:{username}"
     * - Unauthenticated: "ip:{clientIp}"
     */
    private String buildRateLimitKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            return "user:" + username;
        }

        // Fallback to IP-based rate limiting
        String clientIp = IpExtractor.extractClientIp(request);
        return "ip:" + IpExtractor.normalizeIp(clientIp);
    }

    /**
     * Determines the rate limit tier based on HTTP method.
     * - POST/PUT/DELETE → "write" tier (stricter)
     * - GET → "read" tier (permissive)
     * - HEAD/OPTIONS → "read" tier
     */
    private String determineRateLimitTier(HttpServletRequest request) {
        String method = request.getMethod();

        return switch (method) {
            case "POST", "PUT", "DELETE", "PATCH" -> "write";
            case "GET", "HEAD", "OPTIONS" -> "read";
            default -> "read";
        };
    }

    /**
     * Determines if rate limiting should be skipped for a given path.
     * Skips for health checks, error pages, and static resources.
     */
    private boolean shouldSkipRateLimiting(String path) {
        return path.equals("/error") ||
               path.equals("/actuator/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/webjars/") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/img/") ||
               path.startsWith("/fonts/") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".ico") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".svg");
    }
}
