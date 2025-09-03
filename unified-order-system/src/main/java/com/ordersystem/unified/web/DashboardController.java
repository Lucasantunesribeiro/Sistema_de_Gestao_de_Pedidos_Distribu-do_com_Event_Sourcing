package com.ordersystem.unified.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Dashboard controller for web interface.
 * Handles both web pages and API endpoints for dashboard data.
 */
@Controller
public class DashboardController {

    @GetMapping("/")
    public String home(Model model) {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalOrders", 100);
        model.addAttribute("pendingOrders", 25);
        model.addAttribute("completedOrders", 70);
        model.addAttribute("totalRevenue", "$10,000");
        model.addAttribute("recentOrders", getRecentOrders());
        model.addAttribute("systemHealth", getSystemHealth());
        return "dashboard";
    }

    @GetMapping("/api/dashboard")
    @ResponseBody

    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("title", "Order Management Dashboard");
        dashboard.put("totalOrders", 100);
        dashboard.put("pendingOrders", 25);
        dashboard.put("completedOrders", 70);
        dashboard.put("cancelledOrders", 5);
        dashboard.put("totalRevenue", 10000.0);
        dashboard.put("timestamp", System.currentTimeMillis());
        return dashboard;
    }

    public List<Map<String, Object>> getRecentOrders() {
        List<Map<String, Object>> orders = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> order = new HashMap<>();
            order.put("orderId", "ORDER-" + i);
            order.put("status", "PENDING");
            order.put("customerName", "Customer " + i);
            order.put("totalAmount", 100.0 * i);
            order.put("timestamp", System.currentTimeMillis());
            orders.add(order);
        }
        
        return orders;
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "Connected");
        health.put("cache", "Active");
        health.put("services", "Running");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }

    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", 100);
        stats.put("todayOrders", 15);
        stats.put("weekOrders", 75);
        stats.put("monthOrders", 300);
        stats.put("averageOrderValue", 150.0);
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
}