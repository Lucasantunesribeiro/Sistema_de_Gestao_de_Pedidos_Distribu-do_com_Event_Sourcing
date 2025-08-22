package com.ordersystem.query.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Enterprise-grade Web Security Configuration
 * SECURITY TARGET: Zero wildcard CORS, environment-specific origins only
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private QueryCorrelationIdInterceptor correlationIdInterceptor;
    
    @Autowired
    private Environment environment;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Environment-specific origins - NO WILDCARDS
        List<String> allowedOrigins = getAllowedOrigins();
        configuration.setAllowedOriginPatterns(allowedOrigins);
        
        // Restrictive method allowlist
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // Specific headers only
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", 
            "X-Correlation-ID", "Accept", "Origin"
        ));
        
        // Enable credentials for JWT cookies
        configuration.setAllowCredentials(true);
        
        // Security headers exposure
        configuration.setExposedHeaders(Arrays.asList(
            "X-Correlation-ID", "X-Total-Count", "X-RateLimit-Limit", 
            "X-RateLimit-Remaining", "X-RateLimit-Reset"
        ));
        
        // Cache preflight for 1 hour only
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }

    private List<String> getAllowedOrigins() {
        String[] activeProfiles = environment.getActiveProfiles();
        
        // Production Railway environment
        if (Arrays.asList(activeProfiles).contains("railway")) {
            return Arrays.asList(
                "https://your-frontend-domain.railway.app",
                "https://api.your-domain.com"
            );
        }
        
        // Docker environment
        if (Arrays.asList(activeProfiles).contains("docker")) {
            return Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://frontend:3000"
            );
        }
        
        // Local development (default)
        return Arrays.asList(
            "http://localhost:3000",
            "http://localhost:8080",
            "http://127.0.0.1:3000"
        );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(correlationIdInterceptor)
                .addPathPatterns("/api/**");
    }
}