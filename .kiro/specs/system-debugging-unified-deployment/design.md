# Design Document

## Overview

This design addresses the critical 500 Internal Server Error debugging and implements a unified deployment architecture for the distributed order management system. The solution involves systematic debugging of the Query Service event processing, comprehensive logging implementation, and creation of a multi-stage Docker container that unifies frontend and all backend services using Supervisor for process management and Nginx as a reverse proxy.

## Architecture

### Current System Analysis
The system consists of:
- **Order Service (8081)**: Command side with Event Sourcing
- **Payment Service (8082)**: Payment processing 
- **Inventory Service (8083)**: Stock management
- **Query Service (8084)**: CQRS read models
- **Frontend**: React/Vite application
- **Infrastructure**: PostgreSQL databases, RabbitMQ message broker

### Root Cause Analysis Strategy
The 500 error on GET /api/orders suggests:
1. Query Service failing to process order creation events from RabbitMQ
2. Database projection errors in the read model updates
3. Serialization/deserialization issues with event messages
4. Missing event handlers or incorrect event routing

### Unified Deployment Architecture
```
┌─────────────────────────────────────────┐
│              Nginx (Port 80)            │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │   Static    │  │    API Proxy        ││
│  │   Files     │  │   /api/* → Services ││
│  │   (React)   │  │                     ││
│  └─────────────┘  └─────────────────────┘│
└─────────────────────────────────────────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
   ┌────▼────┐ ┌───▼───┐ ┌─────▼─────┐
   │Order    │ │Payment│ │Inventory  │
   │Service  │ │Service│ │Service    │
   │(8081)   │ │(8082) │ │(8083)     │
   └─────────┘ └───────┘ └───────────┘
        │           │           │
        └───────────┼───────────┘
                    │
              ┌─────▼─────┐
              │Query      │
              │Service    │
              │(8084)     │
              └───────────┘
```

## Components and Interfaces

### 1. Debugging Components

#### Enhanced Logging System
```java
// Order Service Event Publishing
@Component
public class OrderEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        logger.info("Publishing OrderCreatedEvent: orderId={}, correlationId={}", 
                   event.getOrderId(), event.getCorrelationId());
        // Publishing logic with error handling
    }
}

// Query Service Event Consumer
@RabbitListener(queues = "order.events.queue")
public void handleOrderCreated(OrderCreatedEvent event) {
    logger.info("Received OrderCreatedEvent: orderId={}, correlationId={}", 
               event.getOrderId(), event.getCorrelationId());
    try {
        // Event processing logic
        logger.info("Successfully processed OrderCreatedEvent: orderId={}", event.getOrderId());
    } catch (Exception e) {
        logger.error("Failed to process OrderCreatedEvent: orderId={}, error={}", 
                    event.getOrderId(), e.getMessage(), e);
        throw e; // Re-throw to trigger retry mechanism
    }
}
```

#### Frontend API Debugging
```typescript
// Enhanced API client with comprehensive logging
class ApiClient {
    async request<T>(config: RequestConfig): Promise<T> {
        console.log('🚀 API Request:', {
            method: config.method,
            url: config.url,
            headers: config.headers,
            data: config.data,
            timestamp: new Date().toISOString()
        });
        
        try {
            const response = await axios(config);
            console.log('✅ API Response:', {
                status: response.status,
                statusText: response.statusText,
                headers: response.headers,
                data: response.data,
                url: config.url,
                timestamp: new Date().toISOString()
            });
            return response.data;
        } catch (error) {
            console.error('❌ API Error:', {
                message: error.message,
                status: error.response?.status,
                statusText: error.response?.statusText,
                data: error.response?.data,
                url: config.url,
                timestamp: new Date().toISOString()
            });
            throw error;
        }
    }
}
```

### 2. Unified Deployment Components

