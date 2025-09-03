package com.ordersystem.unified.inventory;

import com.ordersystem.unified.inventory.dto.ReservationResponse;
import com.ordersystem.unified.shared.events.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InventoryService.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks
    private InventoryService inventoryService;

    private List<OrderItem> orderItems;

    @BeforeEach
    void setUp() {
        // Setup order items
        OrderItem item1 = new OrderItem();
        item1.setProductId("product-1");
        item1.setProductName("Product 1");
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("25.00"));

        OrderItem item2 = new OrderItem();
        item2.setProductId("product-2");
        item2.setProductName("Product 2");
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("50.00"));

        orderItems = Arrays.asList(item1, item2);
    }

    @Test
    void shouldReserveItemsSuccessfully() {
        // When
        ReservationResponse result = inventoryService.reserveItems(orderItems);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReservationId()).isNotNull();
    }

    @Test
    void shouldReleaseItemsSuccessfully() {
        // Given
        ReservationResponse reservationResponse = inventoryService.reserveItems(orderItems);
        String reservationId = reservationResponse.getReservationId();

        // When
        inventoryService.releaseItems(orderItems, reservationId);

        // Then - No exception should be thrown
        assertThat(true).isTrue(); // Simple assertion to verify method completes
    }

    @Test
    void shouldConfirmReservationSuccessfully() {
        // Given
        ReservationResponse reservationResponse = inventoryService.reserveItems(orderItems);
        String reservationId = reservationResponse.getReservationId();

        // When
        inventoryService.confirmReservation(orderItems, reservationId);

        // Then - No exception should be thrown
        assertThat(true).isTrue(); // Simple assertion to verify method completes
    }

    @Test
    void shouldGetAvailableQuantity() {
        // Given
        String productId = "product-1";

        // When
        Integer result = inventoryService.getAvailableQuantity(productId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldCheckSufficientQuantity() {
        // Given
        String productId = "product-1";
        Integer requiredQuantity = 5;

        // When
        Boolean result = inventoryService.hasSufficientQuantity(productId, requiredQuantity);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void shouldGetLowStockProducts() {
        // When
        List<String> result = inventoryService.getLowStockProducts();

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void shouldGetOutOfStockProducts() {
        // When
        List<String> result = inventoryService.getOutOfStockProducts();

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void shouldAddOrUpdateProduct() {
        // Given
        String productId = "new-product";
        String productName = "New Product";
        Integer quantity = 20;

        // When
        inventoryService.addOrUpdateProduct(productId, productName, quantity);

        // Then - No exception should be thrown
        assertThat(true).isTrue(); // Simple assertion to verify method completes
    }
}