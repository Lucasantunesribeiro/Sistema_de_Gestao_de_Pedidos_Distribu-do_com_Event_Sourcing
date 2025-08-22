package com.ordersystem.payment.repository;

import com.ordersystem.payment.model.Payment;
import com.ordersystem.shared.events.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * Encontra um pagamento pelo ID do pedido
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Verifica se existe um pagamento para o pedido
     */
    boolean existsByOrderId(String orderId);

    /**
     * Encontra pagamentos por status
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Encontra pagamentos com múltiplos status
     */
    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    /**
     * Conta pagamentos por status
     */
    long countByStatus(PaymentStatus status);

    /**
     * Encontra pagamentos que podem ser reprocessados
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' OR (p.status IN ('FAILED', 'DECLINED') AND p.retryCount < 3)")
    List<Payment> findProcessablePayments();

    /**
     * Encontra pagamentos que podem ser retentados
     */
    @Query("SELECT p FROM Payment p WHERE p.status IN ('FAILED', 'DECLINED') AND p.retryCount < 3")
    List<Payment> findRetryablePayments();

    /**
     * Encontra pagamentos por correlation ID
     */
    Optional<Payment> findByCorrelationId(String correlationId);

    /**
     * Encontra pagamentos por gateway transaction ID
     */
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    /**
     * Encontra pagamentos pendentes mais antigos que X minutos
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < CURRENT_TIMESTAMP - INTERVAL :minutes MINUTE")
    List<Payment> findPendingPaymentsOlderThan(@Param("minutes") int minutes);

    /**
     * Encontra pagamentos em processamento há mais de X minutos
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PROCESSING' AND p.updatedAt < CURRENT_TIMESTAMP - INTERVAL :minutes MINUTE")
    List<Payment> findStuckProcessingPayments(@Param("minutes") int minutes);
}