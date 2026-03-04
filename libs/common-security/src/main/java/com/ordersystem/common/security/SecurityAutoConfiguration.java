package com.ordersystem.common.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@AutoConfiguration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnClass(HttpSecurity.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityAutoConfiguration {

    @Bean
    public JwtTokenService jwtTokenService(SecurityProperties securityProperties) {
        String secret = securityProperties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("security.secret must be configured");
        }
        return new JwtTokenService(secret, securityProperties.getTokenValidity());
    }

    @Bean
    public RateLimiterService rateLimiterService(SecurityProperties securityProperties) {
        return new RateLimiterService(securityProperties.getRateLimiting().getTiers());
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationFilter(jwtTokenService);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityProperties securityProperties,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        // Build effective allowlist from publicPaths + servicePublicPaths
        List<String> effectivePublicPaths = new ArrayList<>(securityProperties.getPublicPaths());

        // Add service-specific public paths
        for (List<String> servicePaths : securityProperties.getServicePublicPaths().values()) {
            effectivePublicPaths.addAll(servicePaths);
        }

        if (securityProperties.isH2ConsoleEnabled()) {
            effectivePublicPaths.add("/h2-console/**");
        }

        // Add actuator public endpoints
        SecurityProperties.ActuatorSecurity actuator = securityProperties.getActuator();
        for (String endpoint : actuator.getPublicEndpoints()) {
            effectivePublicPaths.add("/actuator/" + endpoint);
            effectivePublicPaths.add("/actuator/" + endpoint + "/**");
        }

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // Public paths
                for (String pattern : effectivePublicPaths) {
                    if (StringUtils.hasText(pattern)) {
                        auth.requestMatchers(new AntPathRequestMatcher(pattern.trim())).permitAll();
                    }
                }

                // Protected actuator endpoints (require authentication)
                if (actuator.isRequireAuthentication()) {
                    for (String endpoint : actuator.getProtectedEndpoints()) {
                        auth.requestMatchers(new AntPathRequestMatcher("/actuator/" + endpoint))
                            .authenticated();
                        auth.requestMatchers(new AntPathRequestMatcher("/actuator/" + endpoint + "/**"))
                            .authenticated();
                    }

                    for (String endpoint : actuator.getAdminEndpoints()) {
                        auth.requestMatchers(new AntPathRequestMatcher("/actuator/" + endpoint))
                            .hasRole("ADMIN");
                        auth.requestMatchers(new AntPathRequestMatcher("/actuator/" + endpoint + "/**"))
                            .hasRole("ADMIN");
                    }
                }

                // Apply authentication enforcement
                if (securityProperties.isEnforceAuthentication()) {
                    auth.anyRequest().authenticated();
                } else {
                    auth.anyRequest().permitAll();
                }
            })
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .frameOptions(frame -> {
                    if (securityProperties.isH2ConsoleEnabled()) {
                        frame.sameOrigin();
                    } else {
                        frame.deny();
                    }
                })
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(securityProperties.getContentSecurityPolicy()))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties securityProperties) {
        SecurityProperties.Cors cors = securityProperties.getCors();
        List<String> allowedOrigins = normalize(cors.getAllowedOrigins());

        if (cors.isAllowCredentials() && allowedOrigins.contains("*")) {
            throw new IllegalStateException("security.cors.allowed-origins cannot contain '*' when allow-credentials is true");
        }

        CorsConfiguration configuration = new CorsConfiguration();
        if (!allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(allowedOrigins);
        }
        configuration.setAllowedMethods(normalize(cors.getAllowedMethods()));
        configuration.setAllowedHeaders(normalize(cors.getAllowedHeaders()));
        configuration.setAllowCredentials(cors.isAllowCredentials());
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static List<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                normalized.add(value.trim());
            }
        }
        return normalized;
    }
}
