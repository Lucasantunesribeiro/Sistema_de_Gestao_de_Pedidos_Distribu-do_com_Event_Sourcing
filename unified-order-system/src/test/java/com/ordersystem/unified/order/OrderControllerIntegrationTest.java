package com.ordersystem.unified.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for OrderController with real database persistence
 */
@SpringBootTest
@AutoConfigureTestMvc
@ActiveProfiles("test")
@Transactional
public class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateAndRetrieveOrderFlow() throws Exception {
        // Create a simplified order
        Map<String, Object> orderRequest = Map.of(
            "customerName", "João Silva",
            "items", List.of(Map.of(
                "productName", "Notebook Dell",
                "price", 2500.00,
                "quantity", 1
            )),
            "totalAmount", 2500.00
        );

        // POST: Create order
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.customerName").value("João Silva"))
                .andExpect(jsonPath("$.totalAmount").value(2500.00))
                .andReturn();

        // Extract order ID from response
        String responseContent = createResult.getResponse().getContentAsString();
        Map<String, Object> createResponse = objectMapper.readValue(responseContent, Map.class);
        String orderId = (String) createResponse.get("orderId");

        // GET: Retrieve all orders - should include the created order
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[?(@.orderId == '" + orderId + "')]").exists())
                .andExpect(jsonPath("$[?(@.orderId == '" + orderId + "')].customerName").value("João Silva"));

        // GET: Retrieve specific order by ID
        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.customerName").value("João Silva"))
                .andExpect(jsonPath("$.totalAmount").value(2500.00))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productName").value("Notebook Dell"));
    }

    @Test
    public void testOrderPersistenceAcrossRequests() throws Exception {
        // Create first order
        Map<String, Object> order1 = Map.of(
            "customerName", "Cliente 1",
            "items", List.of(Map.of(
                "productName", "Produto A",
                "price", 100.00,
                "quantity", 2
            )),
            "totalAmount", 200.00
        );

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order1)))
                .andExpect(status().isCreated());

        // Create second order
        Map<String, Object> order2 = Map.of(
            "customerName", "Cliente 2", 
            "items", List.of(Map.of(
                "productName", "Produto B",
                "price", 150.00,
                "quantity", 1
            )),
            "totalAmount", 150.00
        );

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order2)))
                .andExpect(status().isCreated());

        // Verify both orders exist
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[?(@.customerName == 'Cliente 1')]").exists())
                .andExpect(jsonPath("$[?(@.customerName == 'Cliente 2')]").exists());
    }

    @Test
    public void testInvalidOrderValidation() throws Exception {
        // Test missing customer name
        Map<String, Object> invalidOrder1 = Map.of(
            "items", List.of(Map.of(
                "productName", "Produto",
                "price", 100.00,
                "quantity", 1
            ))
        );

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidOrder1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"));

        // Test empty items
        Map<String, Object> invalidOrder2 = Map.of(
            "customerName", "Cliente",
            "items", List.of()
        );

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidOrder2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"));

        // Test invalid price
        Map<String, Object> invalidOrder3 = Map.of(
            "customerName", "Cliente",
            "items", List.of(Map.of(
                "productName", "Produto",
                "price", -100.00,
                "quantity", 1
            ))
        );

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidOrder3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"));
    }

    @Test
    public void testOrderNotFound() throws Exception {
        String nonExistentOrderId = "non-existent-order-id";
        
        mockMvc.perform(get("/api/orders/" + nonExistentOrderId))
                .andExpect(status().isNotFound());
    }
}