# Descrição de Portfólio — OrderFlow v2

**Análise técnica baseada no repositório vigente em 12/03/2026.**

Projeto avaliado: **OrderFlow — Sistema de Gestão de Pedidos com Event Sourcing**

Leitura executiva: este repositório demonstra **maturidade de engenharia enterprise** em Java/Spring Boot. O runtime principal agora é um **monólito modular completo** com frontend Angular 17, fluxo de pedido fechado de ponta a ponta, tracing distribuído instrumentado, segurança endurecida e documentação alinhada ao código real. Classificação atual: **Pleno em Java/Spring**.

---

## 1. Visão Geral do Projeto

O sistema resolve o ciclo de vida completo de pedidos de e-commerce/backoffice:

- Criação de pedido com validação de domínio
- Reserva de estoque com controle de quantidade e persistência real
- Processamento de pagamento com estorno no cancelamento
- Cancelamento com transações compensatórias automáticas
- Dashboard operacional em tempo real via WebSocket
- Auditoria de eventos via event store (`domain_events`)
- Observabilidade completa: métricas, logs estruturados e tracing distribuído

**Stack principal:**

| Camada | Tecnologia |
|---|---|
| Backend | Java 17 + Spring Boot 3.1.5 |
| Frontend | Angular 17 (standalone components) |
| Banco | PostgreSQL (prod) / H2 (testes) |
| Schema | Flyway migrations |
| Real-time | WebSocket (SockJS + STOMP) |
| Tracing | Micrometer Tracing + Brave → Tempo |
| Métricas | Micrometer + Prometheus + Grafana |
| Logs | Logback estruturado + Loki |
| Testes | JUnit 5 + Mockito + 238 testes |
| Carga | k6 (smoke + load scenarios) |
| CI/CD | GitHub Actions + AWS EC2/ECR |
| Containers | Docker multi-stage + Docker Compose |

---

## 2. Arquitetura do Sistema

### 2.1 Estrutura geral

```
unified-order-system/   — backend (monólito modular, módulo principal)
frontend/               — Angular 17 SPA
libs/                   — bibliotecas compartilhadas (JWT, eventos, mensageria, observabilidade)
observability/          — Prometheus, Grafana, Loki, Tempo configs
tests/                  — E2E (Playwright) e carga (k6)
services/               — microserviços legados (não ativos no runtime principal)
```

### 2.2 Pacotes do backend (`com.ordersystem.unified`)

| Pacote | Responsabilidade |
|---|---|
| `order/application/` | Use cases: `CreateOrderUseCase`, `CancelOrderUseCase` |
| `order/domain/` | Regras de negócio: `OrderBusinessRules` |
| `order/model/` | Entidades JPA: `Order`, `OrderItemEntity` |
| `payment/` | `PaymentService` com processamento e estorno (`refundPayment`) |
| `inventory/` | `InventoryService` com reservas reais (modo mock para testes) |
| `infrastructure/events/` | `EventPublisher`, `DomainEventEntity`, event store |
| `shared/events/` | Eventos de domínio tipados (`OrderCreatedEvent`, etc.) |
| `config/` | Spring config, CORS, segurança, WebSocket |
| `websocket/` | `WebSocketEventService` com SimpMessagingTemplate |

### 2.3 Clean Architecture

O projeto implementa **Clean Architecture pragmática**:

- `application/` — casos de uso sem dependência de framework
- `domain/` — regras de negócio puras
- `model/` — entidades JPA (acoplamento intencional para pragmatismo)
- Separação explícita entre DTOs, entidades e controllers

**Limitação conhecida e aceita:** entidades de domínio são também entidades JPA. Decisão arquitetural para evitar mapeamento duplo em um projeto de portfólio.

### 2.4 Event Sourcing

Todos os eventos de domínio são persistidos em `domain_events` via `EventPublisher`:

```
Order criado → OrderCreatedEvent persisted
Inventário reservado → InventoryReservedEvent persisted
Pagamento processado → PaymentProcessedEvent persisted
Pedido confirmado → OrderStatusUpdatedEvent persisted
Pedido cancelado → OrderCancelledEvent persisted
Pagamento estornado → PaymentRefundedEvent persisted
```

