# Configuração Local de Desenvolvimento

## 🚀 Guia Rápido

### Pré-requisitos
- Docker 24+ e Docker Compose
- Java 21+ (para desenvolvimento local)
- Node.js 22+ (para desenvolvimento frontend)
- Maven 3.9+
- Git

### Inicialização Rápida
```bash
# Clonar e entrar no projeto
git clone <repository-url>
cd Sistema_de_Gestão_de_Pedidos_Distribuído_com_Event_Sourcing

# Iniciar ambiente completo
./scripts/dev-up.sh

# Aguardar inicialização (~2-3 minutos)
# Acessar: http://localhost:3000
```

### Parar Ambiente
```bash
./scripts/dev-down.sh
```

## 📋 Configuração Detalhada

### 1. Configurar Variáveis de Ambiente

```bash
# Copiar template de configuração
cp .env.example .env

# Editar variáveis (opcional, defaults funcionam para dev)
nano .env
```

#### Variáveis principais para desenvolvimento:
```bash
ENVIRONMENT=docker
MESSAGING_TYPE=redis  # ou rabbitmq
DATABASE_URL=jdbc:postgresql://localhost:5432/order_db
REDIS_HOST=localhost
VITE_API_URL=http://localhost:8080
```

### 2. Perfis de Execução

#### Desenvolvimento com Redis (Recomendado para simular produção)
```bash
MESSAGING_TYPE=redis ./scripts/dev-up.sh
```

#### Desenvolvimento com RabbitMQ (Desenvolvimento local completo)
```bash
MESSAGING_TYPE=rabbitmq ./scripts/dev-up.sh
```

#### Simulação de Produção
```bash
ENVIRONMENT=prod-simulated MESSAGING_TYPE=redis docker-compose --profile prod-simulated up
```

### 3. Desenvolvimento Individual de Serviços

#### Iniciar apenas infraestrutura
```bash
docker-compose --profile dev up -d order-db query-db redis rabbitmq
```

#### Executar serviços localmente (hot reload)
```bash
# Terminal 1 - Shared Events
cd shared-events
mvn clean install -DskipTests

# Terminal 2 - Order Service  
cd services/order-service
mvn spring-boot:run

# Terminal 3 - Payment Service
cd services/payment-service  
mvn spring-boot:run

# Terminal 4 - Inventory Service
cd services/inventory-service
mvn spring-boot:run

# Terminal 5 - Query Service
cd services/order-query-service
mvn spring-boot:run

# Terminal 6 - Frontend
cd frontend
npm install
npm run dev
```

## 🔧 Comandos Úteis

### Docker Compose
```bash
# Status dos serviços
docker-compose --profile dev ps

# Logs de todos os serviços
docker-compose --profile dev logs -f

# Logs de um serviço específico
docker-compose --profile dev logs -f order-service

# Restart de um serviço
docker-compose --profile dev restart order-service

# Rebuild e restart
docker-compose --profile dev up -d --build order-service
```

### Health Checks
```bash
# Verificar status de todos os serviços
curl http://localhost:8081/actuator/health  # Order Service
curl http://localhost:8082/actuator/health  # Payment Service  
curl http://localhost:8083/actuator/health  # Inventory Service
curl http://localhost:8084/actuator/health  # Query Service
curl http://localhost:3000/health           # Frontend
```

### Banco de Dados
```bash
# Conectar ao PostgreSQL Order DB
docker exec -it order-db psql -U postgres -d order_db

# Conectar ao PostgreSQL Query DB  
docker exec -it query-db psql -U postgres -d order_query_db

# Executar migrations
./scripts/migrate.sh --env development
```

### Redis
```bash
# Conectar ao Redis
docker exec -it redis redis-cli

# Monitorar events Redis Streams
redis-cli XREAD COUNT 10 STREAMS order-events payment-events inventory-events 0-0 0-0 0-0
```

### RabbitMQ (se usando MESSAGING_TYPE=rabbitmq)
```bash
# Management UI: http://localhost:15672 (guest/guest)

# Listar queues
docker exec -it rabbitmq rabbitmqctl list_queues

# Monitorar exchanges
docker exec -it rabbitmq rabbitmqctl list_exchanges
```

## 🧪 Testes

### Executar todos os testes
```bash
# Backend tests
cd shared-events && mvn test
cd services/order-service && mvn test
cd services/payment-service && mvn test  
cd services/inventory-service && mvn test
cd services/order-query-service && mvn test

# Frontend tests
cd frontend
npm run test
npm run type-check
```

