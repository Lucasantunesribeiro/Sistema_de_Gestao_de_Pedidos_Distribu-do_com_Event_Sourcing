package com.ordersystem.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
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
            // Read from the end of stream to get only new messages
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                .read(StreamOffset.latest(stream))
                .stream()
                .limit(10)
                .toList();
                
            for (MapRecord<String, Object, Object> record : records) {
                try {
                    Map<Object, Object> value = record.getValue();
                    String eventType = (String) value.get("eventType");
                    Object payload = value.get("payload");
                    
                    if (eventType != null && payload != null) {
                        handleEvent(eventType, payload);
                        logger.debug("Successfully processed event {} from stream {}", eventType, stream);
                    } else {
                        logger.warn("Received incomplete event from stream {}: eventType={}, payload={}", 
                                  stream, eventType, payload);
                    }
                } catch (Exception e) {
                    logger.error("Failed to process event from stream {}: {}", stream, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("NOGROUP")) {
                logger.debug("Stream {} does not exist yet, skipping polling", stream);
            } else {
                logger.error("Failed to poll stream {}: {}", stream, e.getMessage());
            }
        }
    }
}