package com.ordersystem.query.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordersystem.query.entity.OrderItemReadModel;
import com.ordersystem.query.entity.OrderReadModel;
import com.ordersystem.query.repository.OrderReadModelRepository;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;

@Service
public class OrderQueryService {

    private static final Logger logger = LoggerFactory.getLogger(OrderQueryService.class);

    private final OrderReadModelRepository orderReadModelRepository;

    public OrderQueryService(OrderReadModelRepository orderReadModelRepository) {
        this.orderReadModelRepository = orderReadModelRepository;
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCreated(OrderCreatedEvent event) {
        requireText(event.getOrderId(), "orderId");
        if (orderReadModelRepository.existsById(event.getOrderId())) {
            return;
        }

        String customerId = (event.getCustomerId() == null || event.getCustomerId().isBlank())
                ? "UNKNOWN_CUSTOMER"
                : event.getCustomerId();
        double totalAmount = event.getTotalAmount() == null ? 0.0 : Math.max(0.0, event.getTotalAmount().doubleValue());
        LocalDateTime createdAt = event.getTimestamp() == null ? LocalDateTime.now() : event.getTimestamp();

        OrderReadModel orderReadModel = new OrderReadModel(
                event.getOrderId(),
                customerId,
                "PENDING",
                totalAmount,
                createdAt);

        if (event.getItems() != null) {
            event.getItems().stream()
                    .filter(item -> item != null && item.getProductId() != null && !item.getProductId().isBlank())
                    .forEach(item -> orderReadModel.getItems().add(new OrderItemReadModel(
                            item.getProductId(),
                            item.getProductName() == null ? "Unknown Product" : item.getProductName(),
                            item.getQuantity() == null ? 1 : item.getQuantity(),
                            item.getUnitPrice() == null ? 0.0 : item.getUnitPrice().doubleValue(),
                            orderReadModel)));
        }

        orderReadModelRepository.save(orderReadModel);
        logger.info("Order read model created: orderId={}", event.getOrderId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        requireText(event.getOrderId(), "orderId");
        requireText(event.getNewStatus(), "newStatus");

        orderReadModelRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus(event.getNewStatus());
            order.setLastUpdated(LocalDateTime.now());
            orderReadModelRepository.save(order);
            logger.info("Order status updated: orderId={}, status={}", event.getOrderId(), event.getNewStatus());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        requireText(event.getOrderId(), "orderId");
        requireText(event.getPaymentId(), "paymentId");
        requireText(event.getPaymentStatus(), "paymentStatus");

        Optional<OrderReadModel> orderOpt = orderReadModelRepository.findById(event.getOrderId());
        if (orderOpt.isEmpty()) {
            return;
        }

        OrderReadModel order = orderOpt.get();
        order.setPaymentId(event.getPaymentId());
        order.setPaymentStatus(event.getPaymentStatus());
        order.setLastUpdated(LocalDateTime.now());
        order.setStatus("APPROVED".equals(event.getPaymentStatus()) ? "PAID" : "CANCELLED");
        orderReadModelRepository.save(order);
        logger.info("Payment processed in read model: orderId={}, status={}", event.getOrderId(),
                event.getPaymentStatus());
    }

    public Optional<OrderReadModel> getOrderById(String orderId) {
        return orderReadModelRepository.findById(orderId);
    }

    public Page<OrderReadModel> getOrders(Pageable pageable, String customerId, String status) {
        boolean hasCustomer = customerId != null && !customerId.isBlank();
        boolean hasStatus = status != null && !status.isBlank();

        if (hasCustomer && hasStatus) {
            return orderReadModelRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        }
        if (hasCustomer) {
            return orderReadModelRepository.findByCustomerId(customerId, pageable);
        }
        if (hasStatus) {
            return orderReadModelRepository.findByStatus(status, pageable);
        }
        return orderReadModelRepository.findAll(pageable);
    }
}
