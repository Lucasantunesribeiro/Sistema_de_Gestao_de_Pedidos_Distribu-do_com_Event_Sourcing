package com.ordersystem.order.controller;

import com.ordersystem.order.model.Order;
import com.ordersystem.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();
    }

    @Test
    void updateStatusReturns200() throws Exception {
        Order order = new Order("customer-1", 100.0);
        order = orderRepository.save(order);

        mockMvc.perform(put("/api/orders/" + order.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStatus").value("PAID"));
    }

    @Test
    void patchStatusReturns200() throws Exception {
        Order order = new Order("customer-1", 100.0);
        order = orderRepository.save(order);

        mockMvc.perform(patch("/api/orders/" + order.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStatus").value("PAID"));
    }

    @Test
    void updateStatusWithInvalidStatusReturns400() throws Exception {
        Order order = new Order("customer-1", 100.0);
        order = orderRepository.save(order);

        mockMvc.perform(put("/api/orders/" + order.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatusForMissingOrderReturns404() throws Exception {
        mockMvc.perform(put("/api/orders/non-existent-id/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found: non-existent-id"));
    }
}
