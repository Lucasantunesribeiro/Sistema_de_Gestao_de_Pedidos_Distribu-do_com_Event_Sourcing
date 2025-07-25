# Technology Stack & Build System

## Core Technologies
- **Java 17**: Primary programming language with modern features
- **Spring Boot 3.1.5**: Main framework for all microservices
- **Maven**: Build system and dependency management
- **PostgreSQL 15**: Database for event store and read models
- **RabbitMQ**: Message broker for event-driven communication
- **Docker**: Containerization and local development environment

## Key Libraries & Frameworks
- **Jackson 2.15.2**: JSON serialization/deserialization
- **Spring Data JPA**: Database access and ORM
- **Spring AMQP**: RabbitMQ integration
- **Spring Boot Actuator**: Health checks and metrics
- **HikariCP**: Database connection pooling

## Build Commands

### Initial Setup
```bash
# Build shared events library first (required by all services)
cd shared-events
mvn clean install
cd ..
```

### Local Development with Docker
```bash
# Start entire system with infrastructure
docker-compose up --build

# Start only infrastructure (databases + RabbitMQ)
docker-compose up order-db query-db rabbitmq

# Build specific service
cd services/order-service
mvn clean package
```

### Testing
```bash
# Run tests for all modules
mvn clean test

# Run tests for specific service
cd services/order-service
mvn test

# Integration tests (requires running infrastructure)
mvn verify
```

### Service Management
```bash
# Health check all services
curl http://localhost:8081/api/orders/health    # Order Service
curl http://localhost:8082/api/payments/health  # Payment Service
curl http://localhost:8083/api/inventory/health # Inventory Service
curl http://localhost:8084/api/orders/health    # Query Service
```

## Development Environment
- **Java Version**: Exactly Java 17 (configured in all pom.xml files)
- **Maven Version**: 3.6+ required
- **Docker**: Required for local infrastructure
- **Ports Used**: 8081-8084 (services), 5432-5433 (databases), 5672/15672 (RabbitMQ)

## Architecture Patterns
- **Event Sourcing**: Events as source of truth in PostgreSQL
- **CQRS**: Separate command (Order Service) and query (Query Service) models
- **Saga Pattern**: Orchestrated distributed transactions
- **Circuit Breaker**: Resilience patterns with Spring Cloud
- **Fanout Exchange**: RabbitMQ pattern for event distribution

## Configuration Standards
- **Environment Variables**: Used for external dependencies (DB, RabbitMQ)
- **Application Properties**: YAML format preferred
- **Health Checks**: All services expose `/api/{service}/health` endpoints
- **Actuator**: Enabled for monitoring and metrics