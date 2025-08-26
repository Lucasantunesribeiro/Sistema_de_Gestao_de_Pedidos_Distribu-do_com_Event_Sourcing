package com.ordersystem.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "messaging.type", havingValue = "redis", matchIfMissing = false)
public abstract class RedisEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisEventListener.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService executor;
    
    public RedisEventListener(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }
    
    @PostConstruct
    public void startListening() {
        String[] streams = getStreamsToListen();
        for (String stream : streams) {
            executor.scheduleAtFixedRate(() -> pollStream(stream), 0, 1, TimeUnit.SECONDS);
        }
    }
    
    protected abstract String[] getStreamsToListen();
    protected abstract void handleEvent(String eventType, Object payload);
    
    private void pollStream(String stream) {
        try {
            List<ObjectRecord<String, Object>> records = redisTemplate.opsForStream()
                .read(StreamOffset.fromStart(stream))
                .stream()
                .limit(10)
                .toList();
                
            for (ObjectRecord<String, Object> record : records) {
                Map<String, Object> value = (Map<String, Object>) record.getValue();
                String eventType = (String) value.get("eventType");
                Object payload = value.get("payload");
                
                try {
                    handleEvent(eventType, payload);
                    // Acknowledge message processing
                    redisTemplate.opsForStream().delete(stream, record.getId());
                } catch (Exception e) {
                    logger.error("Failed to process event {} from stream {}: {}", eventType, stream, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to poll stream {}: {}", stream, e.getMessage());
        }
    }
}