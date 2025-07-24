package com.ordersystem.inventory.service;

import com.ordersystem.inventory.model.InventoryItem;
import com.ordersystem.shared.events.OrderCreatedEvent;
import com.ordersystem.shared.events.PaymentProcessedEvent;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentMap;

@Service
public class InventoryService {

    private final ConcurrentMap<String, InventoryItem> inventory = InventoryItem.getInventory();

    public void reserveInventory(OrderCreatedEvent orderEvent) {
        System.out.println("Reserving inventory for order: " + orderEvent.getOrderId());
        
        boolean allItemsReserved = true;
        
        for (OrderCreatedEvent.OrderItem item : orderEvent.getItems()) {
            InventoryItem inventoryItem = inventory.get(item.getProductId());
            
            if (inventoryItem == null) {
                System.out.println("Product not found in inventory: " + item.getProductId());
                allItemsReserved = false;
                break;
            }
            
            if (!inventoryItem.canReserve(item.getQuantity())) {
                System.out.println("Insufficient inventory for product: " + item.getProductId() + 
                                 " (requested: " + item.getQuantity() + 
                                 ", available: " + inventoryItem.getAvailableQuantity() + ")");
                allItemsReserved = false;
                break;
            }
        }
        
        if (allItemsReserved) {
            // Reserve all items
            for (OrderCreatedEvent.OrderItem item : orderEvent.getItems()) {
                InventoryItem inventoryItem = inventory.get(item.getProductId());
                inventoryItem.reserve(item.getQuantity());
                System.out.println("Reserved " + item.getQuantity() + " units of " + item.getProductName());
            }
        } else {
            System.out.println("Failed to reserve inventory for order: " + orderEvent.getOrderId());
        }
    }

    public void processPaymentResult(PaymentProcessedEvent paymentEvent) {
        System.out.println("Processing payment result for order: " + paymentEvent.getOrderId() + 
                         " with status: " + paymentEvent.getPaymentStatus());
        
        // For demonstration, we'll assume we can retrieve the order items from somewhere
        // In a real system, you'd store the reservation information
        
        if ("APPROVED".equals(paymentEvent.getPaymentStatus())) {
            System.out.println("Payment approved, confirming inventory reservation for order: " + 
                             paymentEvent.getOrderId());
            // In a real system, you'd confirm the reservation here
        } else {
            System.out.println("Payment declined, releasing inventory reservation for order: " + 
                             paymentEvent.getOrderId());
            // In a real system, you'd release the reservation here
        }
    }

    public InventoryItem getInventoryItem(String productId) {
        return inventory.get(productId);
    }
}