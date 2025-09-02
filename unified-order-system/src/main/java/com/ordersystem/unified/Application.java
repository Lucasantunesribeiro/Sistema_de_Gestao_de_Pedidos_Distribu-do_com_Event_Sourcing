package com.ordersystem.unified;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot Application for Unified Order System
 * 
 * This application consolidates all order management functionality
 * into a single modular monolith for simplified deployment and operation.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}