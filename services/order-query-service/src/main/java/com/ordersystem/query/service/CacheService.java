package com.ordersystem.query.service;

import com.ordersystem.query.dto.StockLevel;

import java.util.Optional;

/**
 * Cache service interface for fallback data
 */
public interface CacheService {
    
    Optional<StockLevel> getCachedStockLevel(String productId);
    
    void cacheStockLevel(String productId, StockLevel stockLevel);
    
    void queueStockReservation(String productId, int quantity);
}