package com.ordersystem.common.security;

import java.util.HashMap;
import java.util.List;
import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private String secret;
    private Duration tokenValidity = Duration.ofMinutes(30);

    // Multi-tier allowlist approach
    private List<String> publicPaths = List.of(
        "/error",
        "/actuator/health",  // Only health, not all actuator
        "/api/auth/**",      // Auth endpoints
        "/swagger-ui/**",    // API docs
        "/swagger-ui.html",
        "/v3/api-docs/**"
    );

    private List<String> protectedPaths = List.of("/api/**");

    // Per-service overrides via config
    private Map<String, List<String>> servicePublicPaths = new HashMap<>();

    // Environment-specific enforcement
    private boolean enforceAuthentication = true;  // false in dev/test

    private String contentSecurityPolicy = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'";

    private boolean h2ConsoleEnabled = false;
    private Cors cors = new Cors();
    private RateLimiting rateLimiting = new RateLimiting();
    private ActuatorSecurity actuator = new ActuatorSecurity();
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getTokenValidity() {
        return tokenValidity;
    }

    public void setTokenValidity(Duration tokenValidity) {
        this.tokenValidity = tokenValidity;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public List<String> getProtectedPaths() {
        return protectedPaths;
    }

    public void setProtectedPaths(List<String> protectedPaths) {
        this.protectedPaths = protectedPaths;
    }

    public Map<String, List<String>> getServicePublicPaths() {
        return servicePublicPaths;
    }

    public void setServicePublicPaths(Map<String, List<String>> servicePublicPaths) {
        this.servicePublicPaths = servicePublicPaths;
    }

    public boolean isEnforceAuthentication() {
        return enforceAuthentication;
    }

    public void setEnforceAuthentication(boolean enforceAuthentication) {
        this.enforceAuthentication = enforceAuthentication;
    }

    public RateLimiting getRateLimiting() {
        return rateLimiting;
    }

    public void setRateLimiting(RateLimiting rateLimiting) {
        this.rateLimiting = rateLimiting;
    }

    public ActuatorSecurity getActuator() {
        return actuator;
    }

    public void setActuator(ActuatorSecurity actuator) {
        this.actuator = actuator;
    }

    public boolean isH2ConsoleEnabled() {
        return h2ConsoleEnabled;
    }

    public void setH2ConsoleEnabled(boolean h2ConsoleEnabled) {
        this.h2ConsoleEnabled = h2ConsoleEnabled;
    }

    public String getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    public void setContentSecurityPolicy(String contentSecurityPolicy) {
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public BootstrapAdmin getBootstrapAdmin() {
        return bootstrapAdmin;
    }

    public void setBootstrapAdmin(BootstrapAdmin bootstrapAdmin) {
        this.bootstrapAdmin = bootstrapAdmin;
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of();
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials = false;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }
    }

    public static class RateLimiting {
        private boolean enabled = true;
        private Map<String, TierConfig> tiers = new HashMap<>();

        public RateLimiting() {
            // Default tiers
            tiers.put("read", new TierConfig(100, Duration.ofMinutes(1)));
            tiers.put("write", new TierConfig(30, Duration.ofMinutes(1)));
            tiers.put("admin", new TierConfig(1000, Duration.ofMinutes(1)));
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, TierConfig> getTiers() {
            return tiers;
        }

        public void setTiers(Map<String, TierConfig> tiers) {
            this.tiers = tiers;
        }
    }

    public static class TierConfig {
        private long capacity;
        private Duration refillInterval;

        public TierConfig() {
        }

        public TierConfig(long capacity, Duration refillInterval) {
            this.capacity = capacity;
            this.refillInterval = refillInterval;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public Duration getRefillInterval() {
            return refillInterval;
        }

        public void setRefillInterval(Duration refillInterval) {
            this.refillInterval = refillInterval;
        }
    }

    public static class ActuatorSecurity {
        private List<String> publicEndpoints = List.of("health");
        private List<String> protectedEndpoints = List.of("info", "metrics", "prometheus");
        private List<String> adminEndpoints = List.of("env", "shutdown", "threaddump");
        private boolean requireAuthentication = true;

        public List<String> getPublicEndpoints() {
            return publicEndpoints;
        }

        public void setPublicEndpoints(List<String> publicEndpoints) {
            this.publicEndpoints = publicEndpoints;
        }

        public List<String> getProtectedEndpoints() {
            return protectedEndpoints;
        }

        public void setProtectedEndpoints(List<String> protectedEndpoints) {
            this.protectedEndpoints = protectedEndpoints;
        }

        public List<String> getAdminEndpoints() {
            return adminEndpoints;
        }

        public void setAdminEndpoints(List<String> adminEndpoints) {
            this.adminEndpoints = adminEndpoints;
        }

        public boolean isRequireAuthentication() {
            return requireAuthentication;
        }

        public void setRequireAuthentication(boolean requireAuthentication) {
            this.requireAuthentication = requireAuthentication;
        }
    }

    public static class BootstrapAdmin {
        private boolean enabled = false;
        private String username;
        private String password;
        private String email;
        private List<String> roles = List.of("ADMIN");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
