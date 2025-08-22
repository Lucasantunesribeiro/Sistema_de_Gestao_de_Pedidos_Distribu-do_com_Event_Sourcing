# 🚂 Railway Deployment - Sistema de Gestão de Pedidos Distribuído

## 📋 Guia Completo de Deployment para Produção

### 🎯 Objetivo
Deploy completo do sistema distribuído para produção no Railway.app com:
- 4 Microsserviços (Java 21 + Spring Boot 3.4)
- Frontend React 18 + TypeScript
- PostgreSQL, Redis, RabbitMQ
- Configurações otimizadas para produção

### 🏗️ Arquitetura de Deployment

```
┌─────────────────────────────────────────────────────────────────┐
│                        Railway.app                             │
├─────────────────────────────────────────────────────────────────┤
│  Frontend (React)          │  Microsserviços                    │
│  ├─ Nginx + React Build    │  ├─ order-service (8081)          │
│  └─ Static Assets          │  ├─ payment-service (8082)        │
│                            │  ├─ inventory-service (8083)      │
│                            │  └─ order-query-service (8084)    │
├─────────────────────────────────────────────────────────────────┤
│  Infraestrutura                                                │
│  ├─ PostgreSQL (Event Store + Read Models)                     │
│  ├─ Redis (Cache + Sessions)                                   │
│  └─ RabbitMQ (Event Messaging)                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 🚀 Execução Automática

#### Pré-requisitos
1. **Conta Railway**: [railway.app](https://railway.app)
2. **Railway CLI**: `npm install -g @railway/cli`
3. **Git**: Código commitado no repositório

#### Deployment Automático
```bash
# 1. Autenticar no Railway
railway login

# 2. Executar script de deployment
./deploy-production.sh
```

### 📋 Processo Detalhado

#### 1. Infraestrutura (Ordem de Criação)
```bash
# Bancos de dados
railway add postgresql --name order-eventstore-db
railway add postgresql --name order-query-db

# Cache e Messaging
railway add redis --name order-cache
railway add rabbitmq --name message-broker
```

#### 2. Microsserviços (Ordem de Deployment)
```bash
# Shared Events Library
cd shared-events && mvn clean install

# Services
railway service create order-service
railway service create payment-service  
railway service create inventory-service
railway service create order-query-service

# Frontend
railway service create frontend
```

#### 3. Variáveis de Ambiente

##### Order Service
```env
SPRING_PROFILES_ACTIVE=railway
DATABASE_URL=${{order-eventstore-db.DATABASE_URL}}
RABBITMQ_URL=${{message-broker.RABBITMQ_URL}}
REDIS_URL=${{order-cache.REDIS_URL}}
PORT=8081
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

##### Payment Service
```env
SPRING_PROFILES_ACTIVE=railway
RABBITMQ_URL=${{message-broker.RABBITMQ_URL}}
PORT=8082
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

##### Inventory Service
```env
SPRING_PROFILES_ACTIVE=railway
RABBITMQ_URL=${{message-broker.RABBITMQ_URL}}
PORT=8083
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

##### Query Service
```env
SPRING_PROFILES_ACTIVE=railway
DATABASE_URL=${{order-query-db.DATABASE_URL}}
RABBITMQ_URL=${{message-broker.RABBITMQ_URL}}
REDIS_URL=${{order-cache.REDIS_URL}}
PORT=8084
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

##### Frontend
```env
NODE_ENV=production
VITE_API_ORDER_URL=https://{order-service-url}
VITE_API_QUERY_URL=https://{query-service-url}
VITE_API_PAYMENT_URL=https://{payment-service-url}
VITE_API_INVENTORY_URL=https://{inventory-service-url}
```

### ⚡ Configurações de Performance

#### Connection Pools Otimizados
```yaml
hikari:
  maximum-pool-size: 30      # Railway optimized
  minimum-idle: 15           # Faster startup
  connection-timeout: 8000   # Quick response
  validation-timeout: 3000   # Fast validation
```

#### Cache Strategy
```yaml
redis:
  timeout: 3000ms
  lettuce:
    pool:
      max-active: 20
      max-idle: 10
```

#### Circuit Breaker
```yaml
resilience:
  circuit-breaker:
    failure-rate-threshold: 50
    wait-duration-in-open-state: 30s
    sliding-window-size: 15
