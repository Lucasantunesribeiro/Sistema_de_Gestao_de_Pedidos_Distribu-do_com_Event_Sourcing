package com.ordersystem.order.application;

import com.ordersystem.order.application.port.OrderRepositoryPort;
import com.ordersystem.order.application.usecase.CreateOrderCommand;
import com.ordersystem.order.domain.Order;
import com.ordersystem.order.domain.OrderItem;
import com.ordersystem.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    @Test
    void executeCreatesPendingOrderAndPersists() {
        List<OrderItem> items = List.of(
                new OrderItem("product-1", 2, new BigDecimal("10.50")),
                new OrderItem("product-2", 1, new BigDecimal("25.00"))
        );
        CreateOrderCommand command = new CreateOrderCommand("customer-123", items);

        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderApplicationService.execute(command);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepositoryPort, times(1)).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getCustomerId()).isEqualTo("customer-123");
        assertThat(savedOrder.getItems()).hasSize(2);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getCreatedAt()).isNotNull();
        assertThat(savedOrder.getId()).isNotBlank();
        assertThat(result).isEqualTo(savedOrder);
    }
}
