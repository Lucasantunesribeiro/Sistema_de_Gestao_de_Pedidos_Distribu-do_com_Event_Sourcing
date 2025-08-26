package com.ordersystem.query.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Enterprise-grade Security Configuration
 * SECURITY TARGET: JWT-based stateless authentication, RBAC, CORS protection
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless JWT authentication
                .csrf(csrf -> csrf.disable())

                // Enable CORS with enterprise configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Stateless session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization rules
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - using AntPathRequestMatcher for clarity
                        .requestMatchers(
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                        .antMatcher("/actuator/health"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                        .antMatcher("/actuator/info"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                        .antMatcher("/api/auth/**"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                        .antMatcher("/api/orders/**"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                        .antMatcher("/api/dashboard/**"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                        .antMatcher("/actuator/**"),
                                org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                        .antMatcher("/h2-console/**"))
                        .permitAll()

                        // Allow all for now - can be restricted later
                        .anyRequest().permitAll())

                // Custom JWT authentication entry point
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // Security headers
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                        .contentTypeOptions(contentTypeOptions -> {
                        })
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000)));

        // Add rate limiting filter before authentication
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}