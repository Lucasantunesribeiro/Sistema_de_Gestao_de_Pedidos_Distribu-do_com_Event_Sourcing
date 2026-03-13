package com.ordersystem.query.controller;

import com.ordersystem.query.entity.OrderReadModel;
import com.ordersystem.query.service.OrderQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    private final OrderQueryService orderQueryService;

    public OrderQueryController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    public Page<OrderReadModel> getOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return orderQueryService.getOrders(pageable, customerId, status);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderReadModel> getOrderById(@PathVariable String orderId) {
        return orderQueryService.getOrderById(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
