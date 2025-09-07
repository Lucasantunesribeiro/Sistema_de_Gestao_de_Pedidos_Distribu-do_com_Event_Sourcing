# Distributed Order System

Sistema de gestão de pedidos com arquitetura de microsserviços, Event Sourcing e frontend em React.

## Arquitetura

- **Backend:** Spring Boot (Java 17)
- **Frontend:** React + Vite
- **Event Bus:** RabbitMQ
- **Cache/Event Store:** Redis
- **Banco de Dados:** PostgreSQL
- **Containerização:** Docker / Docker Compose

### Serviços

| Serviço | Porta | Responsabilidade |
|--------|-------|------------------|
| Order Service | 8081 | CRUD de pedidos e publicação de eventos |
| Payment Service | 8082 | Processamento de pagamentos |
| Inventory Service | 8083 | Controle de estoque |
| Query Service | 8084 | Projeções e consultas (CQRS) |

## Endpoints principais

### Order Service (`/api/orders`)

| Método | Caminho | Descrição |
|--------|--------|-----------|
| `POST` | `/api/orders` | Criar pedido |
| `GET` | `/api/orders` | Listar pedidos |
| `GET` | `/api/orders/{id}` | Obter pedido por ID |
| `PUT`/`PATCH` | `/api/orders/{id}/status` | Atualizar status do pedido |
| `GET` | `/api/orders/customer/{customerId}` | Pedidos de um cliente |
| `GET` | `/api/orders/{id}/events` | Eventos de um pedido (Event Sourcing) |
| `GET` | `/api/orders/{id}/rebuild` | Reconstruir pedido a partir dos eventos |
| `GET` | `/api/orders/health` | Health check |

### Payment Service (`/api/payments`)

- `GET /api/payments`
- `GET /api/payments/{id}`
- `POST /api/payments/{id}/retry`

### Inventory Service (`/api/inventory`)

- `GET /api/inventory`

### Query Service (`/api/orders`)

- `GET /api/orders` – consultas paginadas
- `GET /api/orders/{id}` – leitura otimizada
- `GET /api/orders/customer/{customerId}` – pedidos por cliente

## Setup local

```bash
# build biblioteca compartilhada
mvn -q -pl shared-events clean install

# subir infraestrutura
docker-compose up order-db query-db rabbitmq redis

# iniciar um serviço
cd services/order-service
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

## CI/CD

Workflows GitHub Actions:
- **build.yml** – build e testes
- **security.yml** – scan de segurança
- **docker.yml** – imagem Docker
- **deploy-render.yml** – deploy automático no Render

Render usa a branch `main` e um container com `PORT=80` exposta no `Dockerfile` principal.

## Variáveis de ambiente

| Nome | Descrição |
|------|-----------|
| `DATABASE_URL` | JDBC do Postgres |
| `DATABASE_USERNAME` | Usuário do banco |
| `DATABASE_PASSWORD` | Senha do banco |
| `RABBITMQ_HOST` | Host do RabbitMQ |
| `RABBITMQ_PORT` | Porta do RabbitMQ |
| `REDIS_HOST` | Host do Redis |
| `REDIS_PORT` | Porta do Redis |

## Testes

```bash
mvn -pl services/order-service test
```

## Deploy

- Commits na branch configurada disparam o deploy automático no Render.
- Rollback: redeploy de commit anterior via painel do Render.

## Troubleshooting

| Problema | Solução |
|----------|---------|
| Erro de build Maven | verifique dependências e versão do JDK |
| Frontend não inicia | execute `npm install` e cheque `.env` |
| Container não sobe | `docker-compose logs` para detalhes |
| 404 em `PUT`/`PATCH` `/api/orders/{id}/status` | use o `orderId` retornado na criação do pedido e verifique se o Order Service está acessível |

## Exemplos de uso (curl)

```bash
# criar pedido
curl -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"C1","totalAmount":100}'

# atualizar status (PUT ou PATCH)
curl -X PUT http://localhost:8081/api/orders/ID/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"PAID"}'

curl -X PATCH http://localhost:8081/api/orders/ID/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"PAID"}'
```

## API Endpoints

Todas as rotas REST são expostas com o prefixo `/api`:

| Método | Caminho | Descrição |
|-------|--------|-----------|
| `GET` | `/api/orders` | Lista pedidos |
| `POST` | `/api/orders` | Cria pedido |
| `PUT` | `/api/orders/{id}/status` | Atualiza status |

## Health check

O estado da aplicação pode ser verificado em:

```bash
curl -i http://localhost:8080/actuator/health
```

## Smoke tests

Execute uma verificação rápida com:

```bash
scripts/smoke.sh
```

## Deploy no Render

O Render define a porta através da variável `PORT`. O container já usa `java -Dserver.port=$PORT -Dserver.address=0.0.0.0` e expõe o Actuator em `/actuator/health`.
