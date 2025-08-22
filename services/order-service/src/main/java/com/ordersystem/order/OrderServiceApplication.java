package com.ordersystem.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

@RestController
class SimpleController {

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