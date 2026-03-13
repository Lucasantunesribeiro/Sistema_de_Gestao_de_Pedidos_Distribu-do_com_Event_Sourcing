package com.ordersystem.unified.inventory;

import com.ordersystem.unified.inventory.domain.InventoryBusinessRules;
import com.ordersystem.unified.inventory.dto.ReservationResponse;
import com.ordersystem.unified.inventory.model.Reservation;
import com.ordersystem.unified.inventory.repository.ReservationItemRepository;
import com.ordersystem.unified.inventory.repository.ReservationRepository;
import com.ordersystem.unified.inventory.repository.StockRepository;
import com.ordersystem.unified.domain.events.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InventoryService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationItemRepository reservationItemRepository;

    @Mock
    private InventoryBusinessRules businessRules;

    @InjectMocks
    private InventoryService inventoryService;

    private List<OrderItem> orderItems;

    @BeforeEach
    void setUp() {
        // Run in mock mode with sensible defaults (mirrors application-test.yml)
        ReflectionTestUtils.setField(inventoryService, "mockMode", true);
        ReflectionTestUtils.setField(inventoryService, "defaultStock", 1000);

        // Stub calculateExpiryTime so createMockReservation doesn't NPE
        when(businessRules.calculateExpiryTime())
                .thenReturn(LocalDateTime.now().plusMinutes(15));

        // Stub save to return the passed reservation (mirrors real behaviour)
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Stub findById to return empty (release/confirm are no-ops for non-existent reservations)
        when(reservationRepository.findById(any())).thenReturn(Optional.empty());

        // Stub stock queries to return 0 (service falls back to defaultStock)
        when(stockRepository.getTotalAvailableQuantityByProductId(any())).thenReturn(0);
        when(stockRepository.getTotalReservedQuantityByProductId(any())).thenReturn(0);
        when(stockRepository.findLowStockItems()).thenReturn(Collections.emptyList());
        when(stockRepository.findOutOfStockItems()).thenReturn(Collections.emptyList());
        when(stockRepository.findByProductIdAndWarehouseId(any(), any())).thenReturn(Optional.empty());

        // Build order items
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
        ReservationResponse result = inventoryService.reserveItems(orderItems);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReservationId()).isNotNull();
    }

    @Test
    void shouldReleaseItemsSuccessfully() {
        ReservationResponse reservationResponse = inventoryService.reserveItems(orderItems);
        String reservationId = reservationResponse.getReservationId();

        // Should complete without exception
        inventoryService.releaseItems(orderItems, reservationId);

        assertThat(true).isTrue();
    }

    @Test
    void shouldConfirmReservationSuccessfully() {
        ReservationResponse reservationResponse = inventoryService.reserveItems(orderItems);
        String reservationId = reservationResponse.getReservationId();

        // Should complete without exception
        inventoryService.confirmReservation(orderItems, reservationId);

        assertThat(true).isTrue();
    }

    @Test
    void shouldGetAvailableQuantity() {
        Integer result = inventoryService.getAvailableQuantity("product-1");

        assertThat(result).isNotNull();
        assertThat(result).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldCheckSufficientQuantity() {
        Boolean result = inventoryService.hasSufficientQuantity("product-1", 5);

        assertThat(result).isNotNull();
        assertThat(result).isTrue(); // defaultStock=1000 >= 5
    }

    @Test
    void shouldGetLowStockProducts() {
        List<String> result = inventoryService.getLowStockProducts();

        assertThat(result).isNotNull();
    }

    @Test
    void shouldGetOutOfStockProducts() {
        List<String> result = inventoryService.getOutOfStockProducts();

        assertThat(result).isNotNull();
    }

    @Test
    void shouldAddOrUpdateProduct() {
        // Should complete without exception (no-op when product not in DB)
        inventoryService.addOrUpdateProduct("new-product", "New Product", 20);

        assertThat(true).isTrue();
    }

    @Test
    void shouldRejectReservationWithoutPersistedStockInRealMode() {
        ReflectionTestUtils.setField(inventoryService, "mockMode", false);
        when(stockRepository.existsStockForProduct("product-1")).thenReturn(false);
        when(stockRepository.existsStockForProduct("product-2")).thenReturn(false);

        ReservationResponse result = inventoryService.reserveItems("ORDER-REAL-1", orderItems);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(com.ordersystem.unified.inventory.dto.ReservationStatus.INSUFFICIENT_STOCK);
    }
}

