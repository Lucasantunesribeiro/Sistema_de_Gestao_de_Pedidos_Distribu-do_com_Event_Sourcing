package com.ordersystem.inventory.service;

import com.ordersystem.inventory.model.InventoryItem;
import com.ordersystem.inventory.model.StockReservation;
import com.ordersystem.inventory.repository.InventoryRepository;
import com.ordersystem.inventory.repository.StockReservationRepository;
import com.ordersystem.shared.events.InventoryReservationCommand;
import com.ordersystem.shared.events.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Inventory Service
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private StockAllocationService allocationService;

    @Mock
    private InventoryEventPublisher eventPublisher;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem testProduct;
    private OrderItem testOrderItem;
    private InventoryReservationCommand testCommand;

    @BeforeEach
    void setUp() {
        testProduct = new InventoryItem("product-1", "Test Product", 10, 10.00, "A1");
        testOrderItem = new OrderItem("product-1", "Test Product", 5, java.math.BigDecimal.valueOf(10.00));
        
        List<OrderItem> items = Arrays.asList(testOrderItem);
        testCommand = new InventoryReservationCommand("order-123", "customer-456", items);
    }

    @Test
    void shouldSuccessfullyReserveInventory() {
        // Given
        StockAllocationService.AllocationResult successResult = 
            new StockAllocationService.AllocationResult(true, Arrays.asList(), Arrays.asList());
        
        when(allocationService.allocateStock(any(), any(), any()))
            .thenReturn(successResult);

        // When
        inventoryService.reserveInventory(testCommand);

        // Then
        verify(allocationService).allocateStock(
            eq(testCommand.getItems()), 
            eq(testCommand.getOrderId()), 
            eq(StockAllocationService.AllocationStrategy.FIFO)
        );
        verify(reservationRepository).save(any(StockReservation.class));
        verify(eventPublisher).publishInventoryReserved(
            eq(testCommand.getOrderId()),
            eq(testCommand.getCustomerId()),
            eq(testCommand.getItems()),
            anyString()
        );
    }

    @Test
    void shouldHandleInsufficientStock() {
        // Given
        List<String> failures = Arrays.asList("Insufficient stock for product-1");
        StockAllocationService.AllocationResult failureResult = 
            new StockAllocationService.AllocationResult(false, Arrays.asList(), failures);
        
        when(allocationService.allocateStock(any(), any(), any()))
            .thenReturn(failureResult);

        // When
        inventoryService.reserveInventory(testCommand);

        // Then
        verify(allocationService).allocateStock(any(), any(), any());
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher).publishInventoryReservationFailed(
            eq(testCommand.getOrderId()),
            eq(testCommand.getCustomerId()),
            eq(testCommand.getItems()),
            anyString()
        );
    }

    @Test
    void shouldCheckStockAvailability() {
        // Given
        when(allocationService.checkAvailability(any())).thenReturn(true);

        // When
        boolean available = inventoryService.checkStockAvailability(Arrays.asList(testOrderItem));

        // Then
        assertTrue(available);
        verify(allocationService).checkAvailability(Arrays.asList(testOrderItem));
    }

    @Test
    void shouldGetInventoryItem() {
        // Given
        when(inventoryRepository.findByProductId("product-1")).thenReturn(testProduct);

        // When
        InventoryItem result = inventoryService.getInventoryItem("product-1");

        // Then
        assertNotNull(result);
        assertEquals("product-1", result.getProductId());
        assertEquals("Test Product", result.getProductName());
        verify(inventoryRepository).findByProductId("product-1");
    }

    @Test
    void shouldReturnNullForNonExistentProduct() {
        // Given
        when(inventoryRepository.findByProductId("nonexistent")).thenReturn(null);

        // When
        InventoryItem result = inventoryService.getInventoryItem("nonexistent");

        // Then
        assertNull(result);
        verify(inventoryRepository).findByProductId("nonexistent");
    }
}