`EventPublisher` usa `Propagation.REQUIRED` para eventos dentro de transações existentes (sem abertura de nova conexão, prevenindo pool exhaustion em cenários concorrentes).

### 2.5 Saga Pattern

`CreateOrderUseCase` orquestra:

1. Valida regras de negócio (`OrderBusinessRules`)
2. Persiste pedido como `PENDING`
3. Reserva estoque (`InventoryService.reserveItems(orderId, items)`)
4. Processa pagamento (`PaymentService.processPayment(...)`)
5. Confirma pedido como `CONFIRMED`

Compensação via `CancelOrderUseCase`:

1. Libera reserva de estoque (`releaseItems`)
2. Estorna pagamento (`refundPayment` → `Payment.markAsRefunded()`)
3. Marca pedido como `CANCELLED`
4. Persiste `OrderCancelledEvent`

---

## 3. Frontend Angular 17

### 3.1 Estrutura

```
frontend/
  src/app/
    core/
      services/     — OrderService, PaymentService, InventoryService, WebSocketService
      models/       — order.model.ts, payment.model.ts, inventory.model.ts
    features/
      dashboard/    — estatísticas com atualização em tempo real
      orders/       — list (paginação/filtro), detail (cancel), create (form)
      inventory/    — status de estoque com barras de progresso
    shared/
      components/   — loading-spinner, status-badge, toast
  nginx.conf        — SPA routing + proxy /api → backend + WebSocket upgrade
  Dockerfile        — multi-stage: node:20-alpine build → nginx:alpine serve
```

### 3.2 Funcionalidades

- **Dashboard**: total de pedidos, receita, taxa de confirmação, alertas de estoque
- **Pedidos**: listagem com paginação, filtro por status, formulário de criação, detalhe com cancelamento
- **Inventário**: visualização de stock por produto com indicadores visuais
- **WebSocket**: atualizações em tempo real via SockJS + STOMP (tópicos `/topic/orders`, `/topic/inventory`, `/topic/payments`)

### 3.3 Deploy

```bash
# Desenvolvimento
cd frontend && npm install && npm start  # proxy para localhost:8080

# Docker (incluso no docker-compose.yml)
docker compose up -d frontend           # nginx na porta 4200
```

---

## 4. Inventário — Implementação Real

`InventoryService` suporta dois modos via configuração:

**Produção (`inventory.mock-mode=false`):**
- Valida disponibilidade via `StockRepository` com pessimistic locking
- Cria `Reservation` + `ReservationItem` persistidos
- Atualiza contadores de stock
- `releaseReservation()` restaura stock
- `confirmReservation()` deduz permanentemente

**Teste (`inventory.mock-mode=true`, `inventory.default-stock=1000`):**
- Persiste apenas `Reservation` para audit trail
- Valida quantidade contra `defaultStock`
- Não toca tabelas de Stock

```yaml
# application-test.yml
inventory:
  mock-mode: true
  default-stock: 1000
```

---

## 5. Observabilidade

### 5.1 Métricas

- Micrometer + Prometheus (`/actuator/prometheus`)
- Grafana dashboards em `observability/`
- Métricas de JVM, HTTP, HikariCP e domínio

### 5.2 Logs

- Logback com JSON estruturado
- `correlationId` e `traceId` em MDC para rastreabilidade
- Loki como backend de logs

### 5.3 Tracing Distribuído

- **Micrometer Tracing** + **Brave** bridge
- Exporta spans para **Tempo** via endpoint Zipkin-compatível
- `TRACING_ENABLED=true` e `ZIPKIN_BASE_URL=http://tempo:9411` no compose
- Taxa de amostragem configurável via `TRACING_SAMPLE_RATE` (padrão: 100%)

---

## 6. Segurança

