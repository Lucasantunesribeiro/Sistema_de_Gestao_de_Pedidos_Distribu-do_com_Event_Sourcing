package com.ordersystem.unified.inventory.repository;

import com.ordersystem.unified.inventory.dto.ReservationStatus;
import com.ordersystem.unified.inventory.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Reservation repository for database operations
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {
    
    /**
     * Find reservation by order ID
     */
    Optional<Reservation> findByOrderId(String orderId);
    
    /**
     * Find reservations by status
     */
    List<Reservation> findByStatus(ReservationStatus status);
    
    /**
     * Find reservations by correlation ID
     */
    List<Reservation> findByCorrelationId(String correlationId);
    
    /**
     * Find reservations by warehouse ID
     */
    List<Reservation> findByWarehouseId(String warehouseId);
    
    /**
     * Find expired reservations that are still active
     */
    @Query("SELECT r FROM Reservation r WHERE r.expiryTime < :currentTime AND r.status IN ('RESERVED', 'PARTIAL')")
    List<Reservation> findExpiredActiveReservations(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find reservations expiring soon
     */
    @Query("SELECT r FROM Reservation r WHERE r.expiryTime BETWEEN :currentTime AND :warningTime AND r.status IN ('RESERVED', 'PARTIAL')")
    List<Reservation> findReservationsExpiringSoon(@Param("currentTime") LocalDateTime currentTime, @Param("warningTime") LocalDateTime warningTime);
    
    /**
     * Find active reservations (not expired and not terminal status)
     */
    @Query("SELECT r FROM Reservation r WHERE r.status IN ('RESERVED', 'PARTIAL') AND r.expiryTime > :currentTime")
    List<Reservation> findActiveReservations(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find reservations created between dates
     */
    List<Reservation> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Count reservations by status
     */
    long countByStatus(ReservationStatus status);
    
    /**
     * Find reservations by status and warehouse
     */
    List<Reservation> findByStatusAndWarehouseId(ReservationStatus status, String warehouseId);
    
    /**
     * Get reservation statistics
     */
    @Query("SELECT r.status, COUNT(r) FROM Reservation r GROUP BY r.status")
    List<Object[]> getReservationStatistics();
    
    /**
     * Find reservations that can be auto-released (expired active reservations)
     */
    @Query("SELECT r FROM Reservation r WHERE r.expiryTime < :currentTime AND r.status IN ('RESERVED', 'PARTIAL') ORDER BY r.expiryTime ASC")
    List<Reservation> findReservationsForAutoRelease(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find recent reservations (last N records)
     */
    @Query("SELECT r FROM Reservation r ORDER BY r.createdAt DESC LIMIT :limit")
    List<Reservation> findRecentReservations(@Param("limit") int limit);
    
    /**
     * Check if order has active reservation
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reservation r WHERE r.orderId = :orderId AND r.status IN ('RESERVED', 'PARTIAL') AND r.expiryTime > :currentTime")
    boolean hasActiveReservationForOrder(@Param("orderId") String orderId, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find reservations by multiple order IDs
     */
    List<Reservation> findByOrderIdIn(List<String> orderIds);
}