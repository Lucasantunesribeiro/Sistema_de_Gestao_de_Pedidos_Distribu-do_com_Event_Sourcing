package com.ordersystem.unified.inventory.repository;

import com.ordersystem.unified.inventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Product repository for database operations
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    
    /**
     * Find product by SKU
     */
    Optional<Product> findBySku(String sku);
    
    /**
     * Find active products
     */
    List<Product> findByActiveTrue();
    
    /**
     * Find products by category
     */
    List<Product> findByCategoryAndActiveTrue(String category);
    
    /**
     * Find products by name containing (case insensitive)
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.active = true")
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(@Param("name") String name);
    
    /**
     * Find products with low stock
     */
    @Query("SELECT DISTINCT p FROM Product p JOIN p.stocks s WHERE s.availableQuantity <= s.minimumStock AND p.active = true")
    List<Product> findProductsWithLowStock();
    
    /**
     * Find products that need reorder
     */
    @Query("SELECT DISTINCT p FROM Product p JOIN p.stocks s WHERE s.availableQuantity <= s.reorderPoint AND p.active = true")
    List<Product> findProductsNeedingReorder();
    
    /**
     * Check if SKU exists
     */
    boolean existsBySku(String sku);
    
    /**
     * Count active products
     */
    long countByActiveTrue();
    
    /**
     * Find products by category list
     */
    List<Product> findByCategoryInAndActiveTrue(List<String> categories);
    
    /**
     * Find all categories
     */
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.active = true AND p.category IS NOT NULL ORDER BY p.category")
    List<String> findAllCategories();
}