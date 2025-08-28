package com.ordersystem.shared.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Optimized Event Serialization Configuration
 * Performance-focused JSON serialization for Event Sourcing
 */
@Configuration
public class EventSerializationConfig {

    private static final Logger logger = LoggerFactory.getLogger(EventSerializationConfig.class);

    @Bean
    @Primary
    public ObjectMapper optimizedObjectMapper() {
        logger.info("🔧 Configuring optimized ObjectMapper for Event Sourcing");

        ObjectMapper mapper = new ObjectMapper();

        // Performance Optimizations
        
        // 1. Java Time module for LocalDateTime handling
        mapper.registerModule(new JavaTimeModule());
        
        // 2. Disable timestamp serialization as ISO string (faster)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 3. Performance-oriented deserialization settings
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        // 4. Serialization optimizations
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        
        // 5. Type information for polymorphic events (but optimized)
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        
        logger.info("✅ ObjectMapper configured with performance optimizations");
        return mapper;
    }

    @Bean
    public EventCompressionService eventCompressionService() {
        return new EventCompressionService();
    }

    /**
     * Service for compressing large events to save storage and improve performance
     */
    public static class EventCompressionService {
        
        private static final Logger logger = LoggerFactory.getLogger(EventCompressionService.class);
        private static final int COMPRESSION_THRESHOLD = 1024; // 1KB threshold

        public byte[] compress(String json) {
            if (json == null || json.length() < COMPRESSION_THRESHOLD) {
                return json.getBytes();
            }

            try {
                // Use GZIP compression for events larger than threshold
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(baos);
                gzip.write(json.getBytes());
                gzip.close();
                
                byte[] compressed = baos.toByteArray();
                logger.debug("Compressed event from {} to {} bytes", json.length(), compressed.length);
                return compressed;
                
            } catch (Exception e) {
                logger.warn("Failed to compress event, using original: {}", e.getMessage());
                return json.getBytes();
            }
        }

        public String decompress(byte[] compressed) {
            if (compressed == null) {
                return null;
            }

            try {
                // Try to decompress first
                java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(compressed);
                java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bais);
                
                StringBuilder sb = new StringBuilder();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzip.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, len));
                }
                gzip.close();
                
                return sb.toString();
                
            } catch (Exception e) {
                // If decompression fails, assume it's uncompressed data
                logger.debug("Data appears to be uncompressed, using directly");
                return new String(compressed);
            }
        }

        public boolean shouldCompress(String json) {
            return json != null && json.length() > COMPRESSION_THRESHOLD;
        }
    }
}