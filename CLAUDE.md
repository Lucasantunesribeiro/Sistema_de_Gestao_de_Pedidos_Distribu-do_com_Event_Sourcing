# Sistema de Gestão de Pedidos Distribuído - CLAUDE.md

## Localização e Contexto do Projeto
- **Path**: /mnt/d/Programacao/Sistema_de_Gestão_de_Pedidos_Distribuído_com_Event_Sourcing
- **Arquitetura**: 4 Microsserviços + Frontend React + Infraestrutura
- **Padrões**: Event Sourcing, CQRS, Saga Pattern, Circuit Breaker

## Arquitetura dos Microsserviços

### Services (Portas e Responsabilidades)
- **Order Service** (8081): Event Sourcing para agregados de pedido, Command side do CQRS, Saga orchestration
- **Payment Service** (8082): Processamento de pagamentos, Integration com gateways, Compensating transactions
- **Inventory Service** (8083): Controle de estoque, Reservation patterns, Stock allocation algorithms
- **Query Service** (8084): CQRS Read Models, Dashboard analytics, Event replay, Materialized views

### Frontend e API
- **Frontend** (3000): React 18 + TypeScript + shadcn/ui + TanStack Query
- **API Gateway** (8080): Reverse proxy, load balancing, CORS

### Infraestrutura
- **PostgreSQL**: Event Store + Read Models (order-db, query-db)
- **RabbitMQ**: Event-driven messaging e saga coordination
- **Redis**: Projection caching e session management
- **Docker**: Containerização e orquestração

## Comandos de Desenvolvimento

### Inicialização
- `pedidos-quick dev`: Setup infraestrutura + hot reload
- `pedidos full`: Deploy completo (infra + todos serviços + frontend)
- `pedidos backend`: Apenas microsserviços backend
- `pedidos frontend`: Apenas frontend React

### Desenvolvimento Individual
- `start-order`: Order Service (porta 8081)
- `start-payment`: Payment Service (porta 8082)
- `start-inventory`: Inventory Service (porta 8083)
- `start-query`: Query Service (porta 8084)

### Debug
- `debug-order`: Order Service + remote debug (porta 5005)
- `debug-payment`: Payment Service + remote debug (porta 5006)
- `debug-inventory`: Inventory Service + remote debug (porta 5007)
- `debug-query`: Query Service + remote debug (porta 5008)

### Testing
- `test-all`: Todos os testes (unit + integration + contract + e2e)
- `test-integration`: Integration tests entre serviços
- `test-contracts`: Contract testing
- `test-e2e`: End-to-end tests

### Monitoramento
- `health-check`: Status de todos os serviços
- `monitor-logs`: Logs agregados em tempo real
- `check-events`: Status das filas RabbitMQ
- `redis-status`: Cache status

## Workflow EPCC para Microsserviços

### 1. Exploração (Event-Driven Architecture)
"Analise a comunicação entre Order, Payment, Inventory e Query.
Entenda o fluxo: OrderCreated → PaymentRequested → InventoryReserved → OrderConfirmed.
Examine event handlers, projections e saga coordinators.
NÃO escreva código - apenas entenda a arquitetura."

### 2. Planejamento (Distributed Systems)
"think hard sobre implementar [feature] considerando:
- Event Sourcing patterns e aggregate design
- CQRS separation entre command/query
- Eventual consistency handling
- Saga pattern para coordenação
- Circuit breaker para resiliência"

### 3. Codificação (TDD Distribuído)
"Primeiro, crie testes para:
- Unit tests: Domain logic, event handlers
- Integration tests: Event publishing/consuming
- Contract tests: API contracts entre services
- E2E tests: Complete user journeys
Depois implemente seguindo DDD e Event Sourcing."

### 4. Confirmação (Deploy Coordenado)
"Valide:
- Health checks de todos os serviços
- End-to-end communication
- Event propagation timing
- Performance metrics
- Coordinated deployment"

## Event Sourcing e CQRS Patterns

### Event Design
- **Events**: OrderCreatedEvent, PaymentProcessedEvent, InventoryReservedEvent
- **Immutable**: Events nunca mudam após persistência
- **Granular**: Um evento por mudança específica
- **Versionable**: Support para evolução de schema

### Aggregates
- **Order**: Creation, modification, cancellation
- **Payment**: Processing, refunds
- **Inventory**: Allocation, reservation, release
- **Idempotent**: Operations podem ser repetidas safely

### Event Store
- **PostgreSQL**: event_store table com JSON payloads
- **Partitioning**: Por aggregate type
- **Indexing**: Por aggregate_id, event_type, timestamp
- **Snapshotting**: Para aggregates com muitos eventos

### Projections
- **Materialized Views**: Para queries complexas
- **Cache Layer**: Redis para frequent access
- **Rebuild Strategy**: Replay events
- **Consistency**: Eventually consistent < 1s

## Performance Targets

### Latência
- **Command Processing**: < 100ms (95th percentile)
- **Event Processing**: < 50ms por evento
- **Query Response**: < 200ms para read models
- **Saga Completion**: < 5s para transações distribuídas

### Throughput
- **Message Processing**: > 1000 events/sec per service
- **API Requests**: > 500 req/sec per service
- **Database Operations**: > 2000 ops/sec

## Tecnologias (Latest LTS)
- **Java 21** + Spring Boot 3.4
- **React 18** + TypeScript 5.6
- **PostgreSQL 16** + Redis 7.2
- **RabbitMQ 3.13** + Docker 25
- **Maven 3.9** + Node.js 22

## Code Style
- **Java**: Clean Architecture, PascalCase classes, camelCase methods
- **Events**: [Aggregate][Action]Event (OrderCreatedEvent)
- **Commands**: [Action][Aggregate]Command (CreateOrderCommand)
- **TypeScript**: Functional components, TanStack Query, Tailwind CSS

## Security
- **JWT Tokens**: Com refresh token rotation
- **RBAC**: Role-based access control
- **Encryption**: TLS 1.3 in transit, AES-256 at rest
- **Audit Trail**: Complete event history
