# OrderFlow — Sistema Distribuído de Gestão de Pedidos

Sistema de gestão de pedidos em arquitetura de monólito modular, construído com Java 17, Spring Boot 3, PostgreSQL e Redis. Implementa Event Sourcing, Clean Architecture e padrão Saga para orquestração de transações distribuídas.

## Requisitos
- Java 17
- Maven 3.9+
- Docker + Docker Compose v2
- Node 20+ (somente para desenvolvimento do frontend Angular)

## Estrutura do repositório
- `libs/`: bibliotecas compartilhadas
  - `common-events`: envelopes de eventos versionados e convenções de fila
  - `common-security`: autenticação JWT, rate limiting e propriedades de segurança
  - `common-messaging`: correlação de mensagens e auto-configuração de mensageria
  - `common-observability`: correlação de logs e cabeçalhos para rastreabilidade
- `shared-events/`: payloads de eventos legados (preferir eventos em `unified-order-system/shared/events/`)
- `unified-order-system/`: monólito modular principal (módulo de foco)
- `frontend/`: frontend Angular 17 (dashboard, pedidos, estoque)
- `services/`: microserviços legados (`order-service`, `payment-service`, `inventory-service`, `order-query-service`)
- `observability/`: configuração de Prometheus, Grafana, Loki e Tempo
- `tests/`: testes end-to-end (Playwright) e testes de carga com k6

## Execução local (Docker Compose)
1. Copiar o template de ambiente:
   ```bash
   cp .env.example .env
   ```
2. Definir `JWT_SECRET_KEY` (64 caracteres hexadecimais) e manter o `.env` fora do controle de versão.
3. Opcional: definir `COMPOSE_PROJECT_NAME=orderflow` para evitar colisão de nomes de containers.
4. Subir toda a stack (backend + frontend + infra):
   ```bash
   docker compose up -d --build
   ```
5. Subir apenas o backend:
   ```bash
   docker compose up -d --build unified-order-system
   ```
6. Subir também o stack de observabilidade:
   ```bash
   docker compose -f docker-compose.observability.yml up -d
   ```

## Frontend (Angular 17)
Desenvolvimento local do frontend:
```bash
cd frontend
npm install
npm start          # dev server em http://localhost:4200 com proxy para :8080
npm run build      # build de produção em dist/
```

## Build e testes (Maven)
- Build de todos os módulos (sem testes):
  ```bash
  mvn clean install -DskipTests
  ```
- Rodar todos os testes (a partir de `unified-order-system/`):
  ```bash
  cd unified-order-system && mvn clean test
  ```
- Rodar um teste específico:
  ```bash
  cd unified-order-system && mvn test -Dtest=CompleteOrderFlowIntegrationTest
  ```
- Build da imagem Docker do monólito:
  ```bash
  docker build -t unified-order-system:latest -f unified-order-system/Dockerfile .
  ```

## Testes de carga (k6)
```bash
# Requer k6 instalado: https://k6.io/docs/get-started/installation/
k6 run tests/k6/load-test.js

# Com VUs e duração customizados
k6 run --vus 50 --duration 60s tests/k6/load-test.js

# Apontando para instância remota
BASE_URL=http://98.92.208.98 k6 run tests/k6/load-test.js
```

## Variáveis de ambiente principais
- Banco de dados:
  - `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
  - `DATABASE_URL` (produção/PostgreSQL, usada pelo `application-production.yml`)
- Segurança:
  - `JWT_SECRET_KEY` (64 caracteres hexadecimais)
  - `SECURITY_SECRET` (mesmo valor de `JWT_SECRET_KEY`)
  - `SECURITY_ENFORCE_AUTH` (`false` para dev, `true` para produção)
  - `CORS_ALLOWED_ORIGINS` (padrão: `http://localhost:4200,http://localhost:8080`)
- Cache:
  - `REDIS_HOST`, `REDIS_PORT`, `REDIS_ENABLED`

## Endpoints úteis (ambiente local)
- **Frontend (Angular)**: `http://localhost:4200`
- **Health da aplicação**: `http://localhost:8080/actuator/health`
- **API docs (Swagger/OpenAPI)**: `http://localhost:8080/swagger-ui/index.html`
- **WebSocket**: `ws://localhost:8080/ws` (SockJS + STOMP)
  - Tópicos: `/topic/orders`, `/topic/inventory`, `/topic/payments`
- **Grafana (observabilidade)**: `http://localhost:3000` (usuario/senha padrão `admin/admin`)

## Deploy na AWS
O repositório contém automações para deploy em EC2 e uso de ECR:

- **Imagem Docker padrão (ECR)**
  Definida em `docker-compose.prod.yml`:
  ```text
  246599827442.dkr.ecr.us-east-1.amazonaws.com/unified-order-system:latest
  ```
  Pode ser sobrescrita via variável `APP_IMAGE`.

- **URL pública atual (EC2)**
  A instância EC2 atualmente expõe a aplicação em:
  ```text
  http://98.92.208.98/dashboard
  ```
  Esse é o endpoint principal da interface web (Dashboard do OrderFlow).

## Arquitetura (visão geral)
O sistema usa Clean Architecture dentro de um monólito modular:
- `order/application/`: casos de uso (ex.: `CreateOrderUseCase`, `CancelOrderUseCase`) implementando o padrão Saga
- `order/domain/`: regras de negócio e validações (`OrderBusinessRules`)
- `infrastructure/events/`: Event Sourcing via `EventPublisher` (persiste todos os eventos de domínio na tabela `domain_events`)
- `shared/events/`: classes de eventos de domínio (`OrderCreatedEvent`, `PaymentProcessedEvent`, `InventoryReservedEvent`, etc.)
- `config/`: configuração Spring (segurança, cache, banco de dados, métricas, CORS, WebSocket)
- `frontend/`: Angular 17 com componentes standalone, serviços HTTP e integração WebSocket via SockJS + STOMP

Fluxo principal de pedido:
- Status: `PENDING → INVENTORY_RESERVED → PAYMENT_PROCESSING → CONFIRMED`
- Cancelamentos disparam transações compensatórias (liberação de estoque + estorno de pagamento via `PaymentService.refundPayment()`).

## Observações
- **Nunca** commitar `.env` ou qualquer segredo.
- Para gerar um `JWT_SECRET_KEY` seguro:
  ```bash
  bash scripts/generate-jwt-secret.sh
  ```
- O build Docker do backend precisa ser feito a partir do diretório raiz do repositório (para incluir `libs`, `shared-events` e `unified-order-system`).
- Em produção, habilite autenticação e use um gerenciador de segredos:
  - `SECURITY_ENFORCE_AUTH=true`
  - Segredos gerenciados via AWS Secrets Manager, SSM Parameter Store ou equivalente.
