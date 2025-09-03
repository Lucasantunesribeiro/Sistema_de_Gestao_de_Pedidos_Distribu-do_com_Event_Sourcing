package com.ordersystem.unified.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.inventory.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Inventory Service
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class InventoryServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ReservationRequest validReservationRequest;
    private String createdReservationId;

    @BeforeEach
    void setUp() {
        // Create a valid reservation request for testing
        validReservationRequest = new ReservationRequest();
        validReservationRequest.setOrderId("ORDER-INV-12345");
        validReservationRequest.setCorrelationId("test-inventory-correlation");
        validReservationRequest.setReservationTimeout(Duration.ofMinutes(15));

        ItemReservation item1 = new ItemReservation();
        item1.setProductId("PROD-INV-001");
        item1.setQuantity(2);

        ItemReservation item2 = new ItemReservation();
        item2.setProductId("PROD-INV-002");
        item2.setQuantity(1);

        validReservationRequest.setItems(List.of(item1, item2));
    }

    @Test
    @Order(1)
    void testAddStockFirst() throws Exception {
        // Add stock for products before testing reservations
        Map<String, Object> stockRequest1 = Map.of(
            "productId", "PROD-INV-001",
            "quantity", 100,
            "warehouseId", "DEFAULT"
        );

        Map<String, Object> stockRequest2 = Map.of(
            "productId", "PROD-INV-002",
            "quantity", 50,
            "warehouseId", "DEFAULT"
        );

        mockMvc.perform(post("/api/inventory/stock/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockRequest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Stock added successfully"));

        mockMvc.perform(post("/api/inventory/stock/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockRequest2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(2)
    void testSuccessfulInventoryReservation() throws Exception {
        // First add stock
        testAddStockFirst();

        MvcResult result = mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validReservationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").exists())
                .andExpect(jsonPath("$.orderId").value("ORDER-INV-12345"))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.itemResults").isArray())
                .andExpect(jsonPath("$.itemResults", hasSize(2)))
                .andExpect(jsonPath("$.reservationExpiry").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ReservationResponse reservationResponse = objectMapper.readValue(responseContent, ReservationResponse.class);
        createdReservationId = reservationResponse.getReservationId();
        
        assertNotNull(createdReservationId);
        assertTrue(reservationResponse.isSuccess());
        assertEquals(2, reservationResponse.getItemResults().size());
    }

    @Test
    @Order(3)
    void testReservationStatusRetrieval() throws Exception {
        // First create a reservation
        testSuccessfulInventoryReservation();
        
        // Then retrieve its status
        mockMvc.perform(get("/api/inventory/reservation/{reservationId}", createdReservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(createdReservationId))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.itemResults").isArray());
    }

    @Test
    @Order(4)
    void testReservationConfirmation() throws Exception {
        // First create a reservation
        testSuccessfulInventoryReservation();
        
        // Then confirm it
        mockMvc.perform(post("/api/inventory/confirm/{reservationId}", createdReservationId)
                .param("correlationId", "test-confirmation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reservation confirmed successfully"));
    }

    @Test
    @Order(5)
    void testReservationRelease() throws Exception {
        // First create a reservation
        testSuccessfulInventoryReservation();
        
        // Then release it
        mockMvc.perform(post("/api/inventory/release/{reservationId}", createdReservationId)
                .param("correlationId", "test-release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reservation released successfully"));
    }

    @Test
    @Order(6)
    void testInsufficientStockReservation() throws Exception {
        // Try to reserve more items than available
        ReservationRequest largeRequest = new ReservationRequest();
        largeRequest.setOrderId("ORDER-LARGE-INV");
        largeRequest.setCorrelationId("test-large-correlation");

        ItemReservation largeItem = new ItemReservation();
        largeItem.setProductId("PROD-INV-001");
        largeItem.setQuantity(10000); // Very large quantity

        largeRequest.setItems(List.of(largeItem));

        mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value(containsString("insufficient stock")));
    }

    @Test
    @Order(7)
    void testProductAvailabilityCheck() throws Exception {
        // Add stock first
        testAddStockFirst();

        // Check availability for existing product
        mockMvc.perform(get("/api/inventory/check/{productId}", "PROD-INV-001")
                .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("PROD-INV-001"))
                .andExpected(jsonPath("$.requestedQuantity").value(5))
                .andExpect(jsonPath("$.available").value(true));

        // Check availability for non-existent product
        mockMvc.perform(get("/api/inventory/check/{productId}", "NON-EXISTENT-PRODUCT")
                .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @Order(8)
    void testInventoryStatus() throws Exception {
        // Add stock first
        testAddStockFirst();

        mockMvc.perform(get("/api/inventory/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").exists())
                .andExpect(jsonPath("$.totalProducts").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Order(9)
    void testLegacyInventoryReservation() throws Exception {
        // Add stock first
        testAddStockFirst();

        Map<String, Object> legacyRequest = Map.of(
            "orderId", "ORDER-LEGACY-INV",
            "items", Map.of(
                "PROD-INV-001", 1,
                "PROD-INV-002", 2
            ),
            "correlationId", "legacy-inventory-correlation"
        );

        mockMvc.perform(post("/api/inventory/reserve-legacy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(legacyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.reservationId").exists());
    }

    @Test
    @Order(10)
    void testPartialReservation() throws Exception {
        // Add limited stock
        Map<String, Object> limitedStockRequest = Map.of(
            "productId", "PROD-LIMITED",
            "quantity", 3, // Only 3 items available
            "warehouseId", "DEFAULT"
        );

        mockMvc.perform(post("/api/inventory/stock/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(limitedStockRequest)))
                .andExpect(status().isOk());

        // Try to reserve more than available
        ReservationRequest partialRequest = new ReservationRequest();
        partialRequest.setOrderId("ORDER-PARTIAL");
        partialRequest.setCorrelationId("test-partial-correlation");

        ItemReservation partialItem = new ItemReservation();
        partialItem.setProductId("PROD-LIMITED");
        partialItem.setQuantity(5); // Request more than available

        partialRequest.setItems(List.of(partialItem));

        mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @Order(11)
    void testMultiWarehouseAvailability() throws Exception {
        // Add stock to different warehouses
        Map<String, Object> warehouse1Stock = Map.of(
            "productId", "PROD-MULTI-WH",
            "quantity", 10,
            "warehouseId", "WAREHOUSE-1"
        );

        Map<String, Object> warehouse2Stock = Map.of(
            "productId", "PROD-MULTI-WH",
            "quantity", 15,
            "warehouseId", "WAREHOUSE-2"
        );

        mockMvc.perform(post("/api/inventory/stock/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warehouse1Stock)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventory/stock/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warehouse2Stock)))
                .andExpect(status().isOk());

        // Check availability in specific warehouse
        mockMvc.perform(get("/api/inventory/check/{productId}", "PROD-MULTI-WH")
                .param("quantity", "12")
                .param("warehouseId", "WAREHOUSE-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.warehouseId").value("WAREHOUSE-2"));
    }

    @Test
    @Order(12)
    void testInventoryHealthCheck() throws Exception {
        mockMvc.perform(get("/api/inventory/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("inventory-service"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("2.0"))
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.features", hasItem("inventory-reservation")))
                .andExpect(jsonPath("$.features", hasItem("multi-warehouse-support")));
    }

    @Test
    @Order(13)
    void testReservationNotFound() throws Exception {
        mockMvc.perform(get("/api/inventory/reservation/{reservationId}", "NON-EXISTENT-RESERVATION"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(14)
    void testInvalidReservationRequest() throws Exception {
        // Test with empty items list
        ReservationRequest invalidRequest = new ReservationRequest();
        invalidRequest.setOrderId("ORDER-INVALID");
        invalidRequest.setItems(List.of()); // Empty items list

        mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("At least one item is required")));
    }
}