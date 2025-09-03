package com.ordersystem.unified.inventory.repository;

import com.ordersystem.unified.inventory.model.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Reservation item repository for database operations
 */
@Repository
public interface ReservationItemRepository extends JpaRepository<ReservationItem, String> {
    
    /**
     * Find reservation items by reservation ID
     */
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.reservation.id = :reservationId")
    List<ReservationItem> findByReservationId(@Param("reservationId") String reservationId);
    
    /**
     * Find reservation items by product ID
     */
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.product.id = :productId")
    List<ReservationItem> findByProductId(@Param("productId") String productId);
    
    /**
     * Find reservation items by stock ID
     */
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.stock.id = :stockId")
    List<ReservationItem> findByStockId(@Param("stockId") String stockId);
    
    /**
     * Find reservation items with partial reservations
     */
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.reservedQuantity < ri.requestedQuantity AND ri.reservedQuantity > 0")
    List<ReservationItem> findPartialReservations();
    
    /**
     * Find reservation items with full reservations
     */
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.reservedQuantity = ri.requestedQuantity")
    List<ReservationItem> findFullReservations();
    
    /**
     * Find reservation items that failed to reserve
     */
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.reservedQuantity = 0")
    List<ReservationItem> findFailedReservations();
    
    /**
     * Get total reserved quantity for a product
     */
    @Query("SELECT COALESCE(SUM(ri.reservedQuantity), 0) FROM ReservationItem ri WHERE ri.product.id = :productId")
    Integer getTotalReservedQuantityByProduct(@Param("productId") String productId);
    
    /**
     * Get total confirmed quantity for a product
     */
    @Query("SELECT COALESCE(SUM(ri.confirmedQuantity), 0) FROM ReservationItem ri WHERE ri.product.id = :productId")
    Integer getTotalConfirmedQuantityByProduct(@Param("productId") String productId);
    
    /**
     * Find reservation items by order ID (through reservation)
     */
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.reservation.orderId = :orderId")
    List<ReservationItem> findByOrderId(@Param("orderId") String orderId);
    
    /**
     * Find reservation items that can be confirmed
     */
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.reservedQuantity > ri.confirmedQuantity")
    List<ReservationItem> findItemsAvailableForConfirmation();
    
    /**
     * Find reservation items that can be released
     */
    @Query("SELECT ri FROM ReservationItem ri WHERE ri.reservedQuantity > ri.releasedQuantity")
    List<ReservationItem> findItemsAvailableForRelease();
    
    /**
     * Get reservation item statistics
     */
    @Query("SELECT ri.product.id, COUNT(ri), SUM(ri.requestedQuantity), SUM(ri.reservedQuantity), SUM(ri.confirmedQuantity) FROM ReservationItem ri GROUP BY ri.product.id")
    List<Object[]> getReservationItemStatistics();
}