package com.ordersystem.order.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Saga Monitoring and Management
 * 
 * Provides endpoints for:
 * - Saga status monitoring
 * - Recovery management
 * - Performance metrics
 * - Health checks
 * - Manual interventions
 */
@RestController
@RequestMapping("/api/orders/saga")
public class SagaMonitoringController {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaMonitoringController.class);
    
    @Autowired
    private SagaStateManager sagaStateManager;
    
    @Autowired
    private SagaRecoveryService sagaRecoveryService;
    
    @Autowired
    private SagaMetrics sagaMetrics;
    
    @Autowired
    private OrderSaga orderSaga;
    
    /**
     * Get saga health overview
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSagaHealth() {
        try {
            SagaMetrics.MetricsSummary metrics = sagaMetrics.getMetricsSummary();
            SagaRecoveryService.RecoveryMetrics recovery = sagaRecoveryService.getRecoveryMetrics();
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", metrics.isHealthy() ? "HEALTHY" : "DEGRADED");
            health.put("activeSagas", metrics.getActiveSagas());
            health.put("pendingSagas", metrics.getPendingSagas());
            health.put("compensatingSagas", metrics.getCompensatingSagas());
            health.put("successRate", String.format("%.2f%%", metrics.getSuccessRate() * 100));
            health.put("recoverySuccessRate", String.format("%.2f%%", metrics.getRecoverySuccessRate() * 100));
            health.put("recoveryEnabled", recovery.isRecoveryEnabled());
            health.put("totalRecoveryAttempts", recovery.getTotalRecoveryAttempts());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error getting saga health: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get detailed saga statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getSagaStatistics() {
        try {
            SagaStateManager.SagaStatistics stats = sagaStateManager.getSagaStatistics();
            SagaRecoveryService.RecoveryMetrics recovery = sagaRecoveryService.getRecoveryMetrics();
            
            Map<String, Object> statistics = new HashMap<>();
            
            // Status counts
            Map<String, Long> statusCounts = new HashMap<>();
            for (SagaStatus status : SagaStatus.values()) {
                statusCounts.put(status.name(), stats.getCountByStatus(status));
            }
            statistics.put("statusCounts", statusCounts);
            
            // Recovery metrics
            Map<String, Object> recoveryStats = new HashMap<>();
            recoveryStats.put("totalAttempts", recovery.getTotalRecoveryAttempts());
            recoveryStats.put("successful", recovery.getSuccessfulRecoveries());
            recoveryStats.put("failed", recovery.getFailedRecoveries());
            recoveryStats.put("successRate", String.format("%.2f%%", recovery.getSuccessRate()));
            recoveryStats.put("cleanedUp", recovery.getCleanedUpSagas());
            statistics.put("recovery", recoveryStats);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error getting saga statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get saga by ID
     */
    @GetMapping("/{sagaId}")
    public ResponseEntity<SagaInstance> getSaga(@PathVariable String sagaId) {
        try {
            SagaInstance saga = sagaStateManager.getSaga(sagaId);
            return ResponseEntity.ok(saga);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting saga {}: {}", sagaId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get saga by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<SagaInstance> getSagaByOrderId(@PathVariable String orderId) {
        try {
            Optional<SagaInstance> saga = sagaStateManager.getSagaByOrderId(orderId);
            
            if (saga.isPresent()) {
                return ResponseEntity.ok(saga.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error getting saga for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get sagas by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SagaInstance>> getSagasByStatus(@PathVariable SagaStatus status) {
        try {
            List<SagaInstance> sagas = sagaStateManager.findSagasByStatus(status);
            return ResponseEntity.ok(sagas);
            
        } catch (Exception e) {
            logger.error("Error getting sagas by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get sagas requiring attention
     */
    @GetMapping("/attention")
    public ResponseEntity<List<SagaInstance>> getSagasRequiringAttention() {
        try {
            List<SagaInstance> sagas = sagaStateManager.findSagasRequiringAttention();
            return ResponseEntity.ok(sagas);
            
        } catch (Exception e) {
            logger.error("Error getting sagas requiring attention: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Manual saga retry
     */
    @PostMapping("/{sagaId}/retry")
    public ResponseEntity<Map<String, Object>> retrySaga(@PathVariable String sagaId) {
        try {
            boolean success = sagaRecoveryService.forceRecoverSaga(sagaId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("retryInitiated", success);
            response.put("message", success ? "Retry initiated successfully" : "Retry failed or not possible");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrying saga {}: {}", sagaId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Manual saga retry by order ID
     */
    @PostMapping("/order/{orderId}/retry")
    public ResponseEntity<Map<String, Object>> retrySagaByOrderId(@PathVariable String orderId) {
        try {
            boolean success = orderSaga.retrySaga(orderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("retryInitiated", success);
            response.put("message", success ? "Retry initiated successfully" : "Retry failed or not possible");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrying saga for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Enable/disable recovery service
     */
    @PostMapping("/recovery/toggle")
    public ResponseEntity<Map<String, Object>> toggleRecovery(@RequestParam boolean enabled) {
        try {
            sagaRecoveryService.setRecoveryEnabled(enabled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("recoveryEnabled", enabled);
            response.put("message", "Recovery service " + (enabled ? "enabled" : "disabled"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error toggling recovery service: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Force cleanup of old sagas
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> forceCleanup(@RequestParam(defaultValue = "30") int retentionDays) {
        try {
            int cleaned = sagaStateManager.cleanupOldSagas(retentionDays);
            
            Map<String, Object> response = new HashMap<>();
            response.put("cleanedUp", cleaned);
            response.put("retentionDays", retentionDays);
            response.put("message", "Cleaned up " + cleaned + " old sagas");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during forced cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get recovery metrics
     */
    @GetMapping("/recovery/metrics")
    public ResponseEntity<SagaRecoveryService.RecoveryMetrics> getRecoveryMetrics() {
        try {
            SagaRecoveryService.RecoveryMetrics metrics = sagaRecoveryService.getRecoveryMetrics();
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error getting recovery metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update saga timeout for specific saga
     */
    @PostMapping("/{sagaId}/timeout")
    public ResponseEntity<Map<String, Object>> updateSagaTimeout(
            @PathVariable String sagaId, 
            @RequestParam int timeoutMinutes) {
        try {
            SagaInstance saga = sagaStateManager.getSaga(sagaId);
            
            // This would require adding a method to SagaStateManager
            // For now, just return the current state
            Map<String, Object> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("currentTimeout", saga.getTimeoutAt());
            response.put("message", "Timeout update requested for " + timeoutMinutes + " minutes");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating timeout for saga {}: {}", sagaId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}