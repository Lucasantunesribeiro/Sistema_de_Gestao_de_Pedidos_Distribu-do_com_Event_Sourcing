# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

All commands run from the **project root** unless noted. The active module is `unified-order-system/`.

```bash
# Install shared libraries (required before building the main module)
mvn -pl libs/common-events,libs/common-security,libs/common-messaging,libs/common-observability -am -DskipTests install

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
unified-order-system/  # Main application (monolith consolidation of microservices)
legacy/shared-events/  # Legacy event payloads (pre-consolidation)
legacy/services/       # Legacy microservices (not actively developed)
observability/         # Prometheus, Grafana, Loki, Tempo configs
tests/                 # E2E (Playwright) and load tests (k6)
```

`legacy/` holds the historical microservices and their old shared contracts. The active runtime and build pipeline are `unified-order-system/` plus `frontend/`.

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
| `web/` | Thymeleaf legacy views (superseded by Angular frontend) |
| `websocket/` | Real-time event streaming via SockJS + STOMP |

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
- Docker Compose services: PostgreSQL 15, RabbitMQ 3.11 (infra only—not used at runtime), Redis Alpine, `unified-order-system`, `frontend` (Angular/Nginx on port 4200).
- Production environment variables come from a `.env` file (see `.env.example`).
- JVM in Docker: `-Xms256m -Xmx512m`, G1GC, Alpine JRE 17.
- Frontend: Angular 17 at `frontend/`. Dev server: `cd frontend && npm start` (proxies `/api` → `localhost:8080`). Docker: `frontend/Dockerfile` (multi-stage node build → nginx).
- WebSocket: `/ws` endpoint (SockJS + STOMP). Topics: `/topic/orders`, `/topic/inventory`, `/topic/payments`. Config: `WebSocketConfig.java`.
- CORS: driven by `CORS_ALLOWED_ORIGINS` env var (default: `http://localhost:4200,http://localhost:8080`).

## Available MCP Servers

This workspace has the following MCP servers configured. Use them for their specific capabilities:

| MCP Server | When to Use |
|---|---|
| `context7` | Fetch up-to-date library documentation (Spring Boot, Angular, Hibernate, etc.) |
| `Ref` | Read/search documentation URLs for APIs and frameworks |
| `awslabs-cfn` | Create/update/delete AWS CloudFormation resources |
| `awslabs-iam` | Manage IAM users, roles, policies, access keys |
| `awslabs-dynamodb` | Design, model, and validate DynamoDB schemas |
| `awslabs-lambda` | Invoke AWS Lambda functions |
| `awslabs-docs` | Search and read AWS official documentation |
| `supabase` | Manage Supabase projects, run SQL, deploy edge functions |
| `notebooklm-mcp` | Create Google NotebookLM notebooks, add sources, generate audio |
| `playwright` | Browser automation for E2E testing and UI interaction |
| `chrome-devtools` | Browser DevTools: screenshots, performance, console, network |
| `firecrawl-mcp` | Web scraping, crawling, search |
| `shadcn-ui` | Get shadcn/ui component code |
| `magic-mcp` | Generate UI components via 21st.dev |
| `netlify` | Deploy and manage Netlify projects |
| `sequential-thinking` | Break complex problems into sequential reasoning steps |

## Specialized Agents

Use the `Agent` tool with these `subagent_type` values for specialized tasks:

| Agent | When to Use |
|---|---|
| `lucas-frontend-engineer` | Build/review Angular/React components, pages, services, UX patterns |
| `backend-architect` | Implement/review REST endpoints, use cases, JPA entities, Clean Architecture |
| `postgres-architect` | Design/optimize PostgreSQL schemas, Flyway migrations, query performance |
| `qa-engineer` | Create test plans, write unit/integration tests, review test coverage |
| `security-hardening-validator` | Review auth, CORS, input validation, JWT, API security |
| `devops-deploy-architect` | Design CI/CD pipelines, Docker configs, production readiness checklists |
| `sre-observability` | Add logging, metrics, tracing, health checks, alerts |
| `architecture-advisor` | Architectural decisions, refactoring strategy, trade-off analysis |
| `code-quality-reviewer` | Code review before merge: patterns, production readiness |
| `llm-integration-architect` | Integrate LLM/AI features with guardrails and cost optimization |
| `Explore` | Explore codebase: find files by pattern, search for keywords |
| `Plan` | Design implementation plans for complex tasks |
