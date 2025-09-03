package com.ordersystem.unified.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@CrossOrigin(origins = "*")
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Map<String, Object> greeting(Map<String, Object> message) {
        return Map.of(
            "content", "Hello, " + message.get("name") + "!",
            "timestamp", LocalDateTime.now().toString()
        );
    }

    @MessageMapping("/subscribe")
    @SendTo("/topic/system")
    public Map<String, Object> subscribe(Map<String, Object> message) {
        return Map.of(
            "type", "subscription_confirmed",
            "message", "Successfully subscribed to real-time updates",
            "timestamp", LocalDateTime.now().toString()
        );
    }

    // Method to send real-time updates to all connected clients
    public void sendSystemUpdate(String type, Object data) {
        Map<String, Object> update = Map.of(
            "type", type,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/system", update);
    }

    // Method to send order updates
    public void sendOrderUpdate(String orderId, String status, Object orderData) {
        Map<String, Object> update = Map.of(
            "type", "order_update",
            "orderId", orderId,
            "status", status,
            "data", orderData,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/orders", update);
    }

    // Method to send health updates
    public void sendHealthUpdate(Map<String, Object> healthData) {
        Map<String, Object> update = Map.of(
            "type", "health_update",
            "data", healthData,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/health", update);
    }

    // Method to send metrics updates
    public void sendMetricsUpdate(Map<String, Object> metricsData) {
        Map<String, Object> update = Map.of(
            "type", "metrics_update",
            "data", metricsData,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/metrics", update);
    }

    // Method to send notification to specific user
    public void sendUserNotification(String userId, String message, String type) {
        Map<String, Object> notification = Map.of(
            "type", type,
            "message", message,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
    }
}