package com.ordersystem.unified.inventory.repository;

import com.ordersystem.unified.inventory.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Stock repository for database operations with locking support
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, String> {
    
    /**
     * Find stock by product ID and warehouse ID with pessimistic lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId AND s.warehouseId = :warehouseId")
    Optional<Stock> findByProductIdAndWarehouseIdWithLock(@Param("productId") String productId, @Param("warehouseId") String warehouseId);
    
    /**
     * Find stock by product ID and warehouse ID
     */
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId AND s.warehouseId = :warehouseId")
    Optional<Stock> findByProductIdAndWarehouseId(@Param("productId") String productId, @Param("warehouseId") String warehouseId);
    
    /**
     * Find all stocks for a product
     */
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId")
    List<Stock> findByProductId(@Param("productId") String productId);
    
    /**
     * Find stocks by warehouse ID
     */
    List<Stock> findByWarehouseId(String warehouseId);
    
    /**
     * Find stocks with low inventory
     */
    @Query("SELECT s FROM Stock s WHERE s.availableQuantity <= s.minimumStock")
    List<Stock> findLowStockItems();
    
    /**
     * Find stocks that need reorder
     */
    @Query("SELECT s FROM Stock s WHERE s.availableQuantity <= s.reorderPoint")
    List<Stock> findItemsNeedingReorder();
    
    /**
     * Find stocks with available quantity greater than specified amount
     */
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId AND s.availableQuantity >= :quantity")
    List<Stock> findByProductIdWithSufficientStock(@Param("productId") String productId, @Param("quantity") Integer quantity);
    
    /**
     * Get total available quantity for a product across all warehouses
     */
    @Query("SELECT COALESCE(SUM(s.availableQuantity), 0) FROM Stock s WHERE s.product.id = :productId")
    Integer getTotalAvailableQuantityByProductId(@Param("productId") String productId);
    
    /**
     * Get total reserved quantity for a product across all warehouses
     */
    @Query("SELECT COALESCE(SUM(s.reservedQuantity), 0) FROM Stock s WHERE s.product.id = :productId")
    Integer getTotalReservedQuantityByProductId(@Param("productId") String productId);
    
    /**
     * Find stocks with reserved quantity greater than zero
     */
    @Query("SELECT s FROM Stock s WHERE s.reservedQuantity > 0")
    List<Stock> findStocksWithReservations();
    
    /**
     * Find stocks by warehouse with available quantity
     */
    @Query("SELECT s FROM Stock s WHERE s.warehouseId = :warehouseId AND s.availableQuantity > 0")
    List<Stock> findByWarehouseIdWithAvailableStock(@Param("warehouseId") String warehouseId);
    
    /**
     * Check if product has sufficient stock in any warehouse
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Stock s WHERE s.product.id = :productId AND s.availableQuantity >= :quantity")
    boolean hasProductSufficientStock(@Param("productId") String productId, @Param("quantity") Integer quantity);
    
    /**
     * Get stock statistics for dashboard
     */
    @Query("SELECT s.warehouseId, COUNT(s), SUM(s.availableQuantity), SUM(s.reservedQuantity), SUM(s.totalQuantity) FROM Stock s GROUP BY s.warehouseId")
    List<Object[]> getStockStatisticsByWarehouse();
    
    /**
     * Find products with zero stock
     */
    @Query("SELECT s FROM Stock s WHERE s.availableQuantity = 0 AND s.reservedQuantity = 0")
    List<Stock> findOutOfStockItems();
}