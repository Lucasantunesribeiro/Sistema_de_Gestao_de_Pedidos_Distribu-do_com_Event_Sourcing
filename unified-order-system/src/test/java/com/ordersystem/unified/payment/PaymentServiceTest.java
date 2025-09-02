package com.ordersystem.unified.payment;

import com.ordersystem.unified.payment.model.Payment;
import com.ordersystem.unified.payment.repository.PaymentRepository;
import com.ordersystem.unified.shared.events.PaymentStatus;
import com.ordersystem.unified.shared.exceptions.PaymentProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private String orderId;
    private BigDecimal amount;
    private String correlationId;
    private Payment existingPayment;

    @BeforeEach
    void setUp() {
        orderId = "order-123";
        amount = new BigDecimal("100.00");
        correlationId = "corr-123";
        
        existingPayment = new Payment("payment-123", orderId, amount);
        existingPayment.setCorrelationId(correlationId);
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId("payment-" + System.currentTimeMillis());
            }
            return payment;
        });

        // When
        PaymentResult result = paymentService.processPayment(orderId, amount, correlationId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getPaymentId()).isNotNull();
        assertThat(result.getMessage()).isEqualTo("Payment processed successfully");

        verify(paymentRepository, times(2)).save(any(Payment.class)); // Once for pending, once for completed
    }

    @Test
    void shouldReturnExistingPaymentIfAlreadyCompleted() {
        // Given
        existingPayment.markAsCompleted("txn-123");
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        // When
        PaymentResult result = paymentService.processPayment(orderId, amount, correlationId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPaymentId()).isEqualTo("payment-123");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldProcessPaymentWithSpecificPaymentMethod() {
        // Given
        String paymentMethod = "DEBIT_CARD";
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PaymentResult result = paymentService.processPayment(orderId, amount, correlationId, paymentMethod);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void shouldHandlePaymentProcessingFailure() {
        // Given
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                payment.setId("payment-" + System.currentTimeMillis());
                return payment;
            }) // First save succeeds
            .thenThrow(new RuntimeException("Database error")) // Second save fails
            .thenAnswer(invocation -> invocation.getArgument(0)); // Third save in catch block succeeds

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(orderId, amount, correlationId))
            .isInstanceOf(PaymentProcessingException.class)
            .hasMessageContaining("Payment processing failed");

        verify(paymentRepository, times(3)).save(any(Payment.class)); // First, second (fails), third (in catch)
    }

    @Test
    void shouldRetrievePaymentByOrderId() {
        // Given
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        // When
        Optional<Payment> result = paymentService.getPaymentByOrderId(orderId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(orderId);
        assertThat(result.get().getAmount()).isEqualTo(amount);
    }

    @Test
    void shouldRetrievePaymentById() {
        // Given
        String paymentId = "payment-123";
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(existingPayment));

        // When
        Optional<Payment> result = paymentService.getPaymentById(paymentId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(paymentId);
    }

    @Test
    void shouldRetrieveAllPaymentsByOrderId() {
        // Given
        Payment payment1 = new Payment("payment-1", orderId, amount);
        Payment payment2 = new Payment("payment-2", orderId, amount);
        List<Payment> payments = Arrays.asList(payment1, payment2);
        
        when(paymentRepository.findAllByOrderId(orderId)).thenReturn(payments);

        // When
        List<Payment> result = paymentService.getAllPaymentsByOrderId(orderId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(payment1, payment2);
    }

    @Test
    void shouldCheckIfSuccessfulPaymentExists() {
        // Given
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.COMPLETED))
            .thenReturn(true);

        // When
        boolean result = paymentService.hasSuccessfulPayment(orderId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseIfNoSuccessfulPaymentExists() {
        // Given
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.COMPLETED))
            .thenReturn(false);

        // When
        boolean result = paymentService.hasSuccessfulPayment(orderId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldHandleNullCorrelationId() {
        // Given
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PaymentResult result = paymentService.processPayment(orderId, amount, null);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void shouldHandleEmptyOrderId() {
        // Given
        String emptyOrderId = "";
        when(paymentRepository.findByOrderId(emptyOrderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId("payment-" + System.currentTimeMillis());
            return payment;
        });

        // When
        PaymentResult result = paymentService.processPayment(emptyOrderId, amount, correlationId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(paymentRepository).findByOrderId(emptyOrderId);
        verify(paymentRepository, times(2)).save(any(Payment.class)); // Once for pending, once for completed
    }

    @Test
    void shouldHandleZeroAmount() {
        // Given
        BigDecimal zeroAmount = BigDecimal.ZERO;
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PaymentResult result = paymentService.processPayment(orderId, zeroAmount, correlationId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }
}