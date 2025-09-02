package com.ordersystem.unified.payment;

import com.ordersystem.unified.payment.model.Payment;
import com.ordersystem.unified.payment.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Payment operations in the unified order system.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment management operations")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    public PaymentController() {
        logger.info("PaymentController initialized");
    }

    @Autowired
    private PaymentRepository paymentRepository;

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

    @GetMapping("/status")
    @Operation(summary = "Payment service status", description = "Returns the status of the payment service")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "payment");
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }
}