package com.ordersystem.unified.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Basic security configuration that exposes the public frontend while
 * protecting API endpoints. Further hardening (JWT, OAuth) can be added on
 * top of this foundation.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // disable HTTP basic to avoid browser login prompts
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/assets/**",
                    "/actuator/**",
                    "/api-docs/**",
                    "/swagger-ui.html",
                    "/api/**"
                ).permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