| Controle | Implementação |
|---|---|
| Autenticação | JWT via `common-security` lib |
| CORS | Env-variable-driven (`CORS_ALLOWED_ORIGINS`), headers restritos |
| Rate limiting | Presente em `common-security` |
| CSP headers | Configurado via `security.content-security-policy` |
| Endpoints debug | `TestController` isolado com `@Profile("!production")` |
| Schema | Flyway migrations (`DDL_AUTO=none` em produção) |

---

## 7. Testes

### 7.1 Cobertura

- **238 testes** em múltiplas classes
- JaCoCo com mínimo de 60% de cobertura
- Exclusões: `performance/**`, `load/**`

### 7.2 Tipos de teste

| Tipo | Exemplos |
|---|---|
| Unitário | `InventoryServiceTest`, `OrderServiceTest`, `PaymentServiceTest` |
| Integração | `CompleteOrderFlowIntegrationTest`, `OrderControllerTest` |
| Concorrência | `ConcurrentOrderCreationTest` |
| Resiliência | `ResilienceTest` (pool exhaustion, extreme load) |
| E2E | Playwright em `tests/e2e/` |

### 7.3 Testes de carga (k6)

```javascript
// tests/k6/load-test.js
scenarios: {
  smoke: { executor: 'constant-vus', vus: 5, duration: '30s' },
  load: { executor: 'ramping-vus', stages: [20→50→0 VUs em 2 min] }
}
thresholds: {
  http_req_duration: ['p(95)<2000'],
  http_req_failed:   ['rate<0.05'],
  order_creation_success: ['rate>0.90']
}
```

---

## 8. Fluxo Principal de Pedido

```
POST /api/orders
  → CreateOrderUseCase
    → OrderBusinessRules.validate()
    → Order(PENDING) persistido
    → InventoryService.reserveItems(orderId, items)
      → Reservation persistida (+ Stock atualizado em produção)
    → PaymentService.processPayment(...)
      → Payment(COMPLETED) persistida
    → Order(CONFIRMED) atualizado
    → OrderCreatedEvent publicado via EventPublisher
  ← OrderResponse 201

PUT /api/orders/{id}/cancel
  → CancelOrderUseCase
    → InventoryService.releaseItems(items, reservationId)
    → PaymentService.refundPayment(paymentId, reason)
      → Payment(REFUNDED) com REF-{id} transaction
    → Order(CANCELLED) com cancellationReason
    → OrderCancelledEvent publicado
  ← OrderResponse 200
```

---

## 9. Como Apresentar em Entrevista

### Pitch de 30 segundos

"Desenvolvi o OrderFlow, um sistema de gestão de pedidos com arquitetura de monólito modular em Spring Boot, frontend Angular 17 e observabilidade completa. O sistema cobre o ciclo de vida de pedidos com reserva real de estoque, pagamento com estorno e cancelamento com compensação automática. Tem event sourcing com trilha de auditoria, tracing distribuído com Tempo, 238 testes automatizados e deploy em Docker com CI/CD no GitHub Actions."

### Pitch técnico de 2 minutos

"O OrderFlow usa Clean Architecture dentro de um monólito modular em Spring Boot 3. O fluxo principal implementa o padrão Saga: `CreateOrderUseCase` coordena validação de domínio, reserva de estoque com pessimistic locking no banco, processamento de pagamento, confirmação do pedido e publicação de eventos de domínio no event store.

No cancelamento, `CancelOrderUseCase` executa compensação real: libera a reserva de estoque e chama `PaymentService.refundPayment()`, que persiste o status `REFUNDED` com um transaction ID rastreável.

O frontend é Angular 17 com componentes standalone, conectado ao backend via REST e WebSocket (SockJS+STOMP) para atualizações em tempo real. O stack de observabilidade usa Prometheus, Grafana, Loki e Tempo, com a aplicação instrumentada via Micrometer Tracing + Brave para tracing distribuído real. Tenho 238 testes automatizados cobrindo fluxos unitários, integração, concorrência e resiliência, mais scripts k6 para testes de carga."

---

## 10. Pontos Fortes