#### Multi-Stage Dockerfile
```dockerfile
# Stage 1: Build Frontend
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Backend Services
FROM maven:3.8.6-openjdk-17 AS backend-builder
WORKDIR /app

# Build shared-events first
COPY shared-events/ ./shared-events/
WORKDIR /app/shared-events
RUN mvn clean install

# Build all services
WORKDIR /app
COPY services/ ./services/
COPY pom.xml ./
RUN mvn clean package -DskipTests

# Stage 3: Runtime
FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y nginx supervisor && rm -rf /var/lib/apt/lists/*

# Copy built artifacts
COPY --from=frontend-builder /app/frontend/dist /var/www/html
COPY --from=backend-builder /app/services/*/target/*.jar /app/services/
COPY nginx.conf /etc/nginx/nginx.conf
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

EXPOSE 80
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
```

#### Supervisor Configuration
```ini
[supervisord]
nodaemon=true
user=root

[program:nginx]
command=nginx -g "daemon off;"
autostart=true
autorestart=true
priority=100

[program:order-service]
command=java -jar /app/services/order-service.jar
autostart=true
autorestart=true
priority=200
environment=SERVER_PORT=8081,DATABASE_URL=%(ENV_ORDER_DB_URL)s

[program:payment-service]
command=java -jar /app/services/payment-service.jar
autostart=true
autorestart=true
priority=200
environment=SERVER_PORT=8082

[program:inventory-service]
command=java -jar /app/services/inventory-service.jar
autostart=true
autorestart=true
priority=200
environment=SERVER_PORT=8083

[program:query-service]
command=java -jar /app/services/query-service.jar
autostart=true
autorestart=true
priority=300
environment=SERVER_PORT=8084,DATABASE_URL=%(ENV_QUERY_DB_URL)s
```

#### Nginx Configuration
```nginx
events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    upstream order-service {
        server localhost:8081;
    }
    
    upstream payment-service {
        server localhost:8082;
    }
    
    upstream inventory-service {
        server localhost:8083;
    }
    
    upstream query-service {
        server localhost:8084;
    }

    server {
        listen 80;
        
        # Serve React frontend
        location / {
            root /var/www/html;
            try_files $uri $uri/ /index.html;
        }
        
        # API routing
        location /api/orders {
            proxy_pass http://query-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
        
        location /api/orders/create {
            proxy_pass http://order-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
        
        location /api/payments {
            proxy_pass http://payment-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
        
        location /api/inventory {
            proxy_pass http://inventory-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

## Data Models

### Event Processing Flow
```
Order Creation → Order Service → RabbitMQ → Query Service → Read Model Update
     ↓              ↓              ↓            ↓              ↓
  HTTP POST    Event Store    Event Queue   Event Handler   Database
```

### Error Handling Data Structure
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private String error;
    private String message;
    private String correlationId;
    private LocalDateTime timestamp;
    private String path;
    private int status;
    
    // Constructor and getters
}
```

## Error Handling

### 1. Query Service Event Processing
- **Retry Mechanism**: Implement exponential backoff for failed event processing
- **Dead Letter Queue**: Route failed events to DLQ for manual inspection
- **Circuit Breaker**: Prevent cascade failures when database is unavailable
- **Graceful Degradation**: Return cached data when real-time updates fail

### 2. Database Connection Resilience
```java
@Configuration
public class DatabaseConfig {
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setMaximumPoolSize(10);
        config.setLeakDetectionThreshold(60000);
        return new HikariDataSource(config);
    }
}
```

### 3. RabbitMQ Connection Management
```java
@Configuration
public class RabbitConfig {
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setConnectionTimeout(30000);
        factory.setRequestedHeartBeat(30);
        factory.setChannelCacheSize(25);
        return factory;
    }
}
```

## Testing Strategy

### 1. Debugging Verification
- **Unit Tests**: Test event serialization/deserialization
- **Integration Tests**: Verify RabbitMQ message flow
- **End-to-End Tests**: Create order and verify GET /api/orders returns data
- **Load Tests**: Ensure system handles concurrent order creation

### 2. Deployment Testing
- **Container Build Tests**: Verify multi-stage build completes successfully
- **Service Startup Tests**: Ensure all services start in correct order
- **Health Check Tests**: Verify all endpoints respond correctly
- **Proxy Tests**: Ensure Nginx routes requests to correct services

### 3. Monitoring and Observability
- **Correlation ID Tracking**: Trace requests across all services
- **Metrics Collection**: Monitor service health and performance
- **Log Aggregation**: Centralize logs for debugging
- **Alert Configuration**: Set up alerts for critical failures