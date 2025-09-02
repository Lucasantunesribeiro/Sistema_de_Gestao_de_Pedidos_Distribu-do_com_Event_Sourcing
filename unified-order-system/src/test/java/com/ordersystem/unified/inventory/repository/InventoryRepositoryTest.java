package com.ordersystem.unified.inventory.repository;

import com.ordersystem.unified.inventory.model.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for InventoryRepository.
 */
@DataJpaTest
class InventoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Inventory inventory1;
    private Inventory inventory2;
    private Inventory inventory3;

    @BeforeEach
    void setUp() {
        // Create test inventory items
        inventory1 = new Inventory("product-1", "Product One", 100, 10, 500);
        inventory2 = new Inventory("product-2", "Product Two", 5, 10, 200); // Low stock
        inventory3 = new Inventory("product-3", "Product Three", 0, 5, 100); // Out of stock

        // Add some reserved quantities
        inventory1.reserve(20);
        inventory2.reserve(2);

        // Persist inventory items
        entityManager.persistAndFlush(inventory1);
        entityManager.persistAndFlush(inventory2);
        entityManager.persistAndFlush(inventory3);
    }

    @Test
    void shouldFindByProductIdWithLock() {
        Optional<Inventory> result = inventoryRepository.findByProductIdWithLock("product-1");

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("product-1");
        assertThat(result.get().getAvailableQuantity()).isEqualTo(80); // 100 - 20 reserved
    }

    @Test
    void shouldFindByProductIdInWithLock() {
        List<String> productIds = Arrays.asList("product-1", "product-2");
        
        List<Inventory> results = inventoryRepository.findByProductIdInWithLock(productIds);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Inventory::getProductId).containsExactlyInAnyOrder("product-1", "product-2");
    }

    @Test
    void shouldFindByProductNameContainingIgnoreCase() {
        List<Inventory> results = inventoryRepository.findByProductNameContainingIgnoreCase("product");

        assertThat(results).hasSize(3);
        
        List<Inventory> oneResults = inventoryRepository.findByProductNameContainingIgnoreCase("ONE");
        assertThat(oneResults).hasSize(1);
        assertThat(oneResults.get(0).getProductId()).isEqualTo("product-1");
    }

    @Test
    void shouldFindLowStockProducts() {
        List<Inventory> lowStockProducts = inventoryRepository.findLowStockProducts();

        assertThat(lowStockProducts).hasSize(1);
        assertThat(lowStockProducts.get(0).getProductId()).isEqualTo("product-2");
        assertThat(lowStockProducts.get(0).getAvailableQuantity()).isEqualTo(3); // 5 - 2 reserved
        assertThat(lowStockProducts.get(0).getReorderLevel()).isEqualTo(10);
    }

    @Test
    void shouldFindOutOfStockProducts() {
        List<Inventory> outOfStockProducts = inventoryRepository.findOutOfStockProducts();

        assertThat(outOfStockProducts).hasSize(1);
        assertThat(outOfStockProducts.get(0).getProductId()).isEqualTo("product-3");
        assertThat(outOfStockProducts.get(0).getAvailableQuantity()).isEqualTo(0);
    }

    @Test
    void shouldFindByAvailableQuantity() {
        List<Inventory> zeroQuantityProducts = inventoryRepository.findByAvailableQuantity(0);

        assertThat(zeroQuantityProducts).hasSize(1);
        assertThat(zeroQuantityProducts.get(0).getProductId()).isEqualTo("product-3");
    }

    @Test
    void shouldFindProductsWithReservations() {
        List<Inventory> productsWithReservations = inventoryRepository.findProductsWithReservations();

        assertThat(productsWithReservations).hasSize(2);
        assertThat(productsWithReservations).extracting(Inventory::getProductId)
            .containsExactlyInAnyOrder("product-1", "product-2");
    }

    @Test
    void shouldFindByAvailableQuantityGreaterThan() {
        List<Inventory> results = inventoryRepository.findByAvailableQuantityGreaterThan(10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProductId()).isEqualTo("product-1");
        assertThat(results.get(0).getAvailableQuantity()).isEqualTo(80);
    }

    @Test
    void shouldFindByAvailableQuantityLessThan() {
        List<Inventory> results = inventoryRepository.findByAvailableQuantityLessThan(10);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Inventory::getProductId)
            .containsExactlyInAnyOrder("product-2", "product-3");
    }

    @Test
    void shouldCalculateTotalAvailableQuantity() {
        Long totalAvailable = inventoryRepository.getTotalAvailableQuantity();

        assertThat(totalAvailable).isEqualTo(83L); // 80 + 3 + 0
    }

    @Test
    void shouldCalculateTotalReservedQuantity() {
        Long totalReserved = inventoryRepository.getTotalReservedQuantity();

        assertThat(totalReserved).isEqualTo(22L); // 20 + 2 + 0
    }

    @Test
    void shouldCountLowStockProducts() {
        Long count = inventoryRepository.countLowStockProducts();

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void shouldCountOutOfStockProducts() {
        Long count = inventoryRepository.countOutOfStockProducts();

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void shouldFindByAvailableQuantityBetween() {
        List<Inventory> results = inventoryRepository.findByAvailableQuantityBetween(1, 50);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProductId()).isEqualTo("product-2");
        assertThat(results.get(0).getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    void shouldFindOverStockedProducts() {
        // Create an overstocked product
        Inventory overStockedInventory = new Inventory("product-4", "Overstocked Product", 150, 10, 100);
        entityManager.persistAndFlush(overStockedInventory);

        List<Inventory> overStockedProducts = inventoryRepository.findOverStockedProducts();

        assertThat(overStockedProducts).hasSize(1);
        assertThat(overStockedProducts.get(0).getProductId()).isEqualTo("product-4");
        assertThat(overStockedProducts.get(0).getTotalQuantity()).isEqualTo(150);
        assertThat(overStockedProducts.get(0).getMaxStockLevel()).isEqualTo(100);
    }

    @Test
    void shouldCheckIfProductExists() {
        boolean exists = inventoryRepository.existsByProductId("product-1");
        boolean notExists = inventoryRepository.existsByProductId("non-existent");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldCheckSufficientQuantity() {
        Boolean sufficient = inventoryRepository.hasSufficientQuantity("product-1", 50);
        Boolean insufficient = inventoryRepository.hasSufficientQuantity("product-1", 100);
        Boolean notFound = inventoryRepository.hasSufficientQuantity("non-existent", 10);

        assertThat(sufficient).isTrue();
        assertThat(insufficient).isFalse();
        assertThat(notFound).isNull(); // Product not found
    }

    @Test
    void shouldGetAvailableQuantity() {
        Optional<Integer> quantity = inventoryRepository.getAvailableQuantity("product-1");
        Optional<Integer> notFound = inventoryRepository.getAvailableQuantity("non-existent");

        assertThat(quantity).isPresent();
        assertThat(quantity.get()).isEqualTo(80);
        assertThat(notFound).isEmpty();
    }

    @Test
    void shouldGetReservedQuantity() {
        Optional<Integer> quantity = inventoryRepository.getReservedQuantity("product-1");
        Optional<Integer> notFound = inventoryRepository.getReservedQuantity("non-existent");

        assertThat(quantity).isPresent();
        assertThat(quantity.get()).isEqualTo(20);
        assertThat(notFound).isEmpty();
    }

    @Test
    void shouldUpdateAvailableQuantity() {
        int updatedRows = inventoryRepository.updateAvailableQuantity("product-1", 150);
        entityManager.flush();
        entityManager.clear();

        assertThat(updatedRows).isEqualTo(1);

        Optional<Inventory> updated = inventoryRepository.findById("product-1");
        assertThat(updated).isPresent();
        assertThat(updated.get().getAvailableQuantity()).isEqualTo(150);
    }

    @Test
    void shouldUpdateReorderLevels() {
        List<String> productIds = Arrays.asList("product-1", "product-2");
        int updatedRows = inventoryRepository.updateReorderLevels(productIds, 15);
        entityManager.flush();
        entityManager.clear();

        assertThat(updatedRows).isEqualTo(2);

        Optional<Inventory> updated1 = inventoryRepository.findById("product-1");
        Optional<Inventory> updated2 = inventoryRepository.findById("product-2");
        
        assertThat(updated1).isPresent();
        assertThat(updated1.get().getReorderLevel()).isEqualTo(15);
        assertThat(updated2).isPresent();
        assertThat(updated2.get().getReorderLevel()).isEqualTo(15);
    }

    @Test
    void shouldSaveAndRetrieveInventory() {
        Inventory newInventory = new Inventory("product-4", "New Product", 75, 5, 200);
        newInventory.reserve(10);

        Inventory savedInventory = inventoryRepository.save(newInventory);
        entityManager.flush();
        entityManager.clear();

        Optional<Inventory> retrievedInventory = inventoryRepository.findById("product-4");

        assertThat(retrievedInventory).isPresent();
        assertThat(retrievedInventory.get().getProductName()).isEqualTo("New Product");
        assertThat(retrievedInventory.get().getAvailableQuantity()).isEqualTo(65); // 75 - 10 reserved
        assertThat(retrievedInventory.get().getReservedQuantity()).isEqualTo(10);
        assertThat(retrievedInventory.get().getReorderLevel()).isEqualTo(5);
        assertThat(retrievedInventory.get().getMaxStockLevel()).isEqualTo(200);
    }

    @Test
    void shouldDeleteInventory() {
        inventoryRepository.deleteById("product-1");
        entityManager.flush();

        Optional<Inventory> deletedInventory = inventoryRepository.findById("product-1");
        assertThat(deletedInventory).isEmpty();
    }

    @Test
    void shouldHandleEmptyResults() {
        List<Inventory> emptyResults = inventoryRepository.findByProductNameContainingIgnoreCase("nonexistent");
        assertThat(emptyResults).isEmpty();

        Long zeroCount = inventoryRepository.countLowStockProducts();
        // We know there's 1 low stock product from setup, but if we had none:
        assertThat(zeroCount).isGreaterThanOrEqualTo(0);
    }
}