### Testes de Integração
```bash
# Criar pedido
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "test-customer",
    "items": [
      {"productId": "product-1", "quantity": 2, "price": 25.50}
    ]
  }'

# Listar pedidos
curl http://localhost:8084/api/orders

# Dashboard metrics
curl http://localhost:8084/api/dashboard/metrics
```

## 🐛 Troubleshooting

### Problemas Comuns

#### 1. Porta já em uso
```bash
# Encontrar processo usando porta
lsof -i :8081

# Matar processo
kill -9 <PID>

# Ou usar portas alternativas
DATABASE_PORT=5433 docker-compose --profile dev up
```

#### 2. Erro de conexão com banco
```bash
# Verificar se PostgreSQL está rodando
docker-compose --profile dev ps order-db

# Restart banco
docker-compose --profile dev restart order-db

# Verificar logs
docker-compose --profile dev logs order-db
```

#### 3. Shared events não encontrado
```bash
# Rebuild shared events
cd shared-events
mvn clean install -DskipTests

# Limpar cache Maven
rm -rf ~/.m2/repository/com/ordersystem
```

#### 4. Frontend não carrega
```bash
# Limpar cache npm
cd frontend
rm -rf node_modules package-lock.json
npm install

# Verificar variáveis de ambiente
echo $VITE_API_URL
```

#### 5. Redis connection refused
```bash
# Verificar se Redis está rodando
docker-compose --profile dev ps redis

# Restart Redis
docker-compose --profile dev restart redis

# Testar conexão
docker exec -it redis redis-cli ping
```

### Debug Mode

#### Java Services Debug
```bash
# Executar com debug
cd services/order-service
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Conectar IDE na porta 5005
```

#### Frontend Debug
```bash
cd frontend
npm run dev -- --host 0.0.0.0 --port 3001
# React DevTools disponível
```

## 📊 Monitoramento Local

### Logs Estruturados
```bash
# Seguir logs em tempo real
docker-compose --profile dev logs -f | grep ERROR

# Filtrar por serviço
docker-compose --profile dev logs -f order-service | jq '.'
```

### Métricas
- **Spring Boot Actuator**: `http://localhost:808X/actuator`
- **Health Checks**: `http://localhost:808X/actuator/health`
- **Metrics**: `http://localhost:808X/actuator/metrics`
- **Info**: `http://localhost:808X/actuator/info`

### Performance
```bash
# Monitor recursos Docker
docker stats

# Monitor específico
docker stats order-service payment-service inventory-service order-query-service
```

## 🔄 Workflow de Desenvolvimento

### 1. Funcionalidade Nova
```bash
# 1. Criar branch
git checkout -b feature/nova-funcionalidade

# 2. Implementar mudanças
# 3. Testar localmente
./scripts/dev-up.sh

# 4. Executar testes
mvn test (nos serviços alterados)

# 5. Commit e push
git add .
git commit -m "feat: nova funcionalidade"
git push origin feature/nova-funcionalidade
```

### 2. Debug de Issues
```bash
# 1. Reproduzir localmente
./scripts/dev-up.sh

# 2. Verificar logs
docker-compose --profile dev logs -f <service>

# 3. Debug com breakpoints
# (conectar IDE nas portas de debug)

# 4. Testar fix
mvn test
```

### 3. Preparar para Deploy
```bash
# 1. Testar simulação produção
ENVIRONMENT=prod-simulated MESSAGING_TYPE=redis docker-compose --profile prod-simulated up

# 2. Build produção
docker-compose build

# 3. Merge para main
git checkout main
git merge feature/nova-funcionalidade
git push origin main
# (CI/CD automaticamente fará deploy)
```

## 📝 Configurações Específicas

### IDE Configuration

#### IntelliJ IDEA
```bash
# Projeto Maven multi-módulo
# File > Open > selecionar pasta raiz
# Importar como Maven project
# Configurar JDK 21
# Adicionar configurações de run para cada serviço
```

#### VS Code
```bash
# Extensões recomendadas:
- Java Extension Pack
- Spring Boot Extension Pack  
- Docker
- ESLint (para frontend)
- Prettier (para frontend)

# .vscode/launch.json criado automaticamente
```

### Configurações de Rede
```bash
# Docker network
docker network ls | grep order-network

# Inspecionar network
docker network inspect order-network

# DNS interno funciona entre containers:
# order-db, query-db, redis, rabbitmq
```

---

💡 **Dica**: Use `./scripts/dev-up.sh` para ambiente completo ou desenvolvimento individual conforme necessário.

🔧 **Suporte**: Para issues, verificar logs e consultar este guia de troubleshooting.