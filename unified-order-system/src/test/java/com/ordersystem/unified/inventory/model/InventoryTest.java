package com.ordersystem.unified.inventory.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Inventory entity.
 */
class InventoryTest {

    private Inventory inventory;
    private String productId;
    private String productName;
    private Integer initialQuantity;

    @BeforeEach
    void setUp() {
        productId = "product-123";
        productName = "Test Product";
        initialQuantity = 100;
        inventory = new Inventory(productId, productName, initialQuantity);
    }

    @Test
    void shouldCreateInventoryWithCorrectInitialState() {
        assertThat(inventory.getProductId()).isEqualTo(productId);
        assertThat(inventory.getProductName()).isEqualTo(productName);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(initialQuantity);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getTotalQuantity()).isEqualTo(initialQuantity);
    }

    @Test
    void shouldCreateInventoryWithReorderAndMaxLevels() {
        Integer reorderLevel = 10;
        Integer maxStockLevel = 500;
        
        Inventory inventoryWithLevels = new Inventory(productId, productName, initialQuantity, reorderLevel, maxStockLevel);

        assertThat(inventoryWithLevels.getReorderLevel()).isEqualTo(reorderLevel);
        assertThat(inventoryWithLevels.getMaxStockLevel()).isEqualTo(maxStockLevel);
    }

    @Test
    void shouldCheckIfCanReserve() {
        assertThat(inventory.canReserve(50)).isTrue();
        assertThat(inventory.canReserve(100)).isTrue();
        assertThat(inventory.canReserve(101)).isFalse();
        assertThat(inventory.canReserve(0)).isTrue();
    }

    @Test
    void shouldReserveQuantitySuccessfully() {
        Integer reserveQuantity = 30;

        inventory.reserve(reserveQuantity);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
        assertThat(inventory.getReservedQuantity()).isEqualTo(30);
        assertThat(inventory.getTotalQuantity()).isEqualTo(100);
    }

    @Test
    void shouldFailToReserveWhenInsufficientQuantity() {
        Integer reserveQuantity = 150;

        assertThatThrownBy(() -> inventory.reserve(reserveQuantity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot reserve 150 units");
    }

    @Test
    void shouldReleaseReservedQuantity() {
        // First reserve some quantity
        inventory.reserve(40);
        
        // Then release part of it
        inventory.release(20);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(80);
        assertThat(inventory.getReservedQuantity()).isEqualTo(20);
        assertThat(inventory.getTotalQuantity()).isEqualTo(100);
    }

    @Test
    void shouldFailToReleaseWhenInsufficientReserved() {
        inventory.reserve(30);

        assertThatThrownBy(() -> inventory.release(40))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot release 40 units");
    }

    @Test
    void shouldConfirmReservation() {
        // First reserve some quantity
        inventory.reserve(25);
        
        // Then confirm the reservation (items are sold)
        inventory.confirmReservation(15);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(75);
        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
        assertThat(inventory.getTotalQuantity()).isEqualTo(85); // Total reduced as items are sold
    }

    @Test
    void shouldFailToConfirmWhenInsufficientReserved() {
        inventory.reserve(20);

        assertThatThrownBy(() -> inventory.confirmReservation(30))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot confirm 30 units");
    }

    @Test
    void shouldAddStock() {
        inventory.addStock(50);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(150);
        assertThat(inventory.getTotalQuantity()).isEqualTo(150);
    }

    @Test
    void shouldRemoveStock() {
        inventory.removeStock(30);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
        assertThat(inventory.getTotalQuantity()).isEqualTo(70);
    }

    @Test
    void shouldFailToRemoveWhenInsufficientStock() {
        assertThatThrownBy(() -> inventory.removeStock(150))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot remove 150 units");
    }

    @Test
    void shouldCalculateTotalQuantityCorrectly() {
        inventory.reserve(30);
        
        assertThat(inventory.getTotalQuantity()).isEqualTo(100); // 70 available + 30 reserved
        
        inventory.confirmReservation(10);
        
        assertThat(inventory.getTotalQuantity()).isEqualTo(90); // 70 available + 20 reserved
    }

    @Test
    void shouldIdentifyLowStock() {
        inventory.setReorderLevel(20);
        
        assertThat(inventory.isLowStock()).isFalse(); // 100 > 20
        
        inventory.removeStock(85);
        
        assertThat(inventory.isLowStock()).isTrue(); // 15 <= 20
    }

    @Test
    void shouldIdentifyOutOfStock() {
        assertThat(inventory.isOutOfStock()).isFalse();
        
        inventory.removeStock(100);
        
        assertThat(inventory.isOutOfStock()).isTrue();
    }

    @Test
    void shouldIdentifyOverStock() {
        inventory.setMaxStockLevel(80);
        
        assertThat(inventory.isOverStock()).isTrue(); // 100 > 80
        
        inventory.removeStock(30);
        
        assertThat(inventory.isOverStock()).isFalse(); // 70 <= 80
    }

    @Test
    void shouldHandleNullReorderLevel() {
        inventory.setReorderLevel(null);
        
        assertThat(inventory.isLowStock()).isFalse(); // Should not be low stock when reorder level is null
    }

    @Test
    void shouldHandleNullMaxStockLevel() {
        inventory.setMaxStockLevel(null);
        
        assertThat(inventory.isOverStock()).isFalse(); // Should not be over stock when max level is null
    }

    @Test
    void shouldImplementEqualsAndHashCodeCorrectly() {
        Inventory inventory1 = new Inventory("product-1", "Product 1", 50);
        Inventory inventory2 = new Inventory("product-1", "Different Name", 100);
        Inventory inventory3 = new Inventory("product-2", "Product 2", 50);

        assertThat(inventory1).isEqualTo(inventory2); // Same product ID
        assertThat(inventory1).isNotEqualTo(inventory3); // Different product ID
        assertThat(inventory1.hashCode()).isEqualTo(inventory2.hashCode());
    }

    @Test
    void shouldGenerateCorrectToString() {
        inventory.reserve(20);
        String toString = inventory.toString();

        assertThat(toString).contains(productId);
        assertThat(toString).contains(productName);
        assertThat(toString).contains("available=80");
        assertThat(toString).contains("reserved=20");
        assertThat(toString).contains("total=100");
    }

    @Test
    void shouldHandleZeroQuantityOperations() {
        inventory.reserve(0);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);

        inventory.release(0);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);

        inventory.addStock(0);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);

        inventory.removeStock(0);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    void shouldHandleComplexScenario() {
        // Start with 100 available
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);

        // Reserve 30
        inventory.reserve(30);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
        assertThat(inventory.getReservedQuantity()).isEqualTo(30);

        // Add more stock
        inventory.addStock(20);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(90);
        assertThat(inventory.getReservedQuantity()).isEqualTo(30);

        // Confirm part of reservation
        inventory.confirmReservation(20);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(90);
        assertThat(inventory.getReservedQuantity()).isEqualTo(10);

        // Release remaining reservation
        inventory.release(10);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);

        // Total should be 100 (20 items were sold via confirmation)
        assertThat(inventory.getTotalQuantity()).isEqualTo(100);
    }
}