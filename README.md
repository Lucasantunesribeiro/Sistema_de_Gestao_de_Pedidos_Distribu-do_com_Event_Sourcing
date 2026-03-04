Distributed Order System (Java/Spring)

Modular monolith for a distributed order management system built with Java 17, Spring Boot 3, PostgreSQL, RabbitMQ, and Redis. Implements Event Sourcing, CQRS, Clean Architecture, and Saga pattern for distributed transaction management.

Requirements
- Java 17
- Maven 3.9+
- Docker + Docker Compose v2

Repository layout
- libs/: shared libraries (common-events, common-security, common-messaging, common-observability)
- shared-events/: legacy shared event payloads
- unified-order-system/: main modular monolith (primary module)
- services/: legacy microservices (order-service, payment-service, inventory-service, order-query-service)
- observability/: Prometheus, Grafana, Loki, Tempo configurations
- tests/: end-to-end tests (Playwright) and k6 load tests

Local start (Docker Compose)
1) Copy env template: `cp .env.example .env`
2) Set `JWT_SECRET_KEY` (64 hex chars) and keep `.env` untracked.
3) Optional: set `COMPOSE_PROJECT_NAME=ordersystem` to avoid name collisions.
4) Infrastructure only (postgres, rabbitmq, redis):
   `docker compose -f docker-compose.yml up -d`
5) Full app (unified backend):
   `docker compose -f docker-compose.yml up -d --build unified-order-system`
6) With observability stack:
   `docker compose -f docker-compose.observability.yml up -d`

Build and test
- Build all modules: `mvn clean install -DskipTests`
- Run tests (from unified-order-system/): `mvn clean test`
- Run single test: `mvn clean test -Dtest=CompleteOrderFlowIntegrationTest`
- Build Docker image: `docker build -t unified-order-system:latest -f unified-order-system/Dockerfile .`

Environment variables
- POSTGRES_HOST / POSTGRES_PORT / POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD
- SPRING_RABBITMQ_HOST / SPRING_RABBITMQ_PORT / SPRING_RABBITMQ_USERNAME / SPRING_RABBITMQ_PASSWORD
- JWT_SECRET_KEY (64 hex chars)
- SECURITY_SECRET (same value as JWT_SECRET_KEY)
- SECURITY_ENFORCE_AUTH (true for production)
- SECURITY_CORS_ALLOWED_ORIGINS
- REDIS_HOST / REDIS_PORT / REDIS_ENABLED

Validation
- `docker compose ps`
- `docker compose -f docker-compose.yml exec postgres pg_isready -U postgres -d ordersystem`
- Rabbit UI: http://localhost:15672
- Application health: http://localhost:8080/actuator/health
- API docs (Swagger): http://localhost:8080/swagger-ui/index.html
- Grafana: http://localhost:3000 (admin/admin)

Architecture
The system uses Clean Architecture within a modular monolith:
- order/application/: Use Cases (CreateOrderUseCase, CancelOrderUseCase) implementing Saga pattern
- order/domain/: business rules and validations (OrderBusinessRules)
- infrastructure/events/: Event Sourcing via EventPublisher (persists all domain events to domain_events table)
- shared/events/: domain event classes (OrderCreatedEvent, PaymentProcessedEvent, InventoryReservedEvent, etc.)
- config/: Spring configuration (security, cache, database, metrics, CORS)

Order flow: PENDING → INVENTORY_RESERVED → PAYMENT_PROCESSING → CONFIRMED
Cancellation triggers compensating transactions (inventory release + payment refund).

Notes
- Do not commit `.env` or any secrets.
- Generate a secure JWT_SECRET_KEY: `bash scripts/generate-jwt-secret.sh`
- The Docker build requires the root context (libs, shared-events, and unified-order-system).
- For production: set `SECURITY_ENFORCE_AUTH=true` and use a secrets manager.
