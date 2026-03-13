package com.ordersystem.unified.payment;

import com.ordersystem.unified.payment.dto.PaymentRequest;
import com.ordersystem.unified.payment.dto.PaymentResponse;
import com.ordersystem.unified.payment.model.Payment;
import com.ordersystem.unified.payment.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for Payment operations in the unified order system.
 */
@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
@Tag(name = "Payments", description = "Payment management operations")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    public PaymentController() {
        logger.info("PaymentController initialized");
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    @Operation(summary = "Get all payments", description = "Retrieves all payments in the system")
    public ResponseEntity<List<Payment>> getAllPayments() {
        logger.debug("Getting all payments");
        try {
            List<Payment> payments = paymentRepository.findAll();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Error getting payments: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Retrieves a payment by its unique identifier")
    public ResponseEntity<Payment> getPayment(@PathVariable String paymentId) {
        logger.debug("Getting payment: {}", paymentId);
        Optional<Payment> payment = paymentRepository.findById(paymentId);
        return payment.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payments by order ID", description = "Retrieves all payments for a specific order")
    public ResponseEntity<List<Payment>> getPaymentsByOrder(@PathVariable String orderId) {
        logger.debug("Getting payments for order: {}", orderId);
        try {
            List<Payment> payments = paymentRepository.findAllByOrderId(orderId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Error getting payments for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/process")
    @Operation(summary = "Process payment", description = "Processes a payment request")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        logger.debug("Processing payment for order: {}", request.getOrderId());
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Payment service status", description = "Returns the status of the payment service")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "payment");
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/status/{paymentId}")
    @Operation(summary = "Get payment status by ID")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable String paymentId) {
        logger.debug("Getting payment status: {}", paymentId);
        return paymentRepository.findById(paymentId)
                .map(p -> {
                    com.ordersystem.unified.payment.dto.PaymentStatus status =
                        com.ordersystem.unified.payment.dto.PaymentStatus.valueOf(p.getStatus().name());
                    PaymentResponse r = new PaymentResponse(p.getId(), p.getOrderId(), status,
                        p.getAmount(), p.getTransactionId(), java.time.LocalDateTime.now(), p.getCorrelationId());
                    return ResponseEntity.ok(r);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    @Operation(summary = "Payment service health check")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "payment-service");
        health.put("status", "UP");
        health.put("version", "2.0");
        health.put("features", List.of("payment-processing", "refund-processing", "multi-method-support"));
        return ResponseEntity.ok(health);
    }

    @GetMapping("/methods")
    @Operation(summary = "List supported payment methods")
    public ResponseEntity<Map<String, Object>> getPaymentMethods() {
        Map<String, Object> methods = new HashMap<>();

        Map<String, Object> creditCard = new HashMap<>();
        creditCard.put("displayName", "Credit Card");
        creditCard.put("instantProcessing", true);
        creditCard.put("requiresManualVerification", false);
        methods.put("CREDIT_CARD", creditCard);

        Map<String, Object> pix = new HashMap<>();
        pix.put("displayName", "PIX");
        pix.put("instantProcessing", true);
        pix.put("requiresManualVerification", false);
        methods.put("PIX", pix);

        Map<String, Object> boleto = new HashMap<>();
        boleto.put("displayName", "Boleto Bancário");
        boleto.put("instantProcessing", false);
        boleto.put("requiresManualVerification", true);
        methods.put("BOLETO", boleto);

        return ResponseEntity.ok(methods);
    }

    @PostMapping("/refund")
    @Operation(summary = "Process payment refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> processRefund(@RequestBody Map<String, Object> refundRequest) {
        String paymentId = (String) refundRequest.get("paymentId");
        logger.debug("Processing refund for payment: {}", paymentId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", "REFUNDED");
        result.put("message", "Refund processed successfully");
        result.put("paymentId", paymentId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/process-legacy")
    @Operation(summary = "Legacy payment processing endpoint")
    public ResponseEntity<Map<String, Object>> processLegacyPayment(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        String correlationId = (String) request.getOrDefault("correlationId", "legacy-" + System.currentTimeMillis());
        logger.debug("Processing legacy payment for order: {}", orderId);

        String transactionId = "LEG-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("transactionId", transactionId);
        result.put("orderId", orderId);
        return ResponseEntity.ok(result);
    }
}
