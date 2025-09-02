package com.ordersystem.unified.payment.model;

import com.ordersystem.unified.shared.events.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Payment entity.
 */
class PaymentTest {

    private Payment payment;
    private String paymentId;
    private String orderId;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        paymentId = "payment-123";
        orderId = "order-456";
        amount = new BigDecimal("100.00");
        payment = new Payment(paymentId, orderId, amount);
    }

    @Test
    void shouldCreatePaymentWithCorrectInitialState() {
        assertThat(payment.getId()).isEqualTo(paymentId);
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(payment.getTransactionId()).isNull();
        assertThat(payment.getFailureReason()).isNull();
        assertThat(payment.getErrorCode()).isNull();
    }

    @Test
    void shouldCreatePaymentWithSpecificPaymentMethod() {
        Payment debitPayment = new Payment(paymentId, orderId, amount, "DEBIT_CARD");

        assertThat(debitPayment.getPaymentMethod()).isEqualTo("DEBIT_CARD");
        assertThat(debitPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void shouldMarkPaymentAsCompleted() {
        String transactionId = "txn-789";

        payment.markAsCompleted(transactionId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        assertThat(payment.getFailureReason()).isNull();
        assertThat(payment.getErrorCode()).isNull();
        assertThat(payment.isCompleted()).isTrue();
        assertThat(payment.isFailed()).isFalse();
        assertThat(payment.isPending()).isFalse();
    }

    @Test
    void shouldMarkPaymentAsFailed() {
        String reason = "Insufficient funds";
        String errorCode = "INSUFFICIENT_FUNDS";

        payment.markAsFailed(reason, errorCode);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo(reason);
        assertThat(payment.getErrorCode()).isEqualTo(errorCode);
        assertThat(payment.getTransactionId()).isNull();
        assertThat(payment.isCompleted()).isFalse();
        assertThat(payment.isFailed()).isTrue();
        assertThat(payment.isPending()).isFalse();
    }

    @Test
    void shouldMarkPaymentAsPending() {
        // First mark as failed
        payment.markAsFailed("Test failure", "TEST_ERROR");
        
        // Then mark as pending
        payment.markAsPending();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getFailureReason()).isNull();
        assertThat(payment.getErrorCode()).isNull();
        assertThat(payment.isCompleted()).isFalse();
        assertThat(payment.isFailed()).isFalse();
        assertThat(payment.isPending()).isTrue();
    }

    @Test
    void shouldCorrectlyIdentifyPaymentStatus() {
        // Test pending status
        assertThat(payment.isPending()).isTrue();
        assertThat(payment.isCompleted()).isFalse();
        assertThat(payment.isFailed()).isFalse();

        // Test completed status
        payment.markAsCompleted("txn-123");
        assertThat(payment.isPending()).isFalse();
        assertThat(payment.isCompleted()).isTrue();
        assertThat(payment.isFailed()).isFalse();

        // Test failed status
        payment.markAsFailed("Error", "ERROR_CODE");
        assertThat(payment.isPending()).isFalse();
        assertThat(payment.isCompleted()).isFalse();
        assertThat(payment.isFailed()).isTrue();
    }

    @Test
    void shouldImplementEqualsAndHashCodeCorrectly() {
        Payment payment1 = new Payment("payment-1", "order-1", new BigDecimal("50.00"));
        Payment payment2 = new Payment("payment-1", "order-2", new BigDecimal("100.00"));
        Payment payment3 = new Payment("payment-2", "order-1", new BigDecimal("50.00"));

        assertThat(payment1).isEqualTo(payment2); // Same ID
        assertThat(payment1).isNotEqualTo(payment3); // Different ID
        assertThat(payment1.hashCode()).isEqualTo(payment2.hashCode());
    }

    @Test
    void shouldGenerateCorrectToString() {
        payment.markAsCompleted("txn-123");
        String toString = payment.toString();

        assertThat(toString).contains(paymentId);
        assertThat(toString).contains(orderId);
        assertThat(toString).contains("100.00");
        assertThat(toString).contains("COMPLETED");
        assertThat(toString).contains("txn-123");
    }

    @Test
    void shouldHandleNullTransactionIdInCompleted() {
        payment.markAsCompleted(null);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getTransactionId()).isNull();
        assertThat(payment.isCompleted()).isTrue();
    }

    @Test
    void shouldHandleNullValuesInFailed() {
        payment.markAsFailed(null, null);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isNull();
        assertThat(payment.getErrorCode()).isNull();
        assertThat(payment.isFailed()).isTrue();
    }

    @Test
    void shouldSetCorrelationId() {
        String correlationId = "corr-123";
        payment.setCorrelationId(correlationId);

        assertThat(payment.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    void shouldAllowStatusChange() {
        // Start as pending
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        // Change to completed
        payment.setStatus(PaymentStatus.COMPLETED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // Change to failed
        payment.setStatus(PaymentStatus.FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }
}