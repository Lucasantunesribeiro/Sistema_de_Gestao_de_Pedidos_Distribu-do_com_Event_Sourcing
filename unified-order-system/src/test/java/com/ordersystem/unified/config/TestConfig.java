package com.ordersystem.unified.config;

import com.ordersystem.common.security.JwtTokenService;
import com.ordersystem.common.security.SecurityProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityProperties securityProperties() {
        SecurityProperties properties = new SecurityProperties();
        properties.setSecret("test-secret-key-test-secret-key-1234567890");
        properties.setEnforceAuthentication(true);
        return properties;
    }

    @Bean
    public JwtTokenService jwtTokenService(SecurityProperties securityProperties) {
        return new JwtTokenService(securityProperties.getSecret(), securityProperties.getTokenValidity());
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        return Mockito.mock(ConnectionFactory.class);
    }

    @Bean
    public SimpleRabbitListenerContainerFactoryConfigurer simpleRabbitListenerContainerFactoryConfigurer() {
        return Mockito.mock(SimpleRabbitListenerContainerFactoryConfigurer.class);
    }
}
