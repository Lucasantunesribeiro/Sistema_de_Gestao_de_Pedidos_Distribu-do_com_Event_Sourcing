# Deploy no Railway.app - Guia Rápido

## 🚀 Deploy Automático

### 1. Preparação
```bash
git clone <repository-url>
cd Sistema_de_Gestão_de_Pedidos_Distribuído_com_Event_Sourcing
```

### 2. Criar Projeto no Railway
1. Acesse [railway.app](https://railway.app)
2. Conecte seu GitHub
3. Clique em "New Project" → "Deploy from GitHub repo"
4. Selecione este repositório

### 3. Adicionar Bancos de Dados

**PostgreSQL 1 (Event Store):**
- New → Database → PostgreSQL
- Nome: `order-eventstore-db`

**PostgreSQL 2 (Read Model):**
- New → Database → PostgreSQL  
- Nome: `order-query-db`

**RabbitMQ:**
- New → Database → RabbitMQ
- Nome: `message-broker`

### 4. Deploy dos Serviços

Para cada serviço, clique em "New" → "Empty Service":

#### Order Service
- **Nome**: `order-service`
- **Root Directory**: `services/order-service`
- **Variáveis**:
  ```
  DATABASE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
  DATABASE_USERNAME=${{Postgres.PGUSER}}
  DATABASE_PASSWORD=${{Postgres.PGPASSWORD}}
  RABBITMQ_HOST=${{RabbitMQ.RABBITMQ_HOST}}
  RABBITMQ_PORT=${{RabbitMQ.RABBITMQ_PORT}}
  RABBITMQ_USERNAME=${{RabbitMQ.RABBITMQ_DEFAULT_USER}}
  RABBITMQ_PASSWORD=${{RabbitMQ.RABBITMQ_DEFAULT_PASS}}
  SPRING_PROFILES_ACTIVE=railway
  ```

#### Payment Service
- **Nome**: `payment-service`
- **Root Directory**: `services/payment-service`
- **Variáveis**:
  ```
  RABBITMQ_HOST=${{RabbitMQ.RABBITMQ_HOST}}
  RABBITMQ_PORT=${{RabbitMQ.RABBITMQ_PORT}}
  RABBITMQ_USERNAME=${{RabbitMQ.RABBITMQ_DEFAULT_USER}}
  RABBITMQ_PASSWORD=${{RabbitMQ.RABBITMQ_DEFAULT_PASS}}
  SPRING_PROFILES_ACTIVE=railway
  ```

#### Inventory Service
- **Nome**: `inventory-service`
- **Root Directory**: `services/inventory-service`
- **Variáveis**:
  ```
  RABBITMQ_HOST=${{RabbitMQ.RABBITMQ_HOST}}
  RABBITMQ_PORT=${{RabbitMQ.RABBITMQ_PORT}}
  RABBITMQ_USERNAME=${{RabbitMQ.RABBITMQ_DEFAULT_USER}}
  RABBITMQ_PASSWORD=${{RabbitMQ.RABBITMQ_DEFAULT_PASS}}
  SPRING_PROFILES_ACTIVE=railway
  ```

#### Order Query Service
- **Nome**: `order-query-service`
- **Root Directory**: `services/order-query-service`
- **Variáveis**:
  ```
  DATABASE_URL=jdbc:postgresql://${{Postgres-2.PGHOST}}:${{Postgres-2.PGPORT}}/${{Postgres-2.PGDATABASE}}
  DATABASE_USERNAME=${{Postgres-2.PGUSER}}
  DATABASE_PASSWORD=${{Postgres-2.PGPASSWORD}}
  RABBITMQ_HOST=${{RabbitMQ.RABBITMQ_HOST}}
  RABBITMQ_PORT=${{RabbitMQ.RABBITMQ_PORT}}
  RABBITMQ_USERNAME=${{RabbitMQ.RABBITMQ_DEFAULT_USER}}
  RABBITMQ_PASSWORD=${{RabbitMQ.RABBITMQ_DEFAULT_PASS}}
  REDIS_HOST=${{Redis.REDIS_URL}}
  SPRING_PROFILES_ACTIVE=railway
  ```

## ✅ Verificação

### Health Checks
```bash
curl https://order-service-xxx.railway.app/api/orders/actuator/health
curl https://payment-service-xxx.railway.app/api/payments/actuator/health
curl https://inventory-service-xxx.railway.app/api/inventory/actuator/health
curl https://order-query-service-xxx.railway.app/api/orders/actuator/health
```

### Teste Funcional
```bash
# Criar pedido
curl -X POST https://order-service-xxx.railway.app/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "test-customer",
    "items": [
      {
        "productId": "laptop-01",
        "productName": "Laptop Gaming",
        "quantity": 1,
        "unitPrice": 2500.00
      }
    ]
  }'

# Consultar pedidos
curl https://order-query-service-xxx.railway.app/api/orders
```

## 🔧 Troubleshooting

### Logs
- No dashboard Railway, clique em cada serviço
- Vá para "Logs" para ver a saída

### Problemas Comuns

**Build fails:**
- Verifique se o `services/{service-name}/Dockerfile` existe
- Confirme que shared-events compila corretamente

**Service crashes:**
- Verifique variáveis de ambiente
- Aguarde bancos de dados ficarem online primeiro

**Connection timeouts:**
- Databases demoram ~30s para ficar prontos
- Services dependentes podem falhar inicialmente

## 💡 Dicas

- **Order de Deploy**: Bancos → Order Service → Payment Service → Inventory Service → Query Service
- **Logs**: Use filtros no Railway para achar erros específicos
- **Redeploy**: Push para main branch automaticamente faz redeploy
- **Domains**: Railway fornece HTTPS automático para domínios gerados

## 📊 Limitações Gratuitas

- 512MB RAM por serviço
- $5 créditos/mês (suficiente para demo)
- 500h build/mês
- Sem domínio customizado

Para produção, considere upgrade do plano.

---

🎉 **Sucesso!** Seu sistema distribuído está rodando na nuvem!