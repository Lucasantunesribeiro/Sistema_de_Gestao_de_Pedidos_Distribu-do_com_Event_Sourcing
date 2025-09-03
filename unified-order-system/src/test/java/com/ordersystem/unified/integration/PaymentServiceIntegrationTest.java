package com.ordersystem.unified.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordersystem.unified.payment.dto.*;
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

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Payment Service
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class PaymentServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequest validPaymentRequest;
    private String processedPaymentId;

    @BeforeEach
    void setUp() {
        // Create a valid payment request for testing
        validPaymentRequest = new PaymentRequest();
        validPaymentRequest.setOrderId("ORDER-12345");
        validPaymentRequest.setAmount(new BigDecimal("199.99"));
        validPaymentRequest.setCurrency("BRL");
        validPaymentRequest.setMethod(PaymentMethod.CREDIT_CARD);
        validPaymentRequest.setCorrelationId("test-correlation-id");

        CustomerInfo customer = new CustomerInfo();
        customer.setCustomerId("CUST-12345");
        customer.setName("John Doe");
        customer.setEmail("john.doe@example.com");
        validPaymentRequest.setCustomer(customer);
    }

    @Test
    @Order(1)
    void testSuccessfulCreditCardPayment() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.orderId").value("ORDER-12345"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(199.99))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpected(jsonPath("$.message").value("Payment processed successfully"))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        PaymentResponse paymentResponse = objectMapper.readValue(responseContent, PaymentResponse.class);
        processedPaymentId = paymentResponse.getPaymentId();
        
        assertNotNull(processedPaymentId);
        assertNotNull(paymentResponse.getTransactionId());
        assertTrue(paymentResponse.isSuccess());
    }

    @Test
    @Order(2)
    void testPixPayment() throws Exception {
        validPaymentRequest.setMethod(PaymentMethod.PIX);
        validPaymentRequest.setOrderId("ORDER-PIX-123");

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").value(startsWith("PIX-")));
    }

    @Test
    @Order(3)
    void testBoletoPayment() throws Exception {
        validPaymentRequest.setMethod(PaymentMethod.BOLETO);
        validPaymentRequest.setOrderId("ORDER-BOLETO-123");

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.transactionId").value(startsWith("BOL-")))
                .andExpect(jsonPath("$.message").value(containsString("Boleto generated successfully")));
    }

    @Test
    @Order(4)
    void testPaymentStatusRetrieval() throws Exception {
        // First process a payment
        testSuccessfulCreditCardPayment();
        
        // Then retrieve its status
        mockMvc.perform(get("/api/payments/status/{paymentId}", processedPaymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(processedPaymentId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(5)
    void testPaymentRefund() throws Exception {
        // First process a payment
        testSuccessfulCreditCardPayment();
        
        // Then process a refund
        Map<String, Object> refundRequest = Map.of(
            "paymentId", processedPaymentId,
            "refundAmount", 199.99,
            "reason", "Customer requested refund"
        );

        mockMvc.perform(post("/api/payments/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.message").value("Refund processed successfully"));
    }

    @Test
    @Order(6)
    void testInvalidPaymentRequest() throws Exception {
        // Test with invalid amount
        PaymentRequest invalidRequest = new PaymentRequest();
        invalidRequest.setOrderId("ORDER-INVALID");
        invalidRequest.setAmount(new BigDecimal("-10.00")); // Negative amount
        invalidRequest.setMethod(PaymentMethod.CREDIT_CARD);

        CustomerInfo customer = new CustomerInfo();
        customer.setCustomerId("CUST-INVALID");
        customer.setName("Invalid Customer");
        customer.setEmail("invalid@example.com");
        invalidRequest.setCustomer(customer);

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Payment amount must be greater than zero")));
    }

    @Test
    @Order(7)
    void testPaymentsByOrder() throws Exception {
        // Process a payment first
        testSuccessfulCreditCardPayment();
        
        // Then retrieve payments by order ID
        mockMvc.perform(get("/api/payments/order/{orderId}", "ORDER-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].orderId").value("ORDER-12345"));
    }

    @Test
    @Order(8)
    void testPaymentMethods() throws Exception {
        mockMvc.perform(get("/api/payments/methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CREDIT_CARD").exists())
                .andExpect(jsonPath("$.PIX").exists())
                .andExpect(jsonPath("$.BOLETO").exists())
                .andExpect(jsonPath("$.CREDIT_CARD.displayName").value("Credit Card"))
                .andExpect(jsonPath("$.PIX.instantProcessing").value(true))
                .andExpect(jsonPath("$.BOLETO.requiresManualVerification").value(true));
    }

    @Test
    @Order(9)
    void testLegacyPaymentEndpoint() throws Exception {
        Map<String, Object> legacyRequest = Map.of(
            "orderId", "ORDER-LEGACY-123",
            "amount", 99.99,
            "correlationId", "legacy-correlation-id"
        );

        mockMvc.perform(post("/api/payments/process-legacy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(legacyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transactionId").exists());
    }

    @Test
    @Order(10)
    void testPaymentHealthCheck() throws Exception {
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("payment-service"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("2.0"))
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.features", hasItem("payment-processing")))
                .andExpect(jsonPath("$.features", hasItem("refund-processing")));
    }

    @Test
    @Order(11)
    void testHighValuePayment() throws Exception {
        // Test payment with high value to check limits
        validPaymentRequest.setAmount(new BigDecimal("50000.00"));
        validPaymentRequest.setOrderId("ORDER-HIGH-VALUE");

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(50000.00));
    }

    @Test
    @Order(12)
    void testExcessiveAmountPayment() throws Exception {
        // Test payment that exceeds maximum limit
        validPaymentRequest.setAmount(new BigDecimal("200000.00")); // Exceeds limit
        validPaymentRequest.setOrderId("ORDER-EXCESSIVE");

        mockMvc.perform(post("/api/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("exceeds maximum limit")));
    }

    @Test
    @Order(13)
    void testPaymentNotFound() throws Exception {
        mockMvc.perform(get("/api/payments/status/{paymentId}", "NON-EXISTENT-PAYMENT"))
                .andExpect(status().isNotFound());
    }
}