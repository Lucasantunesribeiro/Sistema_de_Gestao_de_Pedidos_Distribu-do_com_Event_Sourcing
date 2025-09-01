package com.ordersystem.query.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Redis Configuration for Order Query Service
 * Enables scheduling for Redis Streams consumer
 * Note: RedisTemplate is configured in CacheConfig to avoid bean conflicts
 */
@Configuration
@EnableScheduling
public class RedisConfig {
  // RedisTemplate bean moved to CacheConfig to avoid conflicts
  // This class now only enables scheduling for Redis Streams consumer
}