package com.ordersystem.unified.web;

import com.ordersystem.unified.health.HealthController;
import com.ordersystem.unified.order.OrderService;
import com.ordersystem.unified.order.dto.Order;
import com.ordersystem.unified.payment.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard Controller - Serves the main dashboard page and API endpoints
 */
@Controller
public class DashboardController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private HealthController healthController;

    /**
     * Serve the main dashboard page
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        try {
            // Get dashboard statistics
            DashboardStats stats = getDashboardStatistics();
            
            // Add data to model
            model.addAttribute("totalOrders", stats.getTotalOrders());
            model.addAttribute("totalRevenue", stats.getTotalRevenue());
            model.addAttribute("pendingOrders", stats.getPendingOrders());
            model.addAttribute("successRate", stats.getSuccessRate());
            model.addAttribute("orderGrowth", stats.getOrderGrowth());
            model.addAttribute("revenueGrowth", stats.getRevenueGrowth());
            model.addAttribute("pendingOrdersChange", stats.getPendingOrdersChange());
            model.addAttribute("successRateChange", stats.getSuccessRateChange());
            
            // System health
            model.addAttribute("systemHealth", healthController.getDetailedHealth());
            model.addAttribute("avgResponseTime", "45ms");
            model.addAttribute("memoryUsage", "68%");
            model.addAttribute("systemUptime", getSystemUptime());
            model.addAttribute("lastDeploy", "2024-01-15");
            
            // Recent orders
            List<Order> recentOrders = getRecentOrders(5);
            model.addAttribute("recentOrders", recentOrders);
            
            // Payment methods distribution
            Map<String, PaymentMethodStats> paymentMethods = getPaymentMethodsDistribution();
            model.addAttribute("paymentMethods", paymentMethods);
            
            // Notification count
            model.addAttribute("notificationCount", 3);
            model.addAttribute("pendingOrdersCount", stats.getPendingOrders());
            
        } catch (Exception e) {
            // Log error and provide default values
            System.err.println("Error loading dashboard data: " + e.getMessage());
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalRevenue", BigDecimal.ZERO);
            model.addAttribute("pendingOrders", 0);
            model.addAttribute("successRate", 0.0);
        }
        
        return "dashboard";
    }

    /**
     * API endpoint for dashboard statistics
     */
    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public DashboardStats getDashboardStats() {
        return getDashboardStatistics();
    }

    /**
     * API endpoint for recent orders
     */
    @GetMapping("/api/orders/recent")
    @ResponseBody
    public List<Order> getRecentOrdersApi() {
        return getRecentOrders(10);
    }

    /**
     * Get comprehensive dashboard statistics
     */
    private DashboardStats getDashboardStatistics() {
        try {
            // Get all orders for calculations
            List<Order> allOrders = orderService.getAllOrders();
            
            // Calculate basic metrics
            long totalOrders = allOrders.size();
            long pendingOrders = allOrders.stream()
                .filter(order -> "PENDING".equals(order.getStatus()))
                .count();
            long confirmedOrders = allOrders.stream()
                .filter(order -> "CONFIRMED".equals(order.getStatus()))
                .count();
            
            // Calculate total revenue
            BigDecimal totalRevenue = allOrders.stream()
                .filter(order -> "CONFIRMED".equals(order.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate success rate
            double successRate = totalOrders > 0 ? 
                (double) confirmedOrders / totalOrders * 100 : 0.0;
            
            // Create statistics object
            DashboardStats stats = new DashboardStats();
            stats.setTotalOrders(totalOrders);
            stats.setPendingOrders(pendingOrders);
            stats.setTotalRevenue(totalRevenue);
            stats.setSuccessRate(Math.round(successRate * 10.0) / 10.0);
            
            // Mock growth percentages (in real implementation, compare with previous period)
            stats.setOrderGrowth("+12.5%");
            stats.setRevenueGrowth("+8.2%");
            stats.setPendingOrdersChange(-5);
            stats.setSuccessRateChange("+2.1%");
            
            return stats;
            
        } catch (Exception e) {
            System.err.println("Error calculating dashboard statistics: " + e.getMessage());
            return new DashboardStats(); // Return empty stats
        }
    }

    /**
     * Get recent orders
     */
    private List<Order> getRecentOrders(int limit) {
        try {
            return orderService.getAllOrders().stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting recent orders: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get payment methods distribution
     */
    private Map<String, PaymentMethodStats> getPaymentMethodsDistribution() {
        Map<String, PaymentMethodStats> distribution = new HashMap<>();
        
        try {
            // Mock data - in real implementation, query from payment service
            distribution.put("CREDIT_CARD", new PaymentMethodStats(45, 65.0, new BigDecimal("12450")));
            distribution.put("PIX", new PaymentMethodStats(20, 25.0, new BigDecimal("4800")));
            distribution.put("BANK_TRANSFER", new PaymentMethodStats(8, 10.0, new BigDecimal("2100")));
            
        } catch (Exception e) {
            System.err.println("Error getting payment methods distribution: " + e.getMessage());
        }
        
        return distribution;
    }

    /**
     * Get system uptime (mock implementation)
     */
    private String getSystemUptime() {
        // In real implementation, calculate from application start time
        return "2d 14h 32m";
    }

    /**
     * Dashboard Statistics DTO
     */
    public static class DashboardStats {
        private long totalOrders;
        private long pendingOrders;
        private BigDecimal totalRevenue;
        private double successRate;
        private String orderGrowth;
        private String revenueGrowth;
        private int pendingOrdersChange;
        private String successRateChange;

        // Constructors
        public DashboardStats() {
            this.totalOrders = 0;
            this.pendingOrders = 0;
            this.totalRevenue = BigDecimal.ZERO;
            this.successRate = 0.0;
            this.orderGrowth = "0%";
            this.revenueGrowth = "0%";
            this.pendingOrdersChange = 0;
            this.successRateChange = "0%";
        }

        // Getters and Setters
        public long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

        public long getPendingOrders() { return pendingOrders; }
        public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }

        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }

        public String getOrderGrowth() { return orderGrowth; }
        public void setOrderGrowth(String orderGrowth) { this.orderGrowth = orderGrowth; }

        public String getRevenueGrowth() { return revenueGrowth; }
        public void setRevenueGrowth(String revenueGrowth) { this.revenueGrowth = revenueGrowth; }

        public int getPendingOrdersChange() { return pendingOrdersChange; }
        public void setPendingOrdersChange(int pendingOrdersChange) { this.pendingOrdersChange = pendingOrdersChange; }

        public String getSuccessRateChange() { return successRateChange; }
        public void setSuccessRateChange(String successRateChange) { this.successRateChange = successRateChange; }
    }

    /**
     * Payment Method Statistics DTO
     */
    public static class PaymentMethodStats {
        private int count;
        private double percentage;
        private BigDecimal amount;

        public PaymentMethodStats(int count, double percentage, BigDecimal amount) {
            this.count = count;
            this.percentage = percentage;
            this.amount = amount;
        }

        // Getters
        public int getCount() { return count; }
        public double getPercentage() { return percentage; }
        public BigDecimal getAmount() { return amount; }
    }
}