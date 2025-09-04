package com.ordersystem.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Production Security Configuration
 * Initial safe configuration with permitAll() for gradual security
 * implementation
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Production-ready configuration with initial permitAll() approach
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(http -> http.disable())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/orders/**").permitAll() // Initial permitAll for validation
                        .anyRequest().permitAll());

        return http.build();
    }
}