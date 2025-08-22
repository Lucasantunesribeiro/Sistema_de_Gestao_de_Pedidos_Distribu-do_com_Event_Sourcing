package com.ordersystem.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Payment Service Application
 * Microsserviço responsável pelo processamento de pagamentos
 * no sistema de gestão de pedidos distribuído.
 * 
 * Features:
 * - Processamento de pagamentos via gateways
 * - Event-driven architecture com RabbitMQ
 * - Saga pattern para compensating transactions
 * - Circuit breaker para resiliência
 * - JWT authentication
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}