package com.ordersystem.unified.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving Thymeleaf HTML pages.
 * Handles all page navigation routes.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("pageSubtitle", "Welcome back! Here's what's happening with your business today.");
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("pageTitle", "Orders");
        model.addAttribute("pageSubtitle", "Manage and track all customer orders");
        model.addAttribute("activePage", "orders");
        return "orders/list";
    }

    @GetMapping("/orders/create")
    public String createOrder(Model model) {
        model.addAttribute("pageTitle", "Create New Order");
        model.addAttribute("pageSubtitle", "Add a new customer order to the system");
        model.addAttribute("activePage", "orders");
        return "orders/create";
    }

    @GetMapping("/payments")
    public String payments(Model model) {
        model.addAttribute("pageTitle", "Payments");
        model.addAttribute("pageSubtitle", "Track and manage all payment transactions");
        model.addAttribute("activePage", "payments");
        return "payments/list";
    }

    @GetMapping("/inventory")
    public String inventory(Model model) {
        model.addAttribute("pageTitle", "Inventory");
        model.addAttribute("pageSubtitle", "Monitor stock levels and manage products");
        model.addAttribute("activePage", "inventory");
        return "inventory/list";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("pageTitle", "Customers");
        model.addAttribute("pageSubtitle", "View and manage customer information");
        model.addAttribute("activePage", "customers");
        return "customers/list";
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("pageTitle", "Analytics");
        model.addAttribute("pageSubtitle", "Business intelligence and performance metrics");
        model.addAttribute("activePage", "analytics");
        return "analytics/index";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "Settings");
        model.addAttribute("pageSubtitle", "System configuration and preferences");
        model.addAttribute("activePage", "settings");
        return "settings/index";
    }
}
