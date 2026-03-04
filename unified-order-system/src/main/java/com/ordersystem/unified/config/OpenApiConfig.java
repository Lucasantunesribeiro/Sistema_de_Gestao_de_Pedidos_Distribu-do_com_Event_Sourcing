package com.ordersystem.unified.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Accessible at /swagger-ui/index.html
 *
 * Production-ready with comprehensive API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Unified Order System API")
                .version("1.0.0")
                .description(
                    "Production-ready order management system with event sourcing, " +
                    "inventory management, and payment processing.\n\n" +
                    "Features:\n" +
                    "- Complete order lifecycle management\n" +
                    "- Atomic inventory reservations with automatic expiry\n" +
                    "- Payment processing with multiple methods (PIX, Credit Card, Debit Card)\n" +
                    "- Order cancellation with compensating transactions\n" +
                    "- Event sourcing for audit trail\n" +
                    "- Distributed tracing with correlation IDs"
                )
                .contact(new Contact()
                    .name("Lucas Antunes Ferreira")
                    .email("lucas@example.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080" + contextPath)
                    .description("Local Development Server"),
                new Server()
                    .url("https://api.production.com" + contextPath)
                    .description("Production Server")
            ));
    }
}
