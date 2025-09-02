package com.ordersystem.unified.payment.repository;

import com.ordersystem.unified.payment.model.Payment;
import com.ordersystem.unified.shared.events.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PaymentRepository.
 */
@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment payment1;
    private Payment payment2;
    private Payment payment3;

    @BeforeEach
    void setUp() {
        // Create test payments
        payment1 = new Payment("payment-1", "order-1", new BigDecimal("100.00"));
        payment1.setStatus(PaymentStatus.COMPLETED);
        payment1.setTransactionId("txn-1");
        payment1.setCorrelationId("corr-1");
        payment1.setPaymentMethod("CREDIT_CARD");

        payment2 = new Payment("payment-2", "order-2", new BigDecimal("200.00"));
        payment2.setStatus(PaymentStatus.FAILED);
        payment2.setFailureReason("Insufficient funds");
        payment2.setErrorCode("INSUFFICIENT_FUNDS");
        payment2.setCorrelationId("corr-2");
        payment2.setPaymentMethod("DEBIT_CARD");

        payment3 = new Payment("payment-3", "order-1", new BigDecimal("50.00"));
        payment3.setStatus(PaymentStatus.PENDING);
        payment3.setCorrelationId("corr-3");
        payment3.setPaymentMethod("CREDIT_CARD");

        // Persist payments
        entityManager.persistAndFlush(payment1);
        entityManager.persistAndFlush(payment2);
        entityManager.persistAndFlush(payment3);
    }

    @Test
    void shouldFindPaymentByOrderId() {
        Optional<Payment> result = paymentRepository.findByOrderId("order-2");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("payment-2");
        assertThat(result.get().getOrderId()).isEqualTo("order-2");
    }

    @Test
    void shouldFindAllPaymentsByOrderId() {
        List<Payment> payments = paymentRepository.findAllByOrderId("order-1");

        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(Payment::getId).containsExactlyInAnyOrder("payment-1", "payment-3");
    }

    @Test
    void shouldFindPaymentsByStatus() {
        List<Payment> completedPayments = paymentRepository.findByStatus(PaymentStatus.COMPLETED);
        List<Payment> failedPayments = paymentRepository.findByStatus(PaymentStatus.FAILED);
        List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);

        assertThat(completedPayments).hasSize(1);
        assertThat(completedPayments.get(0).getId()).isEqualTo("payment-1");

        assertThat(failedPayments).hasSize(1);
        assertThat(failedPayments.get(0).getId()).isEqualTo("payment-2");

        assertThat(pendingPayments).hasSize(1);
        assertThat(pendingPayments.get(0).getId()).isEqualTo("payment-3");
    }

    @Test
    void shouldFindPaymentByTransactionId() {
        Optional<Payment> result = paymentRepository.findByTransactionId("txn-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("payment-1");
        assertThat(result.get().getTransactionId()).isEqualTo("txn-1");
    }

    @Test
    void shouldFindPaymentsByCorrelationId() {
        List<Payment> payments = paymentRepository.findByCorrelationId("corr-1");

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getId()).isEqualTo("payment-1");
    }

    @Test
    void shouldCountPaymentsByStatus() {
        long completedCount = paymentRepository.countByStatus(PaymentStatus.COMPLETED);
        long failedCount = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long pendingCount = paymentRepository.countByStatus(PaymentStatus.PENDING);

        assertThat(completedCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(1);
        assertThat(pendingCount).isEqualTo(1);
    }

    @Test
    void shouldFindPaymentsByPaymentMethod() {
        List<Payment> creditCardPayments = paymentRepository.findByPaymentMethod("CREDIT_CARD");
        List<Payment> debitCardPayments = paymentRepository.findByPaymentMethod("DEBIT_CARD");

        assertThat(creditCardPayments).hasSize(2);
        assertThat(creditCardPayments).extracting(Payment::getId).containsExactlyInAnyOrder("payment-1", "payment-3");

        assertThat(debitCardPayments).hasSize(1);
        assertThat(debitCardPayments.get(0).getId()).isEqualTo("payment-2");
    }

    @Test
    void shouldFindFailedPaymentsByErrorCode() {
        List<Payment> payments = paymentRepository.findByStatusAndErrorCode(PaymentStatus.FAILED, "INSUFFICIENT_FUNDS");

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getId()).isEqualTo("payment-2");
        assertThat(payments.get(0).getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void shouldCalculateTotalSuccessfulPaymentAmount() {
        BigDecimal total = paymentRepository.getTotalSuccessfulPaymentAmount();

        assertThat(total).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void shouldCalculateTotalSuccessfulPaymentAmountBetweenDates() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        BigDecimal total = paymentRepository.getTotalSuccessfulPaymentAmountBetween(start, end);

        assertThat(total).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void shouldFindRecentPayments() {
        List<Payment> recentPayments = paymentRepository.findRecentPayments(PageRequest.of(0, 2));

        assertThat(recentPayments).hasSize(2);
        // Payments should be sorted by processed date descending
        assertThat(recentPayments.get(0).getProcessedAt()).isAfterOrEqualTo(recentPayments.get(1).getProcessedAt());
    }

    @Test
    void shouldFindStalePayments() {
        LocalDateTime cutoffTime = LocalDateTime.now().plusMinutes(1);

        List<Payment> stalePayments = paymentRepository.findStalePayments(cutoffTime);

        assertThat(stalePayments).hasSize(1);
        assertThat(stalePayments.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void shouldCheckIfPaymentExistsForOrder() {
        boolean exists = paymentRepository.existsByOrderId("order-1");
        boolean notExists = paymentRepository.existsByOrderId("non-existent-order");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldCheckIfSuccessfulPaymentExistsForOrder() {
        boolean hasSuccessful = paymentRepository.existsByOrderIdAndStatus("order-1", PaymentStatus.COMPLETED);
        boolean hasNoSuccessful = paymentRepository.existsByOrderIdAndStatus("order-2", PaymentStatus.COMPLETED);

        assertThat(hasSuccessful).isTrue();
        assertThat(hasNoSuccessful).isFalse();
    }

    @Test
    void shouldFindPaymentsProcessedBetweenDates() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        List<Payment> payments = paymentRepository.findPaymentsProcessedBetween(start, end);

        assertThat(payments).hasSize(3);
        assertThat(payments).allMatch(payment -> 
            payment.getProcessedAt().isAfter(start) && payment.getProcessedAt().isBefore(end));
    }

    @Test
    void shouldSaveAndRetrievePayment() {
        Payment newPayment = new Payment("payment-4", "order-4", new BigDecimal("300.00"));
        newPayment.setCorrelationId("corr-4");
        newPayment.markAsCompleted("txn-4");

        Payment savedPayment = paymentRepository.save(newPayment);
        entityManager.flush();
        entityManager.clear();

        Optional<Payment> retrievedPayment = paymentRepository.findById("payment-4");

        assertThat(retrievedPayment).isPresent();
        assertThat(retrievedPayment.get().getOrderId()).isEqualTo("order-4");
        assertThat(retrievedPayment.get().getAmount()).isEqualTo(new BigDecimal("300.00"));
        assertThat(retrievedPayment.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(retrievedPayment.get().getTransactionId()).isEqualTo("txn-4");
    }

    @Test
    void shouldDeletePayment() {
        paymentRepository.deleteById("payment-1");
        entityManager.flush();

        Optional<Payment> deletedPayment = paymentRepository.findById("payment-1");
        assertThat(deletedPayment).isEmpty();
    }
}