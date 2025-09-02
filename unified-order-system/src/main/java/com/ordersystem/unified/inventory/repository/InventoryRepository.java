package com.ordersystem.unified.inventory.repository;

import com.ordersystem.unified.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Inventory entity operations.
 * Includes pessimistic locking for concurrent inventory operations.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    /**
     * Find inventory by product ID with pessimistic write lock for concurrent updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") String productId);

    /**
     * Find multiple inventory items by product IDs with pessimistic write lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds ORDER BY i.productId")
    List<Inventory> findByProductIdInWithLock(@Param("productIds") List<String> productIds);

    /**
     * Find inventory by product name (case-insensitive).
     */
    List<Inventory> findByProductNameContainingIgnoreCase(String productName);

    /**
     * Find products with low stock (available quantity <= reorder level).
     */
    @Query("SELECT i FROM Inventory i WHERE i.reorderLevel IS NOT NULL AND i.availableQuantity <= i.reorderLevel")
    List<Inventory> findLowStockProducts();

    /**
     * Find products that are out of stock.
     */
    List<Inventory> findByAvailableQuantity(Integer quantity);

    /**
     * Find products that are out of stock (available quantity = 0).
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity = 0")
    List<Inventory> findOutOfStockProducts();

    /**
     * Find products with reserved inventory.
     */
    @Query("SELECT i FROM Inventory i WHERE i.reservedQuantity > 0")
    List<Inventory> findProductsWithReservations();

    /**
     * Find products with available quantity greater than specified amount.
     */
    List<Inventory> findByAvailableQuantityGreaterThan(Integer quantity);

    /**
     * Find products with available quantity less than specified amount.
     */
    List<Inventory> findByAvailableQuantityLessThan(Integer quantity);

    /**
     * Calculate total available quantity across all products.
     */
    @Query("SELECT COALESCE(SUM(i.availableQuantity), 0) FROM Inventory i")
    Long getTotalAvailableQuantity();

    /**
     * Calculate total reserved quantity across all products.
     */
    @Query("SELECT COALESCE(SUM(i.reservedQuantity), 0) FROM Inventory i")
    Long getTotalReservedQuantity();

    /**
     * Count products that are low on stock.
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.reorderLevel IS NOT NULL AND i.availableQuantity <= i.reorderLevel")
    Long countLowStockProducts();

    /**
     * Count products that are out of stock.
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.availableQuantity = 0")
    Long countOutOfStockProducts();

    /**
     * Find products with stock levels between min and max quantities.
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity BETWEEN :minQuantity AND :maxQuantity")
    List<Inventory> findByAvailableQuantityBetween(@Param("minQuantity") Integer minQuantity, 
                                                  @Param("maxQuantity") Integer maxQuantity);

    /**
     * Find products that exceed their maximum stock level.
     */
    @Query("SELECT i FROM Inventory i WHERE i.maxStockLevel IS NOT NULL AND (i.availableQuantity + i.reservedQuantity) > i.maxStockLevel")
    List<Inventory> findOverStockedProducts();

    /**
     * Check if a product exists in inventory.
     */
    boolean existsByProductId(String productId);

    /**
     * Check if a product has sufficient available quantity.
     */
    @Query("SELECT CASE WHEN i.availableQuantity >= :requiredQuantity THEN true ELSE false END FROM Inventory i WHERE i.productId = :productId")
    Boolean hasSufficientQuantity(@Param("productId") String productId, @Param("requiredQuantity") Integer requiredQuantity);

    /**
     * Get available quantity for a specific product.
     */
    @Query("SELECT i.availableQuantity FROM Inventory i WHERE i.productId = :productId")
    Optional<Integer> getAvailableQuantity(@Param("productId") String productId);

    /**
     * Get reserved quantity for a specific product.
     */
    @Query("SELECT i.reservedQuantity FROM Inventory i WHERE i.productId = :productId")
    Optional<Integer> getReservedQuantity(@Param("productId") String productId);

    /**
     * Update available quantity for a product (use with caution - prefer entity methods).
     */
    @Query("UPDATE Inventory i SET i.availableQuantity = :quantity WHERE i.productId = :productId")
    int updateAvailableQuantity(@Param("productId") String productId, @Param("quantity") Integer quantity);

    /**
     * Bulk update reorder levels for multiple products.
     */
    @Query("UPDATE Inventory i SET i.reorderLevel = :reorderLevel WHERE i.productId IN :productIds")
    int updateReorderLevels(@Param("productIds") List<String> productIds, @Param("reorderLevel") Integer reorderLevel);
}