- Fluxo de pedido fechado de ponta a ponta (inventário real, estorno real, compensação real)
- Frontend Angular 17 com atualizações em tempo real via WebSocket
- Event sourcing com trilha de auditoria completa e `correlationId`
- Tracing distribuído instrumentado (Micrometer + Brave → Tempo)
- Segurança configurável por ambiente (CORS, JWT, CSP, TestController isolado)
- Flyway ativo, `DDL_AUTO=none` em produção
- 238 testes cobrindo unitário, integração, concorrência e resiliência
- k6 load tests com cenários e thresholds definidos
- CI/CD com GitHub Actions e deploy em AWS EC2/ECR
- Docker multi-stage para backend e frontend
- README e CLAUDE.md alinhados ao estado real do código

---

## 11. Pontos de Evolução Futura

- **Stack .NET**: migrar ou criar versão paralela em ASP.NET Core + EF Core para vagas C#
- **RabbitMQ runtime**: conectar o fluxo principal ao broker para arquitetura event-driven real
- **Testcontainers**: validar Flyway + PostgreSQL real no CI em vez de só H2
- **Kubernetes + Helm**: deploy container orquestrado
- **Terraform**: infraestrutura como código para AWS

---

## 12. Checklist de Mercado

| Requisito | Status | Observação |
|---|---|---|
| Java/Spring Boot | ✅ Sim | Runtime principal |
| Angular | ✅ Sim | Frontend Angular 17 completo |
| APIs REST | ✅ Sim | CRUD completo para pedidos, pagamentos, inventário |
| PostgreSQL | ✅ Sim | Banco alvo com Flyway |
| Docker | ✅ Sim | Multi-stage backend + frontend |
| CI/CD | ✅ Sim | GitHub Actions |
| AWS | ✅ Sim | EC2 + ECR |
| WebSocket | ✅ Sim | SockJS + STOMP |
| Event Sourcing | ✅ Sim | `domain_events` + `EventPublisher` |
| Outbox Pattern | ✅ Sim | `OrderService` publica eventos transacionalmente |
| Tracing | ✅ Sim | Micrometer + Brave → Tempo |
| Observabilidade | ✅ Sim | Prometheus + Grafana + Loki |
| Testes automatizados | ✅ Sim | 238 testes |
| Testes de carga | ✅ Sim | k6 com cenários smoke + load |
| Clean Architecture | ✅ Parcial | Pragmática, não estrita |
| DDD | ✅ Parcial | Bounded contexts, regras de domínio |
| Saga Pattern | ✅ Sim | CreateOrderUseCase + CancelOrderUseCase |
| Idempotência | ⚠️ Parcial | Proteção simples em pagamento |
| RabbitMQ runtime | ⚠️ Parcial | Infra presente, não conectado ao fluxo principal |
| .NET / C# | ❌ Não | Gap para vagas C# |
| Kubernetes | ❌ Não | Não implementado |
| Terraform | ❌ Não | Não implementado |

---

## 13. Score Final

**Nota: 8,1 / 10**

| Dimensão | Nota | Justificativa |
|---|---|---|
| Arquitetura | 8,0 | Monólito modular sólido, Clean Architecture pragmática, Saga bem implementado |
| Engenharia backend | 8,5 | Fluxo fechado, inventário real, estorno real, eventos, tracing |
| Frontend | 7,5 | Angular 17 completo com WebSocket; sem testes de componente ainda |
| Observabilidade | 8,5 | Tracing + métricas + logs — raro em portfólio |
| Segurança | 8,0 | CORS configurável, JWT, CSP, isolamento de debug endpoint |
| Testes | 8,0 | 238 testes + k6; falta Testcontainers com PostgreSQL real |
| Documentação | 8,0 | README e CLAUDE.md alinhados ao código |
| Relevância para vagas Java | 9,0 | Referência forte para posições backend Java pleno |
| Relevância para vagas .NET | 4,0 | Stack não é C#; apresentar como prova de arquitetura |

**Classificação: Pleno em Java/Spring**

Para vagas de backend Java/Spring Boot, este projeto é um ativo de portfólio competitivo no mercado brasileiro. Demonstra domínio de temas enterprise que aparecem em processos seletivos de empresas de médio e grande porte.
