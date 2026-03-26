# OrderFlow — Distributed Order Management System

A production-grade modular monolith built with Java 17, Spring Boot 3, PostgreSQL, Redis and RabbitMQ. Demonstrates Event Sourcing, Clean Architecture, Saga orchestration and a full Angular 17 frontend — deployed continuously to AWS EC2 via GitHub Actions.

## Live Demo

| Resource | URL |
|---|---|
| **Frontend (Angular)** | http://98.92.208.98:4200 |
| **API Health** | http://98.92.208.98:8080/actuator/health |
| **Swagger / OpenAPI** | http://98.92.208.98:8080/swagger-ui/index.html |

Login with the default admin credentials configured in `.env.example` (`admin` / `change-this-admin-password`) or the credentials configured in the running instance.

## What this project demonstrates

- **Event Sourcing** — every domain event is persisted to the `domain_events` table for full audit trail and replay
- **Clean Architecture** — each bounded context (`order/`, `payment/`, `inventory/`) has its own `application/`, `domain/`, `dto/`, `model/`, `repository/` layers
- **Saga orchestration** — `orchestration/` coordinates the order→inventory→payment flow and triggers compensating transactions on failure
- **CQRS** — `query/` module provides read models separate from write operations
- **JWT security** — stateless authentication via `common-security` library with rate limiting and CORS configuration
- **Observability** — Micrometer Tracing (Brave → Tempo/Zipkin), Prometheus metrics, Grafana/Loki/Tempo stack in `docker-compose.observability.yml`
- **Outbox pattern** — `DomainEventOutboxPublisher` dispatches domain events to RabbitMQ reliably via transactional outbox
- **CI/CD pipeline** — GitHub Actions runs 238+ integration tests (Testcontainers + PostgreSQL), Angular unit tests, Playwright E2E smoke tests and auto-deploys to EC2 on every push to `main`

## Repository layout

```
libs/
  common-events/        # Versioned event envelopes, queue naming conventions
  common-security/      # JWT auth, rate limiting, security properties
  common-messaging/     # RabbitMQ auto-configuration, outbox dispatch
  common-observability/ # Log correlation, tracing headers
unified-order-system/   # Main application — active runtime
frontend/               # Angular 17 dashboard (orders, inventory, real-time WebSocket)
observability/          # Prometheus, Grafana, Loki, Tempo configs
tests/                  # E2E (Playwright) and load tests (k6)
legacy/                 # Historical microservices — not in the active pipeline
```

## Running locally

```bash
# 1. Copy and fill the environment file
cp .env.example .env
# Set SECURITY_SECRET and JWT_SECRET_KEY to any 64-char hex string:
bash scripts/generate-jwt-secret.sh

# 2. Start the full stack (backend + frontend + infra)
docker compose up -d --build

# 3. Open in browser
open http://localhost:4200
```

Optional — observability stack (Grafana, Loki, Tempo, Prometheus):
```bash
docker compose -f docker-compose.observability.yml up -d
# Grafana: http://localhost:3000  (admin/admin)
```

## Build and test (Maven)

```bash
# Install shared libraries (required first)
mvn -pl libs/common-events,libs/common-security,libs/common-messaging,libs/common-observability -am -DskipTests install

# Run all integration tests + JaCoCo coverage gate (60% minimum)
mvn -f unified-order-system/pom.xml clean verify -B

# Run a single test class
mvn -f unified-order-system/pom.xml test -Dtest=CompleteOrderFlowIntegrationTest -B

# Run a single test method
mvn -f unified-order-system/pom.xml test -Dtest=ClassName#methodName -B
```

## Frontend (Angular 17)

```bash
cd frontend
npm ci
npm start           # dev server at http://localhost:4200 (proxies /api → :8080)
npm run test:ci     # headless unit tests via Karma + Chrome
npm run build       # production build to dist/
```

## Key endpoints

| Endpoint | Description |
|---|---|
| `POST /api/auth/login` | JWT authentication |
| `GET  /api/orders` | List orders (paginated) |
| `POST /api/orders` | Create order (triggers Saga) |
| `DELETE /api/orders/{id}` | Cancel order (compensation) |
| `GET  /api/inventory` | Inventory status |
| `GET  /api/health` | Detailed service health |
| `GET  /actuator/health` | Spring Boot actuator health |
| `WS   /ws` | WebSocket (SockJS/STOMP) — topics: `/topic/orders`, `/topic/inventory`, `/topic/payments` |

## Architecture overview

```
HTTP → Spring Security (JWT) → REST Controllers
                                     ↓
                              Use Cases (application/)
                                     ↓
                         Domain rules (domain/)  +  JPA Entities (model/)
                                     ↓
                         EventPublisher → domain_events table (Event Sourcing)
                                     ↓
                      DomainEventOutboxPublisher → RabbitMQ (Outbox pattern)
```

Order lifecycle: `PENDING → INVENTORY_RESERVED → PAYMENT_PROCESSING → CONFIRMED`
Cancellation triggers: inventory release + payment refund (compensating transactions)

## CI/CD

- **backend-verify**: installs shared libs → `mvn clean verify` (238+ tests, Testcontainers + PostgreSQL, JaCoCo gate)
- **frontend-unit**: `npm ci` → Angular unit tests → production build
- **frontend-e2e**: full Docker Compose stack → Playwright smoke tests
- **deploy-ec2**: SSH deploy to AWS EC2 on every successful `main` push

## Load testing (k6)

```bash
k6 run tests/k6/load-test.js
k6 run --vus 50 --duration 60s tests/k6/load-test.js
BASE_URL=http://98.92.208.98 k6 run tests/k6/load-test.js
```

## Environment variables

| Variable | Description |
|---|---|
| `SECURITY_SECRET` / `JWT_SECRET_KEY` | JWT signing key (64+ hex chars) |
| `POSTGRES_*` | PostgreSQL connection |
| `SPRING_RABBITMQ_*` | RabbitMQ connection |
| `REDIS_HOST`, `REDIS_PORT` | Redis connection |
| `PAYMENT_GATEWAY_BASE_URL` | WireMock payment sandbox URL |
| `SECURITY_CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins |
| `SECURITY_BOOTSTRAP_ADMIN_*` | Auto-created admin user on first boot |

> **Never commit `.env`** — use `bash scripts/generate-jwt-secret.sh` to generate secrets.
