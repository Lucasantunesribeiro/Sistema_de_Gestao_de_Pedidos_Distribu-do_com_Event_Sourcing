package com.ordersystem.unified.integration;

import com.ordersystem.unified.config.TestConfig;
import com.ordersystem.unified.payment.PaymentResult;
import com.ordersystem.unified.payment.PaymentService;
import com.ordersystem.unified.payment.model.Payment;
import com.ordersystem.unified.payment.repository.PaymentRepository;
import com.ordersystem.unified.support.PostgresIntegrationTestSupport;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "payment.gateway.enabled=true",
    "app.messaging.outbox.enabled=false"
})
@ActiveProfiles("test")
@Import(TestConfig.class)
class PaymentGatewayIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String API_KEY = "test-api-key";
    private static final AtomicReference<String> chargeResponse = new AtomicReference<>();
    private static final AtomicReference<String> refundResponse = new AtomicReference<>();
    private static final AtomicReference<String> lastApiKey = new AtomicReference<>();
    private static HttpServer gateway;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @AfterAll
    static void stopGateway() {
        if (gateway != null) {
            gateway.stop(0);
        }
    }

    @DynamicPropertySource
    static void registerGateway(DynamicPropertyRegistry registry) {
        ensureGateway();
        registry.add("payment.gateway.base-url", () -> "http://localhost:" + gateway.getAddress().getPort());
        registry.add("payment.gateway.api-key", () -> API_KEY);
    }

    @BeforeEach
    void resetGateway() {
        lastApiKey.set(null);
        chargeResponse.set("""
            {
              "approved": true,
              "status": "COMPLETED",
              "transactionId": "gw-tx-123",
              "providerPaymentId": "gw-pay-123",
              "message": "Gateway approved"
            }
            """);
        refundResponse.set("""
            {
              "approved": true,
              "status": "REFUNDED",
              "transactionId": "gw-ref-456",
              "providerPaymentId": "gw-pay-456",
              "message": "Gateway refunded"
            }
            """);
    }

    @Test
    void shouldProcessPaymentThroughExternalGateway() {
        PaymentResult result = paymentService.processPayment(
            "ORDER-GW-1",
            new BigDecimal("149.90"),
            "corr-gw-1",
            "CREDIT_CARD"
        );

        assertThat(result.isSuccess()).isTrue();
        Payment persisted = paymentRepository.findById(result.getPaymentId()).orElseThrow();
        assertThat(persisted.getTransactionId()).isEqualTo("gw-tx-123");
        assertThat(persisted.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(lastApiKey.get()).isEqualTo(API_KEY);
    }

    @Test
    void shouldRefundPaymentThroughExternalGateway() {
        chargeResponse.set("""
            {
              "approved": true,
              "status": "COMPLETED",
              "transactionId": "gw-tx-456",
              "providerPaymentId": "gw-pay-456",
              "message": "Gateway approved"
            }
            """);

        PaymentResult result = paymentService.processPayment(
            "ORDER-GW-2",
            new BigDecimal("249.90"),
            "corr-gw-2",
            "CREDIT_CARD"
        );

        String refundTransactionId = paymentService.refundPayment(
            result.getPaymentId(),
            "Customer requested refund"
        ).orElseThrow();

        Payment refunded = paymentRepository.findById(result.getPaymentId()).orElseThrow();
        assertThat(refundTransactionId).isEqualTo("gw-ref-456");
        assertThat(refunded.getStatus().name()).isEqualTo("REFUNDED");
        assertThat(refunded.getTransactionId()).isEqualTo("gw-ref-456");
        assertThat(lastApiKey.get()).isEqualTo(API_KEY);
    }

    private static synchronized void ensureGateway() {
        if (gateway != null) {
            return;
        }
        try {
            gateway = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start payment gateway test server", exception);
        }
        gateway.createContext("/payments", exchange -> writeJsonResponse(exchange, chargeResponse.get()));
        gateway.createContext("/refunds", exchange -> writeJsonResponse(exchange, refundResponse.get()));
        gateway.start();
    }

    private static void writeJsonResponse(HttpExchange exchange, String responseBody) throws IOException {
        lastApiKey.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        } finally {
            exchange.close();
        }
    }
}
