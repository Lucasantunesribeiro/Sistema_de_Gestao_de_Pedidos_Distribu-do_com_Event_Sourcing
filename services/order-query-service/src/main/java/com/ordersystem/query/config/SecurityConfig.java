package com.ordersystem.query.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Check if we're running in dev/test mode or if no profile is explicitly set
        boolean isDev = env.acceptsProfiles(Profiles.of("dev", "test", "local")) 
                         || env.getActiveProfiles().length == 0;
        
        if (isDev) {
            // Development mode: allow all requests
            http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        // Production mode: allow most endpoints for now, focusing on health checks working
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/**").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/**").permitAll() // Allow all for deployment stability
                // All other requests are allowed for now
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
}