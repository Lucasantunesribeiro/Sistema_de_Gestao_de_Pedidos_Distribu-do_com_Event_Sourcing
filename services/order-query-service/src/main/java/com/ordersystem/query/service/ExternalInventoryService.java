package com.ordersystem.query.service;

import com.ordersystem.query.dto.ReservationResult;
import com.ordersystem.query.dto.StockLevel;

/**
 * External inventory service interface
 */
public interface ExternalInventoryService {
    
    StockLevel checkStockLevel(String productId);
    
    ReservationResult reserveStock(String productId, int quantity);
    
    String getReservationStatus(String orderId);
}