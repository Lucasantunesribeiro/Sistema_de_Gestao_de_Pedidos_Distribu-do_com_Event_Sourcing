package com.ordersystem.unified.inventory;

import com.ordersystem.unified.inventory.model.Inventory;
import com.ordersystem.unified.inventory.repository.InventoryRepository;
import com.ordersystem.unified.shared.events.OrderItem;
import com.ordersystem.unified.shared.exceptions.InsufficientInventoryException;
import com.ordersystem.unified.shared.exceptions.InventoryReservationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryService.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private List<OrderItem> orderItems;
    private List<Inventory> inventoryItems;
    private String correlationId;

    @BeforeEach
    void setUp() {
        correlationId = "corr-123";
        
        // Setup order items
        OrderItem item1 = new OrderItem("product-1", "Product 1", 2, new BigDecimal("25.00"));
        OrderItem item2 = new OrderItem("product-2", "Product 2", 1, new BigDecimal("50.00"));
        orderItems = Arrays.asList(item1, item2);

        // Setup inventory items
        Inventory inventory1 = new Inventory("product-1", "Product 1", 10);
        Inventory inventory2 = new Inventory("product-2", "Product 2", 5);
        inventoryItems = Arrays.asList(inventory1, inventory2);
    }

    @Test
    void shouldReserveItemsSuccessfully() {
        // Given
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenReturn(inventoryItems);
        when(inventoryRepository.saveAll(anyList())).thenReturn(inventoryItems);

        // When
        InventoryResult result = inventoryService.reserveItems(orderItems, correlationId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("reserved successfully");

        verify(inventoryRepository).findByProductIdInWithLock(Arrays.asList("product-1", "product-2"));
        verify(inventoryRepository).saveAll(anyList());
    }

    @Test
    void shouldReserveItemsWithoutCorrelationId() {
        // Given
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenReturn(inventoryItems);
        when(inventoryRepository.saveAll(anyList())).thenReturn(inventoryItems);

        // When
        InventoryResult result = inventoryService.reserveItems(orderItems);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(inventoryRepository).findByProductIdInWithLock(anyList());
    }

    @Test
    void shouldFailReservationWhenProductNotFound() {
        // Given
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenReturn(Arrays.asList(inventoryItems.get(0))); // Only one product found

        // When
        InventoryResult result = inventoryService.reserveItems(orderItems, correlationId);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Insufficient stock");
        verify(inventoryRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldFailReservationWhenInsufficientStock() {
        // Given
        Inventory lowStockInventory = new Inventory("product-1", "Product 1", 1); // Only 1 available, but need 2
        List<Inventory> lowStockItems = Arrays.asList(lowStockInventory, inventoryItems.get(1));
        
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenReturn(lowStockItems);

        // When & Then
        assertThatThrownBy(() -> inventoryService.reserveItems(orderItems, correlationId))
            .isInstanceOf(InsufficientInventoryException.class)
            .hasMessageContaining("Insufficient inventory for product");

        verify(inventoryRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldReleaseItemsSuccessfully() {
        // Given
        // Setup inventory with reserved quantities
        inventoryItems.get(0).reserve(2);
        inventoryItems.get(1).reserve(1);
        
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenReturn(inventoryItems);
        when(inventoryRepository.saveAll(anyList())).thenReturn(inventoryItems);

        // When
        InventoryResult result = inventoryService.releaseItems(orderItems, correlationId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("released successfully");

        verify(inventoryRepository).findByProductIdInWithLock(Arrays.asList("product-1", "product-2"));
        verify(inventoryRepository).saveAll(anyList());
    }

    @Test
    void shouldReleaseItemsWithoutCorrelationId() {
        // Given
        inventoryItems.get(0).reserve(2);
        inventoryItems.get(1).reserve(1);
        
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenReturn(inventoryItems);
        when(inventoryRepository.saveAll(anyList())).thenReturn(inventoryItems);

        // When
        InventoryResult result = inventoryService.releaseItems(orderItems);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(inventoryRepository).findByProductIdInWithLock(anyList());
    }

    @Test
    void shouldHandleReleaseWhenProductNotFound() {
        // Given
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenReturn(Arrays.asList(inventoryItems.get(0))); // Only one product found

        // When
        InventoryResult result = inventoryService.releaseItems(orderItems, correlationId);

        // Then
        assertThat(result.isSuccess()).isTrue(); // Should still succeed for found products
        verify(inventoryRepository).saveAll(anyList());
    }

    @Test
    void shouldConfirmReservationSuccessfully() {
        // Given
        inventoryItems.get(0).reserve(2);
        inventoryItems.get(1).reserve(1);
        
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenReturn(inventoryItems);
        when(inventoryRepository.saveAll(anyList())).thenReturn(inventoryItems);

        // When
        InventoryResult result = inventoryService.confirmReservation(orderItems, correlationId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("reserved successfully");

        verify(inventoryRepository).findByProductIdInWithLock(Arrays.asList("product-1", "product-2"));
        verify(inventoryRepository).saveAll(anyList());
    }

    @Test
    void shouldGetAvailableQuantity() {
        // Given
        String productId = "product-1";
        Integer expectedQuantity = 10;
        when(inventoryRepository.getAvailableQuantity(productId)).thenReturn(Optional.of(expectedQuantity));

        // When
        Integer result = inventoryService.getAvailableQuantity(productId);

        // Then
        assertThat(result).isEqualTo(expectedQuantity);
    }

    @Test
    void shouldReturnZeroWhenProductNotFound() {
        // Given
        String productId = "non-existent";
        when(inventoryRepository.getAvailableQuantity(productId)).thenReturn(Optional.empty());

        // When
        Integer result = inventoryService.getAvailableQuantity(productId);

        // Then
        assertThat(result).isEqualTo(0);
    }

    @Test
    void shouldGetInventory() {
        // Given
        String productId = "product-1";
        Inventory expectedInventory = inventoryItems.get(0);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(expectedInventory));

        // When
        Optional<Inventory> result = inventoryService.getInventory(productId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedInventory);
    }

    @Test
    void shouldCheckSufficientQuantity() {
        // Given
        String productId = "product-1";
        Integer requiredQuantity = 5;
        when(inventoryRepository.hasSufficientQuantity(productId, requiredQuantity)).thenReturn(true);

        // When
        boolean result = inventoryService.hasSufficientQuantity(productId, requiredQuantity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForInsufficientQuantity() {
        // Given
        String productId = "product-1";
        Integer requiredQuantity = 15;
        when(inventoryRepository.hasSufficientQuantity(productId, requiredQuantity)).thenReturn(false);

        // When
        boolean result = inventoryService.hasSufficientQuantity(productId, requiredQuantity);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldGetLowStockProducts() {
        // Given
        List<Inventory> lowStockProducts = Arrays.asList(inventoryItems.get(0));
        when(inventoryRepository.findLowStockProducts()).thenReturn(lowStockProducts);

        // When
        List<Inventory> result = inventoryService.getLowStockProducts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(lowStockProducts);
    }

    @Test
    void shouldGetOutOfStockProducts() {
        // Given
        List<Inventory> outOfStockProducts = Arrays.asList(inventoryItems.get(1));
        when(inventoryRepository.findOutOfStockProducts()).thenReturn(outOfStockProducts);

        // When
        List<Inventory> result = inventoryService.getOutOfStockProducts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(outOfStockProducts);
    }

    @Test
    void shouldAddNewProduct() {
        // Given
        String productId = "new-product";
        String productName = "New Product";
        Integer quantity = 20;
        
        when(inventoryRepository.findById(productId)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Inventory result = inventoryService.addOrUpdateProduct(productId, productName, quantity);

        // Then
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getProductName()).isEqualTo(productName);
        assertThat(result.getAvailableQuantity()).isEqualTo(quantity);
        
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void shouldUpdateExistingProduct() {
        // Given
        String productId = "product-1";
        String productName = "Updated Product";
        Integer additionalQuantity = 15;
        Inventory existingInventory = inventoryItems.get(0);
        Integer originalQuantity = existingInventory.getAvailableQuantity();
        
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Inventory result = inventoryService.addOrUpdateProduct(productId, productName, additionalQuantity);

        // Then
        assertThat(result.getAvailableQuantity()).isEqualTo(originalQuantity + additionalQuantity);
        verify(inventoryRepository).save(existingInventory);
    }

    @Test
    void shouldHandleRepositoryExceptionDuringReservation() {
        // Given
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> inventoryService.reserveItems(orderItems, correlationId))
            .isInstanceOf(InventoryReservationException.class)
            .hasMessageContaining("Inventory reservation failed");
    }

    @Test
    void shouldHandleRepositoryExceptionDuringRelease() {
        // Given
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> inventoryService.releaseItems(orderItems, correlationId))
            .isInstanceOf(InventoryReservationException.class)
            .hasMessageContaining("Inventory release failed");
    }

    @Test
    void shouldHandleRepositoryExceptionDuringConfirmation() {
        // Given
        when(inventoryRepository.findByProductIdInWithLock(anyList())).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> inventoryService.confirmReservation(orderItems, correlationId))
            .isInstanceOf(InventoryReservationException.class)
            .hasMessageContaining("Reservation confirmation failed");
    }
}