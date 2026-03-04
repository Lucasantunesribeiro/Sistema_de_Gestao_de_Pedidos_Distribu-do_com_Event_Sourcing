package com.ordersystem.unified.order;

import com.ordersystem.unified.shared.util.SafeEnumParser;
import com.ordersystem.unified.order.dto.CreateOrderRequest;
import com.ordersystem.unified.order.dto.OrderItemRequest;
import com.ordersystem.unified.order.dto.OrderResponse;
import com.ordersystem.unified.payment.dto.PaymentMethod;
import com.ordersystem.unified.order.dto.OrderItemResponse;
import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.model.OrderItemEntity;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.shared.events.OrderStatus;
import com.ordersystem.unified.shared.exceptions.OrderNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.RoundingMode;

/**
 * Order service for business logic.
 * Implements clean architecture with persistence.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        if (request.getCustomerId() == null || request.getCustomerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        String customerId = request.getCustomerId().trim();
        String customerName = normalizeText(request.getCustomerName(), "Cliente");
        PaymentMethod paymentMethod = request.getPaymentMethod() == null ? PaymentMethod.PIX : request.getPaymentMethod();

        List<OrderItemRequest> resolvedItems = resolveItems(request);
        if (!resolvedItems.isEmpty()) {
            request.setItems(resolvedItems);
        }

        BigDecimal totalAmount = calculateTotal(request);

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order must contain items or a positive totalAmount");
        }

        Order order = new Order(orderId, customerId, customerName, totalAmount);
        order.setCorrelationId(request.getCorrelationId());
        // paymentMethod currently not persisted; keep resolved to validate input path
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        
        if (request.getItems() != null) {
            for (var itemReq : request.getItems()) {
                if (itemReq.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Quantity must be positive");
                }
                if (itemReq.getUnitPrice() == null || itemReq.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Unit price must be positive");
                }
                
                OrderItemEntity item = new OrderItemEntity(
                    itemReq.getProductId(),
                    itemReq.getProductName(),
                    itemReq.getQuantity(),
                    itemReq.getUnitPrice()
                );
                // Total price calculated in constructor
                order.addItem(item);
            }
        }

        Order savedOrder = orderRepository.save(order);
        
        // TODO: Publish OrderCreatedEvent via Outbox
        
        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(String customerId, String status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage;

        if (customerId != null && status != null) {
            orderPage = orderRepository.findByCustomerIdAndStatus(customerId, SafeEnumParser.parseEnumOrThrow(OrderStatus.class, status, "status"), pageRequest);
        } else if (customerId != null) {
            orderPage = orderRepository.findByCustomerId(customerId, pageRequest);
        } else if (status != null) {
            orderPage = orderRepository.findByStatus(SafeEnumParser.parseEnumOrThrow(OrderStatus.class, status, "status"), pageRequest);
        } else {
            orderPage = orderRepository.findAll(pageRequest);
        }

        return orderPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(SafeEnumParser.parseEnumOrThrow(OrderStatus.class, status, "status")).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse cancelOrder(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        // TODO: Validate state transitions
        order.updateStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        
        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        long totalOrders = orderRepository.count();
        long confirmedOrders = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);

        BigDecimal totalRevenue = orderRepository.findByStatus(OrderStatus.CONFIRMED).stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        statistics.put("totalOrders", totalOrders);
        statistics.put("confirmedOrders", confirmedOrders);
        statistics.put("cancelledOrders", cancelledOrders);
        statistics.put("pendingOrders", pendingOrders);
        statistics.put("totalRevenue", totalRevenue);
        statistics.put("timestamp", System.currentTimeMillis());
        return statistics;
    }

    private BigDecimal calculateTotal(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return request.getTotalAmount() == null ? BigDecimal.ZERO : request.getTotalAmount();
        }
        
        return request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<OrderItemRequest> resolveItems(CreateOrderRequest request) {
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            return request.getItems();
        }

        BigDecimal totalAmount = request.getTotalAmount();
        List<String> productIds = request.getProductIds();

        if ((productIds == null || productIds.isEmpty())
                && (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            return new ArrayList<>();
        }

        if (productIds == null || productIds.isEmpty()) {
            productIds = List.of("ITEM-1");
        }

        BigDecimal unitPrice = BigDecimal.ZERO;
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            unitPrice = totalAmount.divide(BigDecimal.valueOf(productIds.size()), 2, RoundingMode.HALF_UP);
        }

        List<OrderItemRequest> items = new ArrayList<>();
        for (String productId : productIds) {
            String normalizedId = normalizeText(productId, "ITEM-1");
            items.add(new OrderItemRequest(normalizedId, normalizedId, 1, unitPrice));
        }
        return items;
    }

    private String normalizeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
    
    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setCustomerName(order.getCustomerName());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setCorrelationId(order.getCorrelationId());
        response.setReservationId(order.getReservationId());
        response.setPaymentId(order.getPaymentId());
        response.setTransactionId(order.getTransactionId());
        response.setCancellationReason(order.getCancellationReason());

        List<OrderItemResponse> itemResponses = order.getItems().stream().map(item -> {
            OrderItemResponse itemResponse = new OrderItemResponse();
            itemResponse.setProductId(item.getProductId());
            itemResponse.setProductName(item.getProductName());
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setUnitPrice(item.getUnitPrice());
            itemResponse.setTotalPrice(item.getTotalPrice());
            return itemResponse;
        }).collect(Collectors.toList());
        
        response.setItems(itemResponses);
        return response;
    }
}
