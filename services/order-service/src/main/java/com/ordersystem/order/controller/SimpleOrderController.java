package com.ordersystem.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class SimpleOrderController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
            "message", "Order Service is running!",
            "service", "Order Service",
            "version", "1.0.0",
            "status", "UP"
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "service", "Order Service"
        );
    }

    @GetMapping("/actuator/health")
    public Map<String, String> actuatorHealth() {
        return Map.of(
            "status", "UP",
            "service", "Order Service"
        );
    }
}