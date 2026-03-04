# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

All commands are run from the **`unified-order-system/`** directory unless stated otherwise.

```bash
# Build the unified system
mvn clean install

# Run all unit tests
mvn clean test

# Run a single test class
mvn clean test -Dtest=OrderServiceTest

# Run integration tests (CompleteOrderFlowIntegrationTest et al.)
mvn clean test -Dtest=CompleteOrderFlowIntegrationTest

# Run all integration tests via failsafe
mvn verify

# Build without tests
mvn clean install -DskipTests

# Build from root (all modules including libs)
cd .. && mvn clean install -DskipTests
```

### Docker

```bash
# Copy env template first (one-time)
cp .env.example .env   # set JWT_SECRET_KEY (64 hex chars)

# Start infrastructure only (postgres, rabbitmq, redis)
docker compose -f docker-compose.yml up -d

# Start full system including unified-order-system
docker compose -f docker-compose.yml up -d --build unified-order-system

# Start observability stack (Prometheus, Grafana, Loki, Tempo)
docker compose -f docker-compose.observability.yml up -d
```

## Architecture

### Module Structure

The root pom.xml builds these modules:
- **`libs/`** — shared libraries installed to local Maven repo first
  - `common-events`: versioned event envelopes (`EventEnvelope`, `VersionedEvent`, `EventCompatibility`), RabbitMQ naming conventions
  - `common-security`: `SecurityAutoConfiguration` (auto-wires JWT filter, rate limiting, security properties)
  - `common-messaging`: `CorrelationId`, `MessagingConstants`, `MessagingAutoConfiguration`
  - `common-observability`: `CorrelationIdFilter`, `RabbitMqHeaders`
- **`shared-events/`** — legacy shared event payloads (not in active use; prefer `unified-order-system/shared/events/`)
- **`unified-order-system/`** — the active monolith (primary development target)
- **`services/`** — legacy microservices (order-service, payment-service, inventory-service, order-query-service); not in root pom and not actively built

### unified-order-system Package Layout

```
com.ordersystem.unified/
├── order/
│   ├── application/          # Use Cases: CreateOrderUseCase, CancelOrderUseCase
│   ├── domain/               # OrderBusinessRules (validation constants, transition rules)
│   ├── model/                # Order, OrderItemEntity JPA entities
│   ├── repository/           # OrderRepository
│   ├── dto/                  # Request/Response DTOs
│   ├── OrderController.java  # REST API
│   └── OrderService.java     # Simple CRUD service (bypasses use cases)
├── payment/
│   ├── domain/               # PaymentBusinessRules
│   ├── model/                # Payment JPA entity
│   ├── repository/           # PaymentRepository
│   ├── dto/                  # PaymentRequest, PaymentResponse, PaymentStatus, PaymentMethod
│   ├── PaymentController.java
│   └── PaymentService.java
├── inventory/
│   ├── domain/               # InventoryBusinessRules
│   ├── model/                # Inventory, Product, Reservation, ReservationItem, Stock
│   ├── repository/
│   ├── dto/
│   ├── service/              # Refactored inventory service layer
│   ├── InventoryController.java
│   └── InventoryService.java
├── infrastructure/
│   ├── events/               # EventPublisher (event sourcing), DomainEventEntity, DomainEventRepository
│   └── scheduler/            # Background jobs
├── shared/
│   ├── events/               # Domain event classes + OrderStatus enum
│   ├── exceptions/           # OrderSystemException hierarchy + ErrorResponse
│   ├── util/                 # SafeEnumParser
│   └── validation/
├── config/                   # Spring configs: WebConfig, CacheConfig, DatabaseConfig, etc.
├── web/                      # Thymeleaf controllers: DashboardController, PageController
└── websocket/                # WebSocketEventService
```

### Key Architectural Patterns

**Order flow (via `CreateOrderUseCase`)**: The use case orchestrates a Saga:
1. Validate via `OrderBusinessRules`
2. Reserve inventory via `InventoryService`
3. Process payment via `PaymentService`
4. Persist order + publish domain events via `EventPublisher`
5. On failure: compensating transactions (release inventory, refund payment)

**Note**: `OrderController` calls `OrderService` (simple CRUD) for basic operations and `CreateOrderUseCase`/`CancelOrderUseCase` for orchestrated flows. `OrderService` bypasses business rules — use the Use Cases when orchestration matters.

**Event Sourcing**: All domain events are persisted to the `domain_events` table via `EventPublisher`. The publisher uses `REQUIRES_NEW` propagation to persist events independently of the calling transaction. `EventPublisher.getAggregateId()` uses `instanceof` checks — **add new event types here manually** when creating new event classes.

**Exception hierarchy**: `OrderSystemException` is the base; `GlobalExceptionHandler` handles it via `ex.getHttpStatus()` and `ex.getErrorCode()`. Subclasses: `OrderNotFoundException`, `InvalidOrderException`, `InsufficientInventoryException`, `PaymentProcessingException`, etc.

**Security**: `libs/common-security` auto-configures JWT + rate limiting. In `application.yml`, `security.enforce-authentication: false` by default. Public paths are enumerated under `security.public-paths`. The `DatabaseConfig` bean only activates on profiles other than `test` and `docker` (uses raw H2 URL for those).

## Test Infrastructure

Tests live in `unified-order-system/src/test/`. The profile `test` (from `application-test.yml`) uses H2 in-memory with `ddl-auto: create-drop`.

```java
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)   // disables security filters
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)                   // required for context overrides
public class CompleteOrderFlowIntegrationTest { ... }
```

**Critical test properties**:
- `spring.main.allow-bean-definition-overriding: true` must be set in `application-test.yml` (already present) because `common-security` scans beans that conflict with test beans.
- `@AutoConfigureMockMvc(addFilters = false)` disables security in `MockMvc` tests.
- `PaymentService.simulatePaymentProcessing()` is deterministic (always succeeds) — do not introduce randomness.

## Important Business Rules

- **`OrderStatus.isTerminal()`**: Only `CANCELLED` and `FAILED` are terminal. `CONFIRMED` is **not** terminal — confirmed orders can still be cancelled.
- **`OrderBusinessRules` validation constants**: `MINIMUM_ORDER_VALUE = 10.00`, `MAXIMUM_ORDER_VALUE = 10_000_000.00`, `MINIMUM_ITEMS = 1`, `MAXIMUM_ITEMS = 100`. Tests expect these exact values.
- **Status transitions**: `PENDING → INVENTORY_RESERVED → PAYMENT_PROCESSING → CONFIRMED`. Terminal states cannot transition. `CONFIRMED → CANCELLED` is allowed.
- **`@JsonIgnoreProperties(ignoreUnknown = true)`**: Required on request DTOs when tests send extra fields.
- **Response fields**: `transactionId`, `reservationId`, and `paymentId` must be set in **all** `mapToResponse()` methods or tests will fail.

## Database Migrations

Flyway migrations are in `unified-order-system/src/main/resources/db/migration/`. They are **disabled** in the `dev` and `test` profiles (H2 uses `ddl-auto: create-drop`). Enabled in `production` profile against PostgreSQL.

Migration files follow the pattern `V{n}__{description}.sql`. The `postgres/` subdirectory contains PostgreSQL-specific variants.

## Observability Endpoints

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/prometheus`
- API docs: `GET /swagger-ui/index.html`
- Grafana: `http://localhost:3000` (admin/admin)
- RabbitMQ UI: `http://localhost:15672`
