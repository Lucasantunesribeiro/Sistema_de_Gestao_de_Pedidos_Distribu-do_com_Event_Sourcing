# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

All commands run from the **project root** unless noted. The active module is `unified-order-system/`.

```bash
# Install shared libraries (required before building the main module)
mvn -pl libs/common-events,libs/common-security,libs/common-messaging,libs/common-observability,shared-events -am -DskipTests install

# Build and test the main module
mvn -f unified-order-system/pom.xml clean test -B

# Run all tests (from inside unified-order-system/)
cd unified-order-system && mvn test -B

# Run a single test class
mvn -f unified-order-system/pom.xml test -Dtest=ClassName -B

# Run a single test method
mvn -f unified-order-system/pom.xml test -Dtest=ClassName#methodName -B

# Full build with Docker
docker-compose up --build
```

JaCoCo enforces a **60% coverage minimum** (excluding `performance/**` and `load/**`).

## Architecture Overview

### Module Layout

```
libs/
  common-events/       # Versioned event envelopes, queue naming conventions
  common-security/     # JWT auth, rate limiting, security properties
  common-messaging/    # Message correlation, auto-configuration
  common-observability/ # Log correlation, tracing headers
shared-events/         # Legacy event payloads (pre-consolidation)
unified-order-system/  # Main application (monolith consolidation of microservices)
services/              # Legacy microservices (not actively developed)
observability/         # Prometheus, Grafana, Loki, Tempo configs
tests/                 # E2E (Playwright) and load tests (k6)
```

### unified-order-system Package Structure

`com.ordersystem.unified` contains:

| Package | Purpose |
|---|---|
| `order/` | Order lifecycle: CRUD, status transitions |
| `payment/` | Payment processing |
| `inventory/` | Stock reservations, product management |
| `orchestration/` | Saga pattern for distributed transactions |
| `query/` | Read models (CQRS) |
| `infrastructure/events/` | `EventPublisher`, `DomainEventEntity`, event persistence |
| `infrastructure/scheduler/` | `ReservationExpiryScheduler` (disabled in tests) |
| `shared/events/` | Domain event types (OrderCreatedEvent, etc.) |
| `shared/exceptions/` | Exception hierarchy rooted at `OrderSystemException` |
| `config/` | Spring config, `GlobalExceptionHandler`, security setup |
| `web/` | Thymeleaf dashboard |
| `websocket/` | Real-time event streaming |

### Key Patterns

**Clean Architecture**: Each domain (order, payment, inventory) has:
- `application/` — use cases
- `domain/` — business rules (`OrderBusinessRules`, etc.)
- `dto/` — request/response objects
- `model/` — JPA entities
- `repository/` — Spring Data JPA interfaces

**Event Sourcing**: Every domain event is persisted to `domain_events` table via `EventPublisher`. When adding new event types, update `EventPublisher.getAggregateId()` which uses `instanceof` checks.

**Saga Orchestration**: `orchestration/` coordinates order→payment→inventory flows and compensations.

**Database migrations**: Flyway under `src/main/resources/db/migration/`. H2 in tests (`create-drop`), PostgreSQL in production.

## Critical Implementation Notes

### Jackson / DTO Pitfalls
- Add `@JsonIgnoreProperties(ignoreUnknown = true)` to **all** response DTOs. Jackson serializes `isX()` boolean methods as fields (e.g., `isSuccess()` → `"success": true`), which causes deserialization failures if the receiving DTO lacks that field.
- Use `PaymentMethod.name()` not `.toString()` for enum-in-string contexts — `toString()` returns a display name with spaces.

### JPA / Hibernate
- **Double-save bug**: calling `repository.save(entity)`, then modifying the returned entity, then saving again causes Hibernate to schedule both INSERT (with `@CreationTimestamp`) and UPDATE (with null `createdAt`). Fix: single save at the end, or use `orElse(new Entity())` + one `save`.
- `Product` has `@OneToMany(cascade = CascadeType.ALL)` on stocks — this affects flush behavior.
- Use `@Version` on entities for optimistic locking in concurrent scenarios.

### Test Configuration
- Security filters must be disabled with `@AutoConfigureMockMvc(addFilters = false)` — **not** `@AutoConfigureWebMvc`.
- Test profile (`@ActiveProfiles("test")`) requires `spring.main.allow-bean-definition-overriding: true` due to `common-security` bean scanning conflicts.
- Tests use `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` — commit all changes before CI to avoid ordering issues.
- `TestConfig.java` provides `MeterRegistry` bean; import it via `@Import(TestConfig.class)` when needed.

### Business Rules
- `OrderStatus.isTerminal()` controls which orders can be cancelled — `CONFIRMED` must **not** be terminal.
- `OrderBusinessRules` defines `MAX_QUANTITY` and `MAXIMUM_ORDER_VALUE` constants — tests must align with these.

### Infrastructure
- Docker Compose services: PostgreSQL 15, RabbitMQ 3.11, Redis Alpine, unified-order-system.
- Production environment variables come from a `.env` file (see `.env.example`).
- JVM in Docker: `-Xms256m -Xmx512m`, G1GC, Alpine JRE 17.
