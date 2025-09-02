package com.ordersystem.unified.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the unified order system.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private CorrelationInterceptor correlationInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(correlationInterceptor)
                .addPathPatterns("/api/**");
    }
}