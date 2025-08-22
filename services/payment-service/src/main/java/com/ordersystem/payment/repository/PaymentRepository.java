package com.ordersystem.payment.repository;

import com.ordersystem.payment.model.Payment;
import com.ordersystem.shared.events.PaymentStatus;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class PaymentRepository {

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final Map<String, String> orderToPaymentMapping = new ConcurrentHashMap<>();

    public Payment save(Payment payment) {
        payments.put(payment.getPaymentId(), payment);
        orderToPaymentMapping.put(payment.getOrderId(), payment.getPaymentId());
        return payment;
    }

    public Optional<Payment> findById(String paymentId) {
        return Optional.ofNullable(payments.get(paymentId));
    }

    public Optional<Payment> findByOrderId(String orderId) {
        String paymentId = orderToPaymentMapping.get(orderId);
        if (paymentId != null) {
            return findById(paymentId);
        }
        return Optional.empty();
    }

    public List<Payment> findAll() {
        return new ArrayList<>(payments.values());
    }

    public List<Payment> findByStatus(PaymentStatus status) {
        return payments.values().stream()
                .filter(payment -> payment.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Payment> findByStatusIn(List<PaymentStatus> statuses) {
        return payments.values().stream()
                .filter(payment -> statuses.contains(payment.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Payment> findProcessablePayments() {
        return payments.values().stream()
                .filter(Payment::isProcessable)
                .collect(Collectors.toList());
    }

    public List<Payment> findRetryablePayments() {
        return payments.values().stream()
                .filter(Payment::canRetry)
                .collect(Collectors.toList());
    }

    public boolean existsByOrderId(String orderId) {
        return orderToPaymentMapping.containsKey(orderId);
    }

    public void deleteById(String paymentId) {
        Payment payment = payments.remove(paymentId);
        if (payment != null) {
            orderToPaymentMapping.remove(payment.getOrderId());
        }
    }

    public long count() {
        return payments.size();
    }

    public long countByStatus(PaymentStatus status) {
        return payments.values().stream()
                .filter(payment -> payment.getStatus() == status)
                .count();
    }

    // For testing purposes
    public void clear() {
        payments.clear();
        orderToPaymentMapping.clear();
    }
}