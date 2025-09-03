package com.ordersystem.unified.order;

import com.ordersystem.unified.order.dto.*;
import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.model.OrderItemEntity;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.shared.events.OrderStatus;
import com.ordersystem.unified.shared.exceptions.InvalidOrderException;
import com.ordersystem.unified.shared.exceptions.OrderNotFoundException;

// Import new services
import com.ordersystem.unified.payment.PaymentService;
import com.ordersystem.unified.payment.dto.*;
import com.ordersystem.unified.inventory.InventoryService;
import com.ordersystem.unified.inventory.dto.*;

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
 * Complete Order Service with integrated Payment and Inventory services
 * Handles order creation, processing, and status management with proper transaction orchestration
 */
@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InventoryService inventoryService;

    /**
     * Creates a new order with complete payment and inventory integration
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        String correlationId = request.getCorrelationId() != null ? 
            request.getCorrelationId() : UUID.randomUUID().toString();
        
        logger.info("Creating order for customer: {}, items: {}, correlationId: {}", 
                   request.getCustomerId(), request.getItems().size(), correlationId);

        String orderId = UUID.randomUUID().toString();
        String reservationId = null;
        String paymentId = null;

        try {
            // 1. Validate order request
            validateOrderRequest(request);

            // 2. Calculate total amount
            BigDecimal totalAmount = calculateTotalAmount(request.getItems());

            // 3. Create order entity (initially PENDING)
            Order order = new Order(orderId, request.getCustomerId(), request.getCustomerName(), totalAmount);
            order.setCorrelationId(correlationId);
            order.updateStatus(OrderStatus.PENDING);

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

            // 5. Save initial order
            order = orderRepository.save(order);
            logger.info("Order created with ID: {}, status: PENDING, correlationId: {}", orderId, correlationId);

            // 6. Reserve inventory
            ReservationRequest reservationRequest = createReservationRequest(order, correlationId);
            ReservationResponse reservationResponse = inventoryService.reserveItems(reservationRequest);
            
            if (!reservationResponse.hasAnyReservation()) {
                logger.warn("Inventory reservation failed: orderId={}, message={}", orderId, reservationResponse.getMessage());
                order.updateStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("Insufficient inventory: " + reservationResponse.getMessage());
                orderRepository.save(order);
                
                throw new OrderProcessingException("Order cancelled due to insufficient inventory: " + reservationResponse.getMessage());
            }
            
            reservationId = reservationResponse.getReservationId();
            order.setReservationId(reservationId);
            order.updateStatus(OrderStatus.INVENTORY_RESERVED);
            orderRepository.save(order);
            
            logger.info("Inventory reserved: orderId={}, reservationId={}, status={}", 
                       orderId, reservationId, reservationResponse.getStatus());

            // 7. Process payment
            PaymentRequest paymentRequest = createPaymentRequest(order, request, correlationId);
            PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
            
            paymentId = paymentResponse.getPaymentId();
            order.setPaymentId(paymentId);
            
            if (paymentResponse.isSuccess()) {
                // Payment successful - confirm inventory reservation
                boolean confirmationSuccess = inventoryService.confirmReservation(reservationId, correlationId);
                
                if (confirmationSuccess) {
                    order.updateStatus(OrderStatus.CONFIRMED);
                    order.setTransactionId(paymentResponse.getTransactionId());
                    logger.info("Order confirmed: orderId={}, paymentId={}, transactionId={}", 
                               orderId, paymentId, paymentResponse.getTransactionId());
                } else {
                    // Inventory confirmation failed - need to refund payment
                    logger.error("Inventory confirmation failed after payment: orderId={}, paymentId={}", orderId, paymentId);
                    
                    // Attempt to refund payment
                    paymentService.processRefund(paymentId, totalAmount, "Inventory confirmation failed");
                    
                    order.updateStatus(OrderStatus.CANCELLED);
                    order.setCancellationReason("Inventory confirmation failed after payment");
                    
                    throw new OrderProcessingException("Order cancelled due to inventory confirmation failure");
                }
            } else {
                // Payment failed - release inventory reservation
                logger.warn("Payment failed: orderId={}, paymentId={}, message={}", 
                           orderId, paymentId, paymentResponse.getMessage());
                
                inventoryService.releaseReservation(reservationId, correlationId);
                
                order.updateStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("Payment failed: " + paymentResponse.getMessage());
                
                throw new OrderProcessingException("Order cancelled due to payment failure: " + paymentResponse.getMessage());
            }

            // 8. Save final order state
            order = orderRepository.save(order);
            
            logger.info("Order processing completed successfully: orderId={}, status={}, correlationId={}", 
                       orderId, order.getStatus(), correlationId);

            return convertToOrderResponse(order);

        } catch (OrderProcessingException e) {
            // Re-throw order processing exceptions as-is
            orderRepository.save(orderRepository.findById(orderId).orElse(null));
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating order: orderId={}, correlationId={}", orderId, correlationId, e);
            
            // Rollback operations
            rollbackOrderCreation(orderId, reservationId, paymentId, correlationId, totalAmount);
            
            throw new OrderProcessingException("Order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel an existing order with proper cleanup
     */
    public OrderResponse cancelOrder(String orderId, String reason, String correlationId) {
        logger.info("Cancelling order: orderId={}, reason={}, correlationId={}", orderId, reason, correlationId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        if (order.getStatus().isTerminal()) {
            throw new InvalidOrderException("Cannot cancel order in terminal status: " + order.getStatus());
        }
        
        try {
            // Release inventory reservation if exists
            if (order.getReservationId() != null) {
                boolean released = inventoryService.releaseReservation(order.getReservationId(), correlationId);
                logger.info("Inventory reservation release result: orderId={}, reservationId={}, success={}", 
                           orderId, order.getReservationId(), released);
            }
            
            // Process refund if payment was made
            if (order.getPaymentId() != null && order.getStatus() == OrderStatus.CONFIRMED) {
                PaymentResponse refundResponse = paymentService.processRefund(
                    order.getPaymentId(), order.getTotalAmount(), reason);
                logger.info("Refund processed: orderId={}, paymentId={}, success={}", 
                           orderId, order.getPaymentId(), refundResponse.isSuccess());
            }
            
            // Update order status
            order.updateStatus(OrderStatus.CANCELLED);
            order.setCancellationReason(reason);
            order = orderRepository.save(order);
            
            logger.info("Order cancelled successfully: orderId={}, correlationId={}", orderId, correlationId);
            
            return convertToOrderResponse(order);
            
        } catch (Exception e) {
            logger.error("Error cancelling order: orderId={}, correlationId={}", orderId, correlationId, e);
            throw new OrderProcessingException("Failed to cancel order: " + e.getMessage(), e);
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

    /**
     * Get order statistics for dashboard
     */
    public OrderStatistics getOrderStatistics() {
        logger.debug("Retrieving order statistics");
        
        long totalOrders = orderRepository.count();
        long confirmedOrders = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        
        BigDecimal totalRevenue = orderRepository.getTotalRevenueByStatus(OrderStatus.CONFIRMED);
        
        return new OrderStatistics(totalOrders, confirmedOrders, cancelledOrders, pendingOrders, totalRevenue);
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

    private ReservationRequest createReservationRequest(Order order, String correlationId) {
        ReservationRequest request = new ReservationRequest();
        request.setOrderId(order.getId());
        request.setCorrelationId(correlationId);
        
        List<ItemReservation> items = order.getItems().stream()
            .map(item -> new ItemReservation(item.getProductId(), item.getQuantity()))
            .collect(Collectors.toList());
        
        request.setItems(items);
        return request;
    }

    private PaymentRequest createPaymentRequest(Order order, CreateOrderRequest originalRequest, String correlationId) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(order.getId());
        request.setAmount(order.getTotalAmount());
        request.setCorrelationId(correlationId);
        
        // Use payment method from original request or default to CREDIT_CARD
        PaymentMethod method = originalRequest.getPaymentMethod() != null ? 
            originalRequest.getPaymentMethod() : PaymentMethod.CREDIT_CARD;
        request.setMethod(method);
        
        // Create customer info
        CustomerInfo customer = new CustomerInfo();
        customer.setCustomerId(order.getCustomerId());
        customer.setName(order.getCustomerName());
        customer.setEmail(originalRequest.getCustomerEmail());
        request.setCustomer(customer);
        
        return request;
    }

    private void rollbackOrderCreation(String orderId, String reservationId, String paymentId, 
                                     String correlationId, BigDecimal amount) {
        logger.info("Rolling back order creation: orderId={}, correlationId={}", orderId, correlationId);
        
        try {
            // Release inventory reservation if it exists
            if (reservationId != null) {
                inventoryService.releaseReservation(reservationId, correlationId);
                logger.info("Inventory reservation released during rollback: reservationId={}", reservationId);
            }
            
            // Process refund if payment was made
            if (paymentId != null) {
                paymentService.processRefund(paymentId, amount, "Order creation rollback");
                logger.info("Payment refunded during rollback: paymentId={}", paymentId);
            }
            
            // Update order status to cancelled
            orderRepository.findById(orderId).ifPresent(order -> {
                order.updateStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("Order creation failed - rolled back");
                orderRepository.save(order);
            });
            
        } catch (Exception e) {
            logger.error("Error during order rollback: orderId={}, correlationId={}", orderId, correlationId, e);
        }
    }

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
            order.getCorrelationId(),
            order.getReservationId(),
            order.getPaymentId(),
            order.getTransactionId(),
            order.getCancellationReason()
        );
    }

    // Exception classes
    public static class OrderProcessingException extends RuntimeException {
        public OrderProcessingException(String message) {
            super(message);
        }
        
        public OrderProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}