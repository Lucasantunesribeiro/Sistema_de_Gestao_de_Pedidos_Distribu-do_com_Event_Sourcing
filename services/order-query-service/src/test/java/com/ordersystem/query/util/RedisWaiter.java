package com.ordersystem.query.util;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

public class RedisWaiter {
    
    public static void waitUntilAvailable(String host, int port, long timeoutMs) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        factory.afterPropertiesSet();
        
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                factory.getConnection().ping();
                factory.destroy();
                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        factory.destroy();
        throw new RuntimeException("Redis not available at " + host + ":" + port + " within " + timeoutMs + "ms");
    }
}