```

### 📊 Health Checks e Monitoramento

#### Endpoints de Health
- **Order Service**: `https://{order-service-url}/actuator/health`
- **Payment Service**: `https://{payment-service-url}/actuator/health`
- **Inventory Service**: `https://{inventory-service-url}/actuator/health`
- **Query Service**: `https://{query-service-url}/actuator/health`

#### Métricas Prometheus
- **Endpoint**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`
- **Custom Metrics**: Connection pools, circuit breakers, saga status

### 🧪 Validação de Deployment

#### 1. Health Checks Automáticos
```bash
curl -f https://{service-url}/actuator/health
```

#### 2. Teste End-to-End
```bash
# Criar pedido
curl -X POST https://{order-service-url}/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "test-customer-prod",
    "items": [{
      "productId": "laptop-prod-01",
      "productName": "Laptop Gaming Pro", 
      "quantity": 1,
      "unitPrice": 2500.00
    }]
  }'

# Consultar pedidos
curl https://{query-service-url}/api/query/orders
```

### 🎯 Performance Targets

#### Latência
- **APIs**: < 100ms (95th percentile)
- **Frontend**: < 1.5s First Paint
- **Event Processing**: < 50ms por evento
- **Cache Hit Ratio**: > 80%

#### Throughput
- **API Requests**: > 500 req/sec por serviço
- **Message Processing**: > 1000 events/sec
- **Database Operations**: > 2000 ops/sec

#### Availability
- **Services**: 99.9% uptime
- **Saga Completion**: 99.9% success rate
- **Database**: 99.95% availability

### 🔐 Segurança

#### Headers de Segurança
```yaml
security:
  cors:
    allowed-origins: "https://*.railway.app"
    allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
    max-age: 3600
```

#### JWT Configuration
```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 hours
  refresh-expiration: 604800000  # 7 days
```

### 📈 Monitoramento Contínuo

#### Métricas Customizadas
- **Connection Pool Usage**: Hikari metrics
- **Circuit Breaker Status**: Resilience4j metrics  
- **Saga Completion Rate**: Custom business metrics
- **Cache Performance**: Redis hit/miss ratios

#### Alertas
- **High Latency**: > 200ms API response
- **Low Success Rate**: < 95% saga completion
- **Resource Usage**: > 80% memory/CPU
- **Database Connections**: > 80% pool usage

### 🛠️ Comandos Úteis

#### Railway Management
```bash
# Status geral
railway status

# Logs específicos
railway logs --service order-service

# Abrir dashboard
railway open

# Conectar ao banco
railway connect order-eventstore-db

# Variáveis de ambiente
railway variables
```

#### Troubleshooting
```bash
# Redeploy específico
railway service use order-service
railway up --detach

# Restart service
railway service restart order-service

# Check logs
railway logs --service order-service --lines 100
```

### 📋 Checklist de Deployment

- [ ] ✅ Railway CLI instalado e autenticado
- [ ] ✅ Projeto Railway criado
- [ ] ✅ Infraestrutura configurada (PostgreSQL, Redis, RabbitMQ)
- [ ] ✅ Shared-events library construída
- [ ] ✅ Microsserviços deployados na ordem correta
- [ ] ✅ Frontend deployado e conectado
- [ ] ✅ Variáveis de ambiente configuradas
- [ ] ✅ Health checks passando
- [ ] ✅ Teste end-to-end funcionando
- [ ] ✅ Performance targets validados
- [ ] ✅ Monitoramento ativo

### 🎉 URLs Finais

Após deployment completo:

```
🌐 Frontend: https://{frontend-url}.railway.app
📝 Order API: https://{order-service-url}.railway.app/api/orders
📊 Query API: https://{query-service-url}.railway.app/api/query
💳 Payment API: https://{payment-service-url}.railway.app/api/payments
📦 Inventory API: https://{inventory-service-url}.railway.app/api/inventory
```

### 📞 Suporte

Para problemas de deployment:
1. Verificar logs: `railway logs --service {service-name}`
2. Validar variáveis: `railway variables`
3. Status dos serviços: `railway status`
4. Dashboard web: `railway open`

---

🚀 **Sistema pronto para produção no Railway!**