package com.ordersystem.unified.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Explicit JPA Repository Configuration.
 * 
 * This configuration resolves the "Multiple Spring Data modules found" conflict
 * by explicitly defining which packages contain JPA repositories and entities.
 * This eliminates ambiguity during Spring Boot's component scanning and ensures
 * proper initialization of the application context.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.ordersystem.unified.order.repository",
        "com.ordersystem.unified.payment.repository", 
        "com.ordersystem.unified.inventory.repository"
    }
)
@EntityScan(
    basePackages = {
        "com.ordersystem.unified.order.model",
        "com.ordersystem.unified.payment.model",
        "com.ordersystem.unified.inventory.model"
    }
)
@EnableTransactionManagement
public class JpaRepositoriesConfig {
    
    private static final Logger log = LoggerFactory.getLogger(JpaRepositoriesConfig.class);
    
    /**
     * Explicit JPA configuration to resolve Spring Data module conflicts.
     * 
     * By explicitly defining the repository and entity packages, we eliminate
     * the ambiguity that causes the "strict repository configuration mode"
     * and prevents the application from completing its startup sequence.
     */
    
    @PostConstruct
    public void started() {
        log.info("JpaRepositoriesConfig loaded - JPA-only repositories configured");
    }
}