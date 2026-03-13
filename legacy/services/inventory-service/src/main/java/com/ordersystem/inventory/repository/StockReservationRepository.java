package com.ordersystem.inventory.repository;

import com.ordersystem.inventory.model.StockReservation;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Repository for stock reservations with expiry management
 */
@Repository
public class StockReservationRepository {
    
    private final ConcurrentMap<String, StockReservation> reservations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> orderToReservationMap = new ConcurrentHashMap<>();
    
    public void save(StockReservation reservation) {
        reservations.put(reservation.getReservationId(), reservation);
        orderToReservationMap.put(reservation.getOrderId(), reservation.getReservationId());
    }
    
    public StockReservation findByReservationId(String reservationId) {
        return reservations.get(reservationId);
    }
    
    public StockReservation findByOrderId(String orderId) {
        String reservationId = orderToReservationMap.get(orderId);
        return reservationId != null ? reservations.get(reservationId) : null;
    }
    
    public Collection<StockReservation> findAll() {
        return reservations.values();
    }
    
    public List<StockReservation> findByStatus(StockReservation.ReservationStatus status) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    public List<StockReservation> findExpiredReservations() {
        return reservations.values().stream()
                .filter(StockReservation::isExpired)
                .collect(Collectors.toList());
    }
    
    public List<StockReservation> findByCustomerId(String customerId) {
        return reservations.values().stream()
                .filter(reservation -> customerId.equals(reservation.getCustomerId()))
                .collect(Collectors.toList());
    }
    
    public void delete(String reservationId) {
        StockReservation reservation = reservations.remove(reservationId);
        if (reservation != null) {
            orderToReservationMap.remove(reservation.getOrderId());
        }
    }
    
    public void deleteExpiredReservations() {
        List<StockReservation> expired = findExpiredReservations();
        expired.forEach(reservation -> {
            reservation.expire();
            delete(reservation.getReservationId());
        });
    }
    
    public long countByStatus(StockReservation.ReservationStatus status) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getStatus() == status)
                .count();
    }
    
    public long countExpired() {
        return reservations.values().stream()
                .filter(StockReservation::isExpired)
                .count();
    }
}