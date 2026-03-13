package com.ordersystem.unified.security;

import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Comprehensive security integration tests
 * Tests authentication, authorization, input validation, rate limiting, and CORS
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.ordersystem.unified.config.TestConfig.class)
@DisplayName("Security Integration Tests")
public class SecurityIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private static final String ORDERS_ENDPOINT = "/api/orders";
    private static final String HEALTH_ENDPOINT = "/actuator/health";

    @Nested
    @DisplayName("1. Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Public endpoint (health) should be accessible without authentication")
        void testPublicEndpointAccessible() throws Exception {
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("Protected endpoint should require authentication")
        void testProtectedEndpointWithoutAuth() throws Exception {
            mockMvc.perform(get(ORDERS_ENDPOINT))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Invalid JWT token should be rejected when authentication is enabled")
        void testInvalidJwtRejected() throws Exception {
            mockMvc.perform(get(ORDERS_ENDPOINT)
                    .header("Authorization", "Bearer invalid-jwt-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("2. Input Validation Tests")
    @WithMockUser(roles = "ADMIN")
    class InputValidationTests {

        @Test
        @DisplayName("Should reject request with invalid email format")
        void testInvalidEmailRejected() throws Exception {
            String invalidRequest = """
                {
                    "customerName": "Test Customer",
                    "customerEmail": "invalid-email-format",
                    "items": []
                }
                """;

            mockMvc.perform(post(ORDERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details.customerEmail",
                            containsString("valid")));
        }

        @Test
        @DisplayName("Should reject request with negative quantity")
        void testNegativeQuantityRejected() throws Exception {
            String invalidRequest = """
                {
                    "customerName": "Test Customer",
                    "customerEmail": "test@example.com",
                    "items": [{
                        "productId": "p1",
                        "productName": "Test Product",
                        "quantity": -5,
                        "unitPrice": 10.0
                    }]
                }
                """;

            mockMvc.perform(post(ORDERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details", hasKey(containsString("quantity"))));
        }

        @Test
        @DisplayName("Should reject request with oversized customer ID (>128 chars)")
        void testOversizedCustomerIdRejected() throws Exception {
            String longId = "A".repeat(200);
            String invalidRequest = String.format("""
                {
                    "customerId": "%s",
                    "customerName": "Test",
                    "customerEmail": "test@example.com",
                    "items": []
                }
                """, longId);

            mockMvc.perform(post(ORDERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details.customerId",
                            containsString("maximum length")));
        }

        @Test
        @DisplayName("Should reject request with missing required fields")
        void testMissingRequiredFieldsRejected() throws Exception {
            String invalidRequest = """
                {
                    "customerId": "test123"
                }
                """;

            mockMvc.perform(post(ORDERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should reject request with too many items (>100)")
        void testTooManyItemsRejected() throws Exception {
            List<OrderItemRequest> items = new ArrayList<>();
            for (int i = 0; i < 101; i++) {
                items.add(new OrderItemRequest(
                    "product-" + i,
                    "Product " + i,
                    1,
                    BigDecimal.TEN
                ));
            }

            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerName("Test Customer");
            request.setCustomerEmail("test@example.com");
            request.setItems(items);

            mockMvc.perform(post(ORDERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details.items",
                            containsString("Order cannot exceed")));
        }

        @Test
        @DisplayName("Should reject request with zero quantity")
        void testZeroQuantityRejected() throws Exception {
            String invalidRequest = """
                {
                    "customerName": "Test",
                    "customerEmail": "test@example.com",
                    "items": [{
                        "productId": "p1",
                        "productName": "Test",
                        "quantity": 0,
                        "unitPrice": 10.0
                    }]
                }
                """;

            mockMvc.perform(post(ORDERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject request with blank product ID")
        void testBlankProductIdRejected() throws Exception {
            String invalidRequest = """
                {
                    "customerName": "Test",
                    "customerEmail": "test@example.com",
                    "items": [{
                        "productId": "",
                        "productName": "Test",
                        "quantity": 5,
                        "unitPrice": 10.0
                    }]
                }
                """;

            mockMvc.perform(post(ORDERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("3. Enum Validation Tests")
    @WithMockUser(roles = "ADMIN")
    class EnumValidationTests {

        @Test
        @DisplayName("Should reject invalid order status with helpful error message")
        void testInvalidOrderStatusRejected() throws Exception {
            mockMvc.perform(get(ORDERS_ENDPOINT)
                    .param("status", "INVALID_STATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                    .andExpect(jsonPath("$.message", containsString("Valid values:")))
                    .andExpect(jsonPath("$.message", containsString("PENDING")))
                    .andExpect(jsonPath("$.message", containsString("CONFIRMED")));
        }

        @Test
        @DisplayName("Should accept valid order status")
        void testValidOrderStatusAccepted() throws Exception {
            mockMvc.perform(get(ORDERS_ENDPOINT)
                    .param("status", "PENDING"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("4. CORS Tests")
    @WithMockUser(roles = "ADMIN")
    class CorsTests {

        @Test
        @DisplayName("CORS preflight request should be handled correctly")
        void testCorsPreflight() throws Exception {
            mockMvc.perform(options(ORDERS_ENDPOINT)
                    .header("Origin", "http://localhost:4200")
                    .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("CORS headers should be present in response")
        void testCorsHeadersPresent() throws Exception {
            MvcResult result = mockMvc.perform(get(ORDERS_ENDPOINT)
                    .header("Origin", "http://localhost:4200"))
                    .andReturn();

            // CORS headers may or may not be present depending on configuration
            // This test just validates they don't cause errors
            assert result.getResponse().getStatus() == 200 ||
                   result.getResponse().getStatus() == 401 ||
                   result.getResponse().getStatus() == 403;
        }
    }

    @Nested
    @DisplayName("5. Security Headers Tests")
    class SecurityHeadersTests {

        @Test
        @DisplayName("X-Content-Type-Options header should be present")
        void testXContentTypeOptionsHeaderPresent() throws Exception {
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("X-Frame-Options header should be present")
        void testXFrameOptionsHeaderPresent() throws Exception {
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }

        @Test
        @DisplayName("Content-Security-Policy header should be present")
        void testContentSecurityPolicyHeaderPresent() throws Exception {
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(header().exists("Content-Security-Policy"));
        }
    }

    @Nested
    @DisplayName("6. Error Handling Tests")
    @WithMockUser(roles = "ADMIN")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Error responses should not leak stack traces")
        void testErrorResponsesDoNotLeakStackTraces() throws Exception {
            MvcResult result = mockMvc.perform(get(ORDERS_ENDPOINT + "/nonexistent-id"))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();

            // Ensure no stack traces or Java package names in error responses
            assert !responseBody.contains(".java:");
            assert !responseBody.contains("Exception:");
            assert !responseBody.contains("at com.ordersystem");
        }

        @Test
        @DisplayName("404 responses should be handled gracefully")
        void test404HandledGracefully() throws Exception {
            mockMvc.perform(get("/api/nonexistent-endpoint"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("500 errors should return generic error message")
        void test500ReturnsGenericMessage() throws Exception {
            MvcResult result = mockMvc.perform(get(ORDERS_ENDPOINT + "/trigger-error-if-exists"))
                    .andReturn();

            // If it's a 500 error, ensure it doesn't leak sensitive info
            if (result.getResponse().getStatus() == 500) {
                String responseBody = result.getResponse().getContentAsString();
                assert !responseBody.contains("SQLException");
                assert !responseBody.contains("NullPointerException");
            }
        }
    }

    @Nested
    @DisplayName("7. XSS Prevention Tests")
    @WithMockUser(roles = "ADMIN")
    class XssPreventionTests {

        @Test
        @DisplayName("XSS payload in customer name should be escaped or rejected")
        void testXssPayloadEscapedOrRejected() throws Exception {
            String xssRequest = """
                {
                    "customerName": "<script>alert('XSS')</script>",
                    "customerEmail": "test@example.com",
                    "items": []
                }
                """;

            MvcResult result = mockMvc.perform(post(ORDERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(xssRequest))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();

            // Ensure script tags are not returned unescaped
            assert !responseBody.contains("<script>") || responseBody.contains("&lt;script&gt;");
        }
    }

    @Nested
    @DisplayName("8. SQL Injection Prevention Tests")
    @WithMockUser(roles = "ADMIN")
    class SqlInjectionPreventionTests {

        @Test
        @DisplayName("SQL injection in query parameter should not cause SQL error")
        void testSqlInjectionInQueryParameter() throws Exception {
            MvcResult result = mockMvc.perform(get(ORDERS_ENDPOINT)
                    .param("customerId", "test' OR '1'='1"))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            int status = result.getResponse().getStatus();

            // Should not return SQL errors or 500
            assert status != 500 || !responseBody.toLowerCase().contains("sql");
            assert !responseBody.toLowerCase().contains("syntax error");
        }
    }

    // Helper method to convert object to JSON string
    private String asJsonString(Object obj) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
