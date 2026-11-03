# OrderFlow — Sistema de Gestão de Pedidos com Event Sourcing

Monólito modular em Java 17 / Spring Boot 3.1 com PostgreSQL, Redis e RabbitMQ, mais um frontend
Angular 17. Pedidos passam por reserva de estoque e pagamento (gateway simulado por WireMock), os
eventos de domínio são gravados num event store versionado que também é outbox, e um consumidor
RabbitMQ idempotente mantém uma projeção reconstruível por replay.

## O que o código faz

- **Pedidos** (`order/`): criação com reserva de estoque e pagamento na mesma transação
  (`CreateOrderUseCase`), cancelamento com liberação de estoque e estorno (`CancelOrderUseCase`).
  Ciclo de vida: `PENDING -> INVENTORY_RESERVED -> PAYMENT_PROCESSING -> CONFIRMED`; `CANCELLED` e
  `FAILED` são terminais. A máquina de estados está em `OrderStatus.canTransitionTo`.
- **Event store** (`infrastructure/events/`): tabela `domain_events`, um stream por
  `(aggregate_type, aggregate_id)` com `aggregate_version` sequencial e restrição única. Gravar com
  versão esperada desatualizada gera `409 CONCURRENCY_CONFLICT`, nunca sobrescrita.
- **Agregado event-sourced** (`order/domain/OrderAggregate`): reconstruído aplicando os eventos em
  ordem de versão; lacunas, duplicatas ou transições impossíveis no stream são detectadas.
  `GET /api/orders/{id}/replay` mostra o estado reconstruído e se ele bate com `system_orders`.
- **Outbox** (`DomainEventOutboxPublisher`): a cada 5 s publica eventos pendentes no RabbitMQ
  (`order.exchange`, `payment.exchange`, `inventory.exchange`) com `messageId` = id do evento.
  Se o envio da versão N de um stream falha, as versões seguintes do mesmo stream esperam o próximo
  ciclo (outros streams seguem), para o consumidor não receber N+1 antes de N.
- **Consumidor idempotente** (`query/projection/`): lê `order.projection.queue`, deduplica por
  `eventId` em `processed_events`, aplica na tabela `order_projection` e só faz ack após o commit.
  Falhas: 3 tentativas e depois `order.projection.dlq`; mensagem malformada vai direto para a DLQ
  (sem retry). `POST /api/projections/orders/rebuild`
  reconstrói a projeção por replay determinístico.
- **Estoque** (`inventory/`) e **pagamentos** (`payment/`, cliente HTTP para o sandbox WireMock em
  `infra/payment-sandbox` ou cliente simulado quando o gateway está desligado).
- **Segurança**: JWT stateless, rate limiting e CORS pela lib `libs/common-security`; usuário admin
  criado no primeiro boot quando `SECURITY_BOOTSTRAP_ADMIN_ENABLED=true`.
- **Observabilidade**: métricas Prometheus em `/actuator/prometheus`, tracing Micrometer/Brave
  (exportação Zipkin para Tempo) e stack opcional em `docker-compose.observability.yml`.

## Arquitetura

```mermaid
flowchart LR
    UI[Frontend Angular<br/>nginx :4200] -->|/api, /ws| API

    subgraph APP[unified-order-system :8080]
        API[REST controllers<br/>JWT] --> UC[Casos de uso<br/>CreateOrder / CancelOrder]
        UC --> AGG[OrderAggregate<br/>máquina de estados]
        UC --> INV[InventoryService]
        UC --> PAY[PaymentService]
        AGG --> ES[EventStore<br/>versão esperada]
        OUT[DomainEventOutboxPublisher<br/>polling 5 s]
        LST[OrderProjectionListener]
        IDEM[IdempotentMessageProcessor]
        PROJ[OrderProjector]
        LST --> IDEM --> PROJ
    end

    subgraph PG[(PostgreSQL)]
        ORD[system_orders / order_items]
        EV[domain_events<br/>event store + outbox]
        PE[processed_events]
        OP[order_projection]
    end

    UC --> ORD
    ES --> EV
    PAY -->|HTTP| WM[WireMock<br/>payment-sandbox]
    OUT -->|lê pendentes| EV
    OUT -->|publica| X{{order.exchange}}
    X -->|order.#| Q[order.projection.queue]
    X -->|order.#| AQ[order.audit.queue<br/>sem consumidor]
    Q --> LST
    Q -. reject após 3 tentativas .-> DLX{{order.dlx}} --> DLQ[order.projection.dlq]
    IDEM --> PE
    PROJ --> OP
    API -. cache .-> R[(Redis)]
```

