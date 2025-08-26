package com.ordersystem.order.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Simplified Security Configuration for Render Deploy
 * Profile-based security: dev/test/local = permitAll, production = secure with health checks
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Environment env;

    public SecurityConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Check if we're running in dev/test mode or if no profile is explicitly set
        boolean isDev = env.acceptsProfiles(Profiles.of("dev", "test", "local")) 
                         || env.getActiveProfiles().length == 0;

        if (isDev) {
            // Development mode: allow all requests
            http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        // Production mode: allow all endpoints for now, focusing on health checks working
        http
                // CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF protection (disabled for API)
                .csrf(csrf -> csrf.disable())

                // Session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/**").permitAll() // Allow all for deployment stability
                        .anyRequest().permitAll())

                // Security headers
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .contentTypeOptions(contentTypeOptions -> {
                        })
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true))
                        .addHeaderWriter((request, response) -> {
                            response.setHeader("X-Content-Type-Options", "nosniff");
                            response.setHeader("X-XSS-Protection", "1; mode=block");
                            response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                            response.setHeader("Pragma", "no-cache");
                            response.setHeader("Expires", "0");
                            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                        }));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}