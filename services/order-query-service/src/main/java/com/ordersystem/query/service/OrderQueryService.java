package com.ordersystem.query.service;

import com.ordersystem.query.entity.OrderItemReadModel;
import com.ordersystem.query.entity.OrderReadModel;
import com.ordersystem.query.repository.OrderReadModelRepository;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.OrderItem;
import com.ordersystem.shared.events.OrderStatusUpdatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderQueryService {

    @Autowired
    private OrderReadModelRepository orderReadModelRepository;

    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    public void handleOrderCreated(OrderCreatedEvent event) {
        OrderReadModel orderReadModel = new OrderReadModel(
            event.getOrderId(),
            event.getCustomerId(),
            "PENDING",
            event.getTotalAmount().doubleValue(),
            event.getTimestamp()
        );

        for (OrderItem item : event.getItems()) {
            OrderItemReadModel orderItem = new OrderItemReadModel(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice().doubleValue(),
                orderReadModel
            );
            orderReadModel.getItems().add(orderItem);
        }

        orderReadModelRepository.save(orderReadModel);
        System.out.println("Order read model created for order: " + event.getOrderId());
    }

    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        Optional<OrderReadModel> orderOpt = orderReadModelRepository.findById(event.getOrderId());
        if (orderOpt.isPresent()) {
            OrderReadModel order = orderOpt.get();
            order.setStatus(event.getNewStatus());
            order.setLastUpdated(LocalDateTime.now());
            orderReadModelRepository.save(order);
            
            // Invalidate cache after updating the order
            cacheInvalidationService.handleOrderStatusUpdated(event);
            
            System.out.println("Order status updated in read model: " + event.getOrderId() + 
                             " -> " + event.getNewStatus());
        }
    }

    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        Optional<OrderReadModel> orderOpt = orderReadModelRepository.findById(event.getOrderId());
        if (orderOpt.isPresent()) {
            OrderReadModel order = orderOpt.get();
            order.setPaymentId(event.getPaymentId());
            order.setPaymentStatus(event.getPaymentStatus());
            order.setLastUpdated(LocalDateTime.now());
            
            // Update order status based on payment result
            if ("APPROVED".equals(event.getPaymentStatus())) {
                order.setStatus("PAID");
            } else {
                order.setStatus("CANCELLED");
            }
            
            orderReadModelRepository.save(order);
            
            // Invalidate cache after processing payment
            cacheInvalidationService.handlePaymentProcessed(event);
            
            System.out.println("Payment processed in read model: " + event.getOrderId() + 
                             " -> " + event.getPaymentStatus());
        }
    }

    @Cacheable(value = "orders", key = "'findAllOrderByCreatedAtDesc'")
    public List<OrderReadModel> getAllOrders() {
        return orderReadModelRepository.findAllOrderByCreatedAtDesc();
    }

    @Cacheable(value = "single-order", key = "#orderId")
    public Optional<OrderReadModel> getOrderById(String orderId) {
        return orderReadModelRepository.findById(orderId);
    }

    @Cacheable(value = "customer-orders", key = "#customerId")
    public List<OrderReadModel> getOrdersByCustomerId(String customerId) {
        return orderReadModelRepository.findByCustomerId(customerId);
    }

    @Cacheable(value = "status-orders", key = "#status")
    public List<OrderReadModel> getOrdersByStatus(String status) {
        return orderReadModelRepository.findByStatus(status);
    }

    @Cacheable(value = "customer-orders", key = "#customerId + '::' + #status")
    public List<OrderReadModel> getOrdersByCustomerIdAndStatus(String customerId, String status) {
        return orderReadModelRepository.findByCustomerIdAndStatus(customerId, status);
    }
}