Decisões registradas em [`docs/adr/`](docs/adr/).

## Estrutura

```
unified-order-system/   # aplicação ativa (Spring Boot)
libs/
  common-events/        # envelopes de evento versionados, nomes de filas
  common-security/      # JWT, rate limiting, propriedades de segurança
  common-messaging/     # auto-configuração RabbitMQ (template, factory com retry/DLQ)
  common-observability/ # correlação de logs e headers de tracing
frontend/               # Angular 17
infra/payment-sandbox/  # mapeamentos WireMock do gateway de pagamento
observability/          # Prometheus, Grafana, Loki, Tempo, Alertmanager
tests/e2e/              # Playwright (smoke)
tests/k6/               # teste de carga k6
legacy/                 # microsserviços históricos — fora do build, CI e compose
```

## Como rodar

### Docker Compose (stack completa)

```bash
cp .env.example .env
# preencha SECURITY_SECRET (obrigatório no compose) e JWT_SECRET_KEY, por exemplo:
openssl rand -hex 32
docker compose up -d --build
```

- Frontend: http://localhost:4200 (login com `SECURITY_BOOTSTRAP_ADMIN_USERNAME` /
  `SECURITY_BOOTSTRAP_ADMIN_PASSWORD` do `.env`)
- API: http://localhost:8080 — Swagger em `/swagger-ui/index.html`, saúde em `/actuator/health`
- RabbitMQ management: http://localhost:15672

Stack de observabilidade (opcional, depende dos serviços do compose principal):

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d
# Grafana: http://localhost:3000
```

### Local (sem Docker)

O perfil padrão é `dev`: H2 em memória, Flyway desligado, schema criado pelo Hibernate, cache
simples, outbox e consumidor desligados (não precisa de RabbitMQ nem Redis).

```bash
mvn -B -pl libs/common-events,libs/common-security,libs/common-messaging,libs/common-observability -am install
SECURITY_SECRET=$(openssl rand -hex 32) mvn -f unified-order-system/pom.xml spring-boot:run

cd frontend && npm ci && npm start   # http://localhost:4200, proxy de /api para :8080
```

Para rodar contra PostgreSQL/RabbitMQ locais use `SPRING_PROFILES_ACTIVE=docker` e as variáveis da
tabela abaixo.

## Variáveis de ambiente

| Variável | Uso | Padrão |
|---|---|---|
| `SECURITY_SECRET` / `JWT_SECRET_KEY` | Chave de assinatura JWT (obrigatória no compose) | — |
| `SECURITY_CORS_ALLOWED_ORIGINS` | Origens permitidas (vírgula) | `http://localhost:4200,http://localhost:8080` |
| `SECURITY_BOOTSTRAP_ADMIN_ENABLED` / `_USERNAME` / `_PASSWORD` / `_EMAIL` | Admin criado no primeiro boot | compose: `true` / `admin` / `change-this-admin-password` |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Banco do compose | `ordersystem` / `postgres` / `postgres` |
| `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD` | JDBC da aplicação (fora do compose) | H2 em memória |
| `SPRING_RABBITMQ_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | RabbitMQ | `localhost` / `5672` / `guest` / `guest` |
| `OUTBOX_ENABLED`, `OUTBOX_BATCH_SIZE`, `OUTBOX_FIXED_DELAY` | Publicação do outbox | `false` (perfil docker: `true`), `50`, `5000` ms |
| `ORDER_PROJECTION_CONSUMER_ENABLED` | Consumidor idempotente da projeção | `false` (perfil docker: `true`) |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`, `REDIS_ENABLED`, `CACHE_TYPE`, `CACHE_TTL` | Cache | `localhost` / `6379`, `false`, `simple`, `1h` |
| `PAYMENT_GATEWAY_ENABLED` / `_BASE_URL` / `_API_KEY` | Gateway de pagamento (WireMock) | `false` |
| `TRACING_ENABLED`, `TRACING_SAMPLE_RATE`, `ZIPKIN_BASE_URL` | Tracing | `false`, `1.0`, `http://localhost:9411` |
| `LOG_LEVEL`, `WEB_LOG_LEVEL`, `SQL_LOG_LEVEL` | Níveis de log | `INFO`, `WARN`, `WARN` |
| `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` | Stack de observabilidade | `admin` / `admin` |

`.env.example` traz apenas valores de exemplo; nunca commite o `.env`.

## Endpoints principais

| Endpoint | Descrição |
|---|---|
| `POST /api/auth/login` | Autenticação JWT |
| `GET /api/orders`, `GET /api/orders/{id}` | Consulta de pedidos |
| `POST /api/orders` | Cria pedido (estoque + pagamento) |
| `PUT /api/orders/{id}/cancel` ou `POST /api/orders/cancel/{id}` | Cancela com compensação |
| `GET /api/orders/{id}/replay` (ADMIN) | Estado reconstruído do event store vs. estado gravado |
| `POST /api/projections/orders/rebuild` (ADMIN) | Reconstrói `order_projection` por replay |
| `/api/inventory`, `/api/payments` | Estoque e pagamentos |
| `GET /actuator/health`, `/actuator/prometheus` | Saúde e métricas |
| `WS /ws` | WebSocket (SockJS/STOMP) |

## Testes

```bash
# libs (inclui os testes de common-security)
mvn -B -pl libs/common-events,libs/common-security,libs/common-messaging,libs/common-observability -am install

# unitários (surefire) + integração (failsafe, pacote integration/) + gate JaCoCo de 60% de linhas
mvn -B -f unified-order-system/pom.xml clean verify

# uma classe
mvn -B -f unified-order-system/pom.xml test -Dtest=IdempotentOrderProjectionConsumerTest
```

Testes que precisam de banco usam PostgreSQL real: Testcontainers (`postgres:16-alpine`) quando há
Docker, senão PostgreSQL embarcado (zonky). Force com `TEST_DB_PROVIDER=embedded` ou
`TEST_DB_PROVIDER=testcontainers`, ou aponte para um banco com `TEST_DB_URL`.

Testes do foco event sourcing / mensageria:

- `order/domain/OrderAggregateTest` — replay em ordem de versão, determinismo, lacunas/duplicatas,
  transições inválidas, ida e volta JSON.
- `eventsourcing/EventStoreConcurrencyTest` — versão esperada, conflito em vez de sobrescrita,
  restrição única.
- `eventsourcing/IdempotentOrderProjectionConsumerTest` — mesma mensagem 2x => 1 efeito, entregas
  duplicadas concorrentes, rollback do marcador quando o efeito falha, rebuild 2x idêntico.
- `eventsourcing/OrderUseCaseEventSourcingTest` — casos de uso gravando o stream e rollback conjunto
  de pedido + eventos.

Frontend: `cd frontend && npm ci && npm run test:ci && npm run build` (Karma precisa de Chrome).
E2E: `tests/e2e` (Playwright) roda no CI contra a stack do compose.

## CI/CD

`.github/workflows/ci.yml` roda em `pull_request` e `push` para `main`: build e testes das libs e
do backend (`mvn verify`), testes e build do frontend, e smoke E2E com Playwright sobre o compose.
`deploy-ec2.yml` faz deploy por SSH após CI verde de um **push** em `main` (ou manualmente); não
roda para pull requests.

**Deploy EC2 é legado.** O workflow foi mantido como estava (só ganhou a trava contra execuções
vindas de pull request), mas não há intenção de manter infraestrutura paga (AWS, Azure, Oracle).
Sem os secrets `EC2_*` configurados ele falha no passo de SSH. Alternativa gratuita sugerida, não
implementada: rodar a stack localmente com `docker compose up`; para uma demo pública, publicar só
o frontend estático (GitHub Pages) e expor a API de uma máquina própria via Cloudflare Tunnel
(camada gratuita), sem custo de nuvem. Qualquer opção de hospedagem gratuita tem limites de
memória/tempo que precisam ser avaliados para PostgreSQL + RabbitMQ + Redis.

## Limitações conhecidas

- Modelo híbrido: `system_orders` continua sendo o estado consultado pela API; o event store é usado
  para replay, auditoria e controle de concorrência. Não há snapshots.
- Pedidos criados antes do versionamento têm streams incompletos; o replay deles pode divergir da
  tabela (o endpoint de replay indica `matchesStoredState=false`).
- Streams `Payment` e `Inventory` são versionados, mas não têm agregado reconstruído nem checagem de
  versão esperada (append no fim do stream).
- Outbox sem *publisher confirms*: reenvio é possível (*at-least-once* do lado do banco), mas uma
  mensagem perdida pelo broker depois do envio fica marcada como publicada. Só existe um consumidor (projeção de
  pedidos); `*.audit.queue` não têm consumidor e acumulam mensagens.
- DLQ sem reprocessamento automático; `processed_events` não é expurgada.
- O rebuild da projeção deve ser feito com o consumidor pausado.
- `legacy/` não é compilado nem testado.
