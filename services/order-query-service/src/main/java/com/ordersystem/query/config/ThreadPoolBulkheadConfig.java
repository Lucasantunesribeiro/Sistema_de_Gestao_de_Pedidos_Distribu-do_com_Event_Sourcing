package com.ordersystem.query.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Thread Pool Bulk-head Configuration for service isolation
 * Prevents one service from exhausting all available threads
 */
@Configuration
public class ThreadPoolBulkheadConfig {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolBulkheadConfig.class);

    /**
     * Isolated thread pool for payment operations
     * Conservative pool size for critical payment processing
     */
    @Bean("payment-executor")
    public Executor paymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("payment-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        // Custom thread factory for monitoring
        executor.setThreadFactory(r -> {
            Thread thread = new Thread(r);
            thread.setName("payment-" + thread.getId());
            thread.setDaemon(false);
            thread.setUncaughtExceptionHandler((t, e) -> 
                log.error("Uncaught exception in payment thread {}: {}", t.getName(), e.getMessage(), e));
            return thread;
        });
        
        executor.initialize();
        log.info("Initialized payment executor with core={}, max={}, queue={}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * Isolated thread pool for inventory operations  
     * Higher throughput for inventory checks and reservations
     */
    @Bean("inventory-executor")
    public Executor inventoryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("inventory-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.setThreadFactory(r -> {
            Thread thread = new Thread(r);
            thread.setName("inventory-" + thread.getId());
            thread.setDaemon(false);
            thread.setUncaughtExceptionHandler((t, e) -> 
                log.error("Uncaught exception in inventory thread {}: {}", t.getName(), e.getMessage(), e));
            return thread;
        });
        
        executor.initialize();
        log.info("Initialized inventory executor with core={}, max={}, queue={}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * Isolated thread pool for database operations
     * Larger pool for database queries and projections
     */
    @Bean("database-executor") 
    public Executor databaseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("database-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        
        executor.setThreadFactory(r -> {
            Thread thread = new Thread(r);
            thread.setName("database-" + thread.getId());
            thread.setDaemon(false);
            thread.setUncaughtExceptionHandler((t, e) -> 
                log.error("Uncaught exception in database thread {}: {}", t.getName(), e.getMessage(), e));
            return thread;
        });
        
        executor.initialize();
        log.info("Initialized database executor with core={}, max={}, queue={}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * Isolated thread pool for query operations
     * High capacity for read-heavy operations
     */
    @Bean("query-executor")
    public Executor queryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(15);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("query-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.setThreadFactory(r -> {
            Thread thread = new Thread(r);
            thread.setName("query-" + thread.getId());
            thread.setDaemon(false);
            thread.setUncaughtExceptionHandler((t, e) -> 
                log.error("Uncaught exception in query thread {}: {}", t.getName(), e.getMessage(), e));
            return thread;
        });
        
        executor.initialize();
        log.info("Initialized query executor with core={}, max={}, queue={}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * General purpose executor for non-critical operations
     */
    @Bean("general-executor")
    public Executor generalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("general-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        log.info("Initialized general executor with core={}, max={}, queue={}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}