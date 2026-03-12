package com.ordersystem.unified.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service that broadcasts domain events to WebSocket subscribers in real time.
 *
 * <p>Angular clients subscribe to the following STOMP topics:</p>
 * <ul>
 *   <li>{@code /topic/orders}     — order status changes</li>
 *   <li>{@code /topic/inventory}  — stock level changes</li>
 *   <li>{@code /topic/payments}   — payment status changes</li>
 * </ul>
 *
 * <p>Messages are plain {@code Map<String,Object>} payloads serialised to JSON by the
 * Spring WebSocket message converter.  The WebSocket endpoint is {@code /ws} (with
 * SockJS fallback) — see {@link com.ordersystem.unified.config.WebSocketConfig}.</p>
 */
@Service
public class WebSocketEventService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventService.class);

    private static final String TOPIC_ORDERS    = "/topic/orders";
    private static final String TOPIC_INVENTORY = "/topic/inventory";
    private static final String TOPIC_PAYMENTS  = "/topic/payments";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendOrderUpdate(String orderId, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("status", status);
        payload.put("timestamp", Instant.now().toString());

        logger.debug("Broadcasting order update: orderId={}, status={}", orderId, status);
        messagingTemplate.convertAndSend(TOPIC_ORDERS, payload);
    }

    public void sendInventoryUpdate(String productId, int quantity) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("availableQuantity", quantity);
        payload.put("timestamp", Instant.now().toString());

        logger.debug("Broadcasting inventory update: productId={}, qty={}", productId, quantity);
        messagingTemplate.convertAndSend(TOPIC_INVENTORY, payload);
    }

    public void sendPaymentUpdate(String paymentId, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", paymentId);
        payload.put("status", status);
        payload.put("timestamp", Instant.now().toString());

        logger.debug("Broadcasting payment update: paymentId={}, status={}", paymentId, status);
        messagingTemplate.convertAndSend(TOPIC_PAYMENTS, payload);
    }

    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", true);
        status.put("endpoint", "/ws");
        status.put("topics", new String[]{TOPIC_ORDERS, TOPIC_INVENTORY, TOPIC_PAYMENTS});
        status.put("timestamp", Instant.now().toString());
        return status;
    }
}
