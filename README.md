Sistema Distribuído de Gestão de Pedidos (Java/Spring)

Sistema de gestão de pedidos em arquitetura de monólito modular, construído com Java 17, Spring Boot 3, PostgreSQL, RabbitMQ e Redis. Implementa Event Sourcing, CQRS, Clean Architecture e padrão Saga para orquestração de transações distribuídas.

## Requisitos
- Java 17
- Maven 3.9+
- Docker + Docker Compose v2

## Estrutura do repositório
- `libs/`: bibliotecas compartilhadas
  - `common-events`: envelopes de eventos versionados e convenções de fila
  - `common-security`: autenticação JWT, rate limiting e propriedades de segurança
  - `common-messaging`: correlação de mensagens e auto-configuração de mensageria
  - `common-observability`: correlação de logs e cabeçalhos para rastreabilidade
- `shared-events/`: payloads de eventos legados (preferir eventos em `unified-order-system/shared/events/`)
- `unified-order-system/`: monólito modular principal (módulo de foco)
- `services/`: microserviços legados (`order-service`, `payment-service`, `inventory-service`, `order-query-service`)
- `observability/`: configuração de Prometheus, Grafana, Loki e Tempo
- `tests/`: testes end-to-end (Playwright) e testes de carga com k6

## Execução local (Docker Compose)
1. Copiar o template de ambiente:
   ```bash
   cp .env.example .env
   ```
2. Definir `JWT_SECRET_KEY` (64 caracteres hexadecimais) e manter o `.env` fora do controle de versão.
3. Opcional: definir `COMPOSE_PROJECT_NAME=ordersystem` para evitar colisão de nomes de containers.
4. Subir apenas a infraestrutura (Postgres, RabbitMQ, Redis):
   ```bash
   docker compose -f docker-compose.yml up -d
   ```
5. Subir a aplicação completa (monólito `unified-order-system`):
   ```bash
   docker compose -f docker-compose.yml up -d --build unified-order-system
   ```
6. Subir também o stack de observabilidade:
   ```bash
   docker compose -f docker-compose.observability.yml up -d
   ```

## Build e testes (Maven)
- Build de todos os módulos (sem testes):
  ```bash
  mvn clean install -DskipTests
  ```
- Rodar todos os testes (a partir de `unified-order-system/`):
  ```bash
  mvn clean test
  ```
- Rodar um teste específico:
  ```bash
  mvn clean test -Dtest=CompleteOrderFlowIntegrationTest
  ```
- Build da imagem Docker do monólito:
  ```bash
  docker build -t unified-order-system:latest -f unified-order-system/Dockerfile .
  ```

## Variáveis de ambiente principais
- Banco de dados:
  - `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
  - `DATABASE_URL` (produção/PostgreSQL, usada pelo `application-production.yml`)
- Mensageria (RabbitMQ):
  - `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`
- Segurança:
  - `JWT_SECRET_KEY` (64 caracteres hexadecimais)
  - `SECURITY_SECRET` (mesmo valor de `JWT_SECRET_KEY`)
  - `SECURITY_ENFORCE_AUTH` (`false` para dev, `true` para produção)
  - `SECURITY_CORS_ALLOWED_ORIGINS` / `CORS_ORIGINS`
- Cache:
  - `REDIS_HOST`, `REDIS_PORT`, `REDIS_ENABLED`

## Endpoints úteis (ambiente local)
- UI do RabbitMQ: `http://localhost:15672`
- Health da aplicação: `http://localhost:8080/actuator/health`
- API docs (Swagger/OpenAPI): `http://localhost:8080/swagger-ui/index.html`
- Grafana (observabilidade): `http://localhost:3000` (usuario/senha padrão `admin/admin`)

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
  Esse é o endpoint principal da interface web (Dashboard do Unified Order System).

## Arquitetura (visão geral)
O sistema usa Clean Architecture dentro de um monólito modular:
- `order/application/`: casos de uso (ex.: `CreateOrderUseCase`, `CancelOrderUseCase`) implementando o padrão Saga
- `order/domain/`: regras de negócio e validações (`OrderBusinessRules`)
- `infrastructure/events/`: Event Sourcing via `EventPublisher` (persiste todos os eventos de domínio na tabela `domain_events`)
- `shared/events/`: classes de eventos de domínio (`OrderCreatedEvent`, `PaymentProcessedEvent`, `InventoryReservedEvent`, etc.)
- `config/`: configuração Spring (segurança, cache, banco de dados, métricas, CORS)

Fluxo principal de pedido:
- Status: `PENDING → INVENTORY_RESERVED → PAYMENT_PROCESSING → CONFIRMED`
- Cancelamentos disparam transações compensatórias (liberação de estoque + estorno de pagamento).

## Observações
- **Nunca** commitar `.env` ou qualquer segredo.
- Para gerar um `JWT_SECRET_KEY` seguro:
  ```bash
  bash scripts/generate-jwt-secret.sh
  ```
- O build Docker precisa ser feito a partir do diretório raiz do repositório (para incluir `libs`, `shared-events` e `unified-order-system`).
- Em produção, habilite autenticação e use um gerenciador de segredos:
  - `SECURITY_ENFORCE_AUTH=true`
  - Segredos gerenciados via AWS Secrets Manager, SSM Parameter Store ou equivalente.
