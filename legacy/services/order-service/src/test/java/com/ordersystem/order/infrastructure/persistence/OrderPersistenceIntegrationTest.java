package com.ordersystem.order.infrastructure.persistence;

import com.ordersystem.order.application.usecase.CreateOrderCommand;
import com.ordersystem.order.application.usecase.CreateOrderUseCase;
import com.ordersystem.order.application.port.OrderRepositoryPort;
import com.ordersystem.order.domain.Order;
import com.ordersystem.order.domain.OrderItem;
import com.ordersystem.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderPersistenceIntegrationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private OrderRepositoryPort orderRepositoryPort;

    @Test
    void createOrderPersistsAndCanBeLoaded() {
        List<OrderItem> items = List.of(
                new OrderItem("sku-001", 1, new BigDecimal("19.90")),
                new OrderItem("sku-002", 3, new BigDecimal("7.50"))
        );
        CreateOrderCommand command = new CreateOrderCommand("customer-456", items);

        Order created = createOrderUseCase.execute(command);

        Order reloaded = orderRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getCustomerId()).isEqualTo("customer-456");
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(reloaded.getItems()).hasSize(2);
        assertThat(reloaded.getItems().get(0).getProductId()).isEqualTo("sku-001");
    }
}
