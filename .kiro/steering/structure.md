# Project Structure & Organization

## Root Level Structure
```
├── services/                    # All microservices
│   ├── order-service/          # Command side - order management
│   ├── payment-service/        # Payment processing
│   ├── inventory-service/      # Stock management
│   └── order-query-service/    # Query side - read models
├── shared-events/              # Shared event classes library
├── scripts/                    # Deployment and utility scripts
├── .kiro/                      # Kiro IDE configuration
├── .github/                    # GitHub Actions CI/CD
├── docker-compose.yml          # Local development environment
└── pom.xml                     # Parent Maven configuration
```

## Service Organization Pattern
Each service follows standard Spring Boot structure:
```
services/{service-name}/
├── src/main/java/com/ordersystem/{service}/
│   ├── config/                 # Configuration classes
│   ├── controller/             # REST controllers
│   ├── service/                # Business logic
│   ├── repository/             # Data access
│   ├── model/                  # Domain models
│   ├── event/                  # Event handlers
│   └── Application.java        # Main class
├── src/main/resources/
│   ├── application.yml         # Configuration
│   └── db/migration/           # Database migrations (if applicable)
├── src/test/                   # Unit and integration tests
├── Dockerfile                  # Container configuration
└── pom.xml                     # Service dependencies
```

## Shared Components
- **shared-events/**: Contains all event classes used across services
  - Must be built first: `mvn clean install`
  - All services depend on this module
  - Contains domain events, DTOs, and common utilities

## Service Responsibilities

### order-service (Port 8081)
- **Command Side**: Handles order creation and updates
- **Event Store**: Persists events using Event Sourcing
- **Saga Orchestration**: Coordinates distributed transactions
- **Database**: PostgreSQL (order-db:5432)

### payment-service (Port 8082)
- **Payment Processing**: Simulated payment gateway
- **Event Consumer**: Reacts to order events
- **Stateless**: No persistent storage, event-driven only

### inventory-service (Port 8083)
- **Stock Management**: Reserves and confirms inventory
- **In-Memory Storage**: For demonstration purposes
- **Event-Driven**: Reacts to order and payment events

### order-query-service (Port 8084)
- **Query Side**: Optimized read models for CQRS
- **Event Projection**: Builds views from all system events
- **Database**: PostgreSQL (query-db:5433)
- **Caching**: Optimized for read operations

## Configuration Conventions

### Environment Variables
- `DATABASE_URL`: JDBC connection string
- `DATABASE_USERNAME/PASSWORD`: Database credentials
- `RABBITMQ_HOST/PORT/USERNAME/PASSWORD`: Message broker config

### Port Allocation
- **8081**: Order Service (Command)
- **8082**: Payment Service
- **8083**: Inventory Service
- **8084**: Order Query Service (Query)
- **5432**: Order Database
- **5433**: Query Database
- **5672/15672**: RabbitMQ (AMQP/Management)

## Development Workflow

### Adding New Features
1. Update shared-events if new events needed
2. Implement in appropriate service(s)
3. Update event handlers in consuming services
4. Add integration tests
5. Update docker-compose if needed

### Database Changes
- **Order Service**: Use Event Sourcing, avoid schema changes
- **Query Service**: Update read models and projections
- **Migrations**: Use Flyway or Liquibase for schema evolution

### Event-Driven Development
- All inter-service communication via RabbitMQ events
- No direct service-to-service HTTP calls
- Events are immutable and versioned
- Use correlation IDs for tracing