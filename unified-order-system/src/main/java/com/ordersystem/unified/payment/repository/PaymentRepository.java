package com.ordersystem.unified.payment.repository;

import com.ordersystem.unified.payment.model.Payment;
import com.ordersystem.unified.shared.events.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * Find payment by order ID.
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Find all payments by order ID (in case of multiple payment attempts).
     */
    List<Payment> findAllByOrderId(String orderId);

    /**
     * Find payments by status.
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find payments by transaction ID.
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Find payments by correlation ID for tracing.
     */
    List<Payment> findByCorrelationId(String correlationId);

    /**
     * Find payments processed within a date range.
     */
    @Query("SELECT p FROM Payment p WHERE p.processedAt BETWEEN :startDate AND :endDate ORDER BY p.processedAt DESC")
    List<Payment> findPaymentsProcessedBetween(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Count payments by status.
     */
    long countByStatus(PaymentStatus status);

    /**
     * Find payments by payment method.
     */
    List<Payment> findByPaymentMethod(String paymentMethod);

    /**
     * Find failed payments with specific error codes.
     */
    List<Payment> findByStatusAndErrorCode(PaymentStatus status, String errorCode);

    /**
     * Calculate total amount of successful payments.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal getTotalSuccessfulPaymentAmount();

    /**
     * Calculate total amount of successful payments for a specific date range.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSuccessfulPaymentAmountBetween(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find recent payments (last N payments).
     */
    @Query("SELECT p FROM Payment p ORDER BY p.processedAt DESC")
    List<Payment> findRecentPayments(org.springframework.data.domain.Pageable pageable);

    /**
     * Find pending payments older than specified time.
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.processedAt < :cutoffTime")
    List<Payment> findStalePayments(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Check if a payment exists for an order.
     */
    boolean existsByOrderId(String orderId);

    /**
     * Check if a successful payment exists for an order.
     */
    boolean existsByOrderIdAndStatus(String orderId, PaymentStatus status);
}