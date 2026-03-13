package com.ordersystem.unified.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.auth.model.ApplicationUser;
import com.ordersystem.unified.auth.repository.ApplicationUserRepository;
import com.ordersystem.unified.config.TestConfig;
import com.ordersystem.unified.support.PostgresIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "security.enforce-authentication=true",
    "security.bootstrap-admin.enabled=true",
    "security.bootstrap-admin.username=admin",
    "security.bootstrap-admin.password=Admin123!SecurePass",
    "security.bootstrap-admin.email=admin@example.com"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(classes = {com.ordersystem.unified.Application.class, TestConfig.class})
@DisplayName("Authentication Flow Integration Tests")
class AuthenticationFlowIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsers() {
        ensureUserExists("operator", "operator@example.com", "Operator123!SecurePass", "OPERATOR");
        ensureUserExists("viewer", "viewer@example.com", "Viewer123!SecurePass", "VIEWER");
    }

    @Test
    @DisplayName("Protected order endpoint requires authentication")
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login returns JWT and current user payload")
    void loginReturnsJwtAndCurrentUser() throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "Admin123!SecurePass"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("admin"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String token = readToken(responseBody);

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("Operator can access orders but cannot refund payments")
    void operatorHasLimitedAccess() throws Exception {
        String token = authenticate("operator", "Operator123!SecurePass");

        mockMvc.perform(get("/api/orders")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments/refund")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "paymentId": "payment-123"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Viewer can access dashboard but not order management")
    void viewerCanReadDashboardOnly() throws Exception {
        String token = authenticate("viewer", "Viewer123!SecurePass");

        mockMvc.perform(get("/api/dashboard")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Order Management Dashboard"));

        mockMvc.perform(get("/api/orders")
                .header("Authorization", bearer(token)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test endpoints stay protected even when controller is loaded in test profile")
    void testEndpointsAreNotPublic() throws Exception {
        mockMvc.perform(get("/api/test"))
            .andExpect(status().isUnauthorized());

        String token = authenticate("admin", "Admin123!SecurePass");

        mockMvc.perform(get("/api/test")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"));
    }

    private String authenticate(String username, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return readToken(responseBody);
    }

    private String readToken(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void ensureUserExists(String username, String email, String rawPassword, String role) {
        if (applicationUserRepository.findByUsernameIgnoreCase(username).isPresent()) {
            return;
        }

        applicationUserRepository.save(new ApplicationUser(
            UUID.randomUUID(),
            username,
            passwordEncoder.encode(rawPassword),
            email,
            role,
            true
        ));
    }
}
