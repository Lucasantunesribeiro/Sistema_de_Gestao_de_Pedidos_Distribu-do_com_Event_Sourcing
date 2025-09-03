package com.ordersystem.unified.order;

import com.ordersystem.unified.order.dto.*;
import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.model.OrderItemEntity;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.shared.events.OrderStatus;
import com.ordersystem.unified.shared.exceptions.InvalidOrderException;
import com.ordersystem.unified.shared.exceptions.OrderNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing orders in the unified system.
 * Handles order creation, processing, and status management with direct service calls.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    // Temporarily comment out external services to avoid dependency issues
    // @Autowired
    // private PaymentService paymentService;

    // @Autowired
    // private InventoryService inventoryService;

    /**
     * Creates a new order with simplified processing (no external services).
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        String correlationId = request.getCorrelationId() != null ? 
            request.getCorrelationId() : UUID.randomUUID().toString();
        
        logger.info("Creating order for customer: {}, correlationId: {}", 
                   request.getCustomerId(), correlationId);

        try {
            // 1. Validate order request
            validateOrderRequest(request);

            // 2. Calculate total amount
            BigDecimal totalAmount = calculateTotalAmount(request.getItems());

            // 3. Create order entity
            String orderId = UUID.randomUUID().toString();
            Order order = new Order(orderId, request.getCustomerId(), request.getCustomerName(), totalAmount);
            order.setCorrelationId(correlationId);

            // 4. Add items to order
            for (OrderItemRequest itemRequest : request.getItems()) {
                OrderItemEntity item = new OrderItemEntity(
                    itemRequest.getProductId(),
                    itemRequest.getProductName(),
                    itemRequest.getQuantity(),
                    itemRequest.getUnitPrice()
                );
                order.addItem(item);
            }

            // 5. Save order directly as CONFIRMED (simplified version)
            order.updateStatus(OrderStatus.CONFIRMED);
            order = orderRepository.save(order);
            logger.info("Order created and confirmed with ID: {}, correlationId: {}", orderId, correlationId);

            // 6. Log order creation (simplified event publishing)
            logger.info("Order created successfully: orderId={}, customerId={}, totalAmount={}", 
                       orderId, request.getCustomerId(), totalAmount);

            return convertToOrderResponse(order);

        } catch (Exception e) {
            logger.error("Failed to create order for customer: {}, correlationId: {}, error: {}", 
                        request.getCustomerId(), correlationId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieves an order by ID.
     */
    @Cacheable(value = "orders", key = "#orderId")
    public OrderResponse getOrder(String orderId) {
        logger.debug("Retrieving order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        return convertToOrderResponse(order);
    }

    /**
     * Retrieves orders for a specific customer.
     */
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        logger.debug("Retrieving orders for customer: {}", customerId);
        
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orders.stream()
            .map(this::convertToOrderResponse)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves orders by status.
     */
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        logger.debug("Retrieving orders with status: {}", status);
        
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
            .map(this::convertToOrderResponse)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves recent orders with pagination.
     */
    public List<OrderResponse> getRecentOrders(org.springframework.data.domain.Pageable pageable) {
        logger.debug("Retrieving recent orders with page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        List<Order> orders = orderRepository.findRecentOrders(pageable);
        return orders.stream()
            .map(this::convertToOrderResponse)
            .collect(Collectors.toList());
    }

    // Private helper methods

    private void validateOrderRequest(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }

        for (OrderItemRequest item : request.getItems()) {
            if (item.getQuantity() <= 0) {
                throw new InvalidOrderException("Item quantity must be positive: " + item.getProductId());
            }
            if (item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidOrderException("Item price must be positive: " + item.getProductId());
            }
        }
    }

    private BigDecimal calculateTotalAmount(List<OrderItemRequest> items) {
        return items.stream()
            .map(OrderItemRequest::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Removed unused methods that were causing dependency issues

    private OrderResponse convertToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
            .map(item -> new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                item.getTotalPrice()
            ))
            .collect(Collectors.toList());

        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getCustomerName(),
            order.getStatus(),
            order.getTotalAmount(),
            itemResponses,
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getCorrelationId()
        );
    }
}