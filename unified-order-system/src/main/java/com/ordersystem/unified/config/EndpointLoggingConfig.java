package com.ordersystem.unified.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class EndpointLoggingConfig {
    private static final Logger logger = LoggerFactory.getLogger(EndpointLoggingConfig.class);

    @Bean
    public ApplicationRunner logEndpoints(RequestMappingHandlerMapping mapping) {
        return args -> mapping.getHandlerMethods().forEach((info, method) ->
                logger.info("Mapped {} -> {}", info, method));
    }
}
