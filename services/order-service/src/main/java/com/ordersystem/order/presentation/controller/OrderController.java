package com.ordersystem.order.presentation.controller;

import com.ordersystem.common.observability.CorrelationIdFilter;
import com.ordersystem.common.security.RateLimiterService;
import com.ordersystem.order.application.usecase.CreateOrderCommand;
import com.ordersystem.order.application.usecase.CreateOrderUseCase;
import com.ordersystem.order.domain.OrderItem;
import com.ordersystem.order.presentation.dto.CreateOrderRequest;
import com.ordersystem.order.presentation.dto.OrderResponse;
import com.ordersystem.order.application.port.OrderRepositoryPort;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final RateLimiterService rateLimiterService;
    private final OrderRepositoryPort repository;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           RateLimiterService rateLimiterService,
                           OrderRepositoryPort repository) {
        this.createOrderUseCase = createOrderUseCase;
        this.rateLimiterService = rateLimiterService;
        this.repository = repository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORDER_USER')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        if (!rateLimiterService.tryConsume(CorrelationIdFilter.CORRELATION_ID)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        CreateOrderCommand command = new CreateOrderCommand(
                request.customerId(),
                request.items().stream()
                        .map(item -> new OrderItem(item.productId(), item.quantity(), item.price()))
                        .collect(Collectors.toList())
        );
        var result = createOrderUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.fromDomain(result));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return repository.findById(orderId)
                .map(order -> ResponseEntity.ok(OrderResponse.fromDomain(order)))
                .orElse(ResponseEntity.notFound().build());
    }
}
