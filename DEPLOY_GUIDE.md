# Guia de Deploy - Sistema Unificado de Pedidos

## 🎯 Visão Geral

Este guia fornece instruções completas para fazer o deploy do Sistema Unificado de Pedidos no Render.com. O sistema consolida todos os microserviços (Order, Payment, Inventory, Query) em uma única aplicação Spring Boot otimizada.

## ✅ Pré-requisitos

- Conta no [Render.com](https://render.com)
- Repositório Git com o código
- Java 17+ (para desenvolvimento local)
- Maven 3.6+ (para build local)

## 🏗️ Arquitetura do Sistema Unificado

```
┌─────────────────────────────────────────┐
│           Unified Order System          │
├─────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │ Order   │ │Payment  │ │Inventory│   │
│  │ Module  │ │ Module  │ │ Module  │   │
│  └─────────┘ └─────────┘ └─────────┘   │
│  ┌─────────┐ ┌─────────────────────┐   │
│  │ Query   │ │   Shared Components │   │
│  │ Module  │ │   (Config, Events)  │   │
│  └─────────┘ └─────────────────────┘   │
└─────────────────────────────────────────┘
           │                    │
    ┌─────────────┐      ┌─────────────┐
    │ PostgreSQL  │      │    Redis    │
    │  Database   │      │    Cache    │
    └─────────────┘      └─────────────┘
```

## 🚀 Processo de Deploy

### Passo 1: Preparar o Repositório

1. **Commit e Push do Código**
   ```bash
   git add .
   git commit -m "feat: unified order system ready for deploy"
   git push origin main
   ```

2. **Verificar Estrutura do Projeto**
   ```
   projeto/
   ├── unified-order-system/          # Aplicação Spring Boot
   │   ├── src/main/java/            # Código fonte
   │   ├── src/main/resources/       # Configurações
   │   ├── pom.xml                   # Dependências Maven
   │   └── Dockerfile               # Container alternativo
   ├── render-unified.yaml          # Configuração Render
   ├── deploy.sh                    # Script de deploy
   └── DEPLOY_GUIDE.md             # Este guia
   ```

### Passo 2: Configurar Serviços no Render

#### 2.1 Criar PostgreSQL Database

1. Acesse [Render Dashboard](https://dashboard.render.com)
2. Clique em "New +" → "PostgreSQL"
3. Configure:
   - **Name**: `unified-order-db`
   - **Database Name**: `unified_order_system`
   - **User**: `unified_user`
   - **Region**: `Oregon` (ou mais próximo)
   - **Plan**: `Starter` ($7/mês)

#### 2.2 Criar Redis Instance

1. Clique em "New +" → "Redis"
2. Configure:
   - **Name**: `unified-order-redis`
   - **Region**: `Oregon`
   - **Plan**: `Starter` ($7/mês)
   - **Maxmemory Policy**: `allkeys-lru`

#### 2.3 Criar Web Service

1. Clique em "New +" → "Web Service"
2. Conecte seu repositório Git
3. Configure:
   - **Name**: `unified-order-system`
   - **Environment**: `Java`
   - **Region**: `Oregon`
   - **Branch**: `main`
   - **Root Directory**: deixe vazio
   - **Build Command**: 
     ```bash
     cd unified-order-system && ./mvnw clean package -DskipTests
     ```
   - **Start Command**:
     ```bash
     cd unified-order-system && java -Dspring.profiles.active=prod -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -Dspring.backgroundpreinitializer.ignore=true -Dspring.jmx.enabled=false -jar target/unified-order-system-1.0.0.jar
     ```
   - **Plan**: `Starter` ($7/mês)

### Passo 3: Configurar Variáveis de Ambiente

No painel do Web Service, adicione as seguintes variáveis:

#### Variáveis Básicas
```
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
JPA_DDL_AUTO=update
JPA_SHOW_SQL=false
```

#### Variáveis de Database (conectar ao PostgreSQL criado)
```
DATABASE_URL=[Auto-conectado do PostgreSQL]
DATABASE_USERNAME=[Auto-conectado do PostgreSQL]
DATABASE_PASSWORD=[Auto-conectado do PostgreSQL]
```

#### Variáveis de Redis (conectar ao Redis criado)
```
REDIS_HOST=[Auto-conectado do Redis]
REDIS_PORT=[Auto-conectado do Redis]
REDIS_PASSWORD=[Auto-conectado do Redis]
```

### Passo 4: Deploy Automático

1. **Usando render-unified.yaml** (Recomendado)
   - Copie o conteúdo de `render-unified.yaml` para `render.yaml` na raiz
   - Commit e push
   - Render detectará automaticamente e criará todos os serviços

2. **Deploy Manual**
   - Configure cada serviço individualmente conforme Passo 2

## 🔧 Configurações Avançadas

### Health Check
- **Path**: `/actuator/health`
- **Timeout**: 30 segundos
- **Interval**: 30 segundos

### Otimizações JVM
```bash
-Xms256m -Xmx512m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseContainerSupport
-Djava.security.egd=file:/dev/./urandom
```

### Configurações de Cache Redis
```properties
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
spring.data.redis.timeout=5000ms
```

## 📊 Monitoramento e Observabilidade

### Endpoints Disponíveis

#### Health Checks
- `GET /actuator/health` - Status geral do sistema
- `GET /actuator/health/db` - Status do banco de dados
- `GET /actuator/health/redis` - Status do Redis

#### Métricas
- `GET /actuator/metrics` - Métricas do sistema
- `GET /actuator/prometheus` - Métricas para Prometheus

#### API Documentation
- `GET /swagger-ui.html` - Interface Swagger
- `GET /api-docs` - OpenAPI JSON

### APIs Principais

#### Orders API
```
POST /api/orders                    # Criar pedido
GET  /api/orders/{id}              # Buscar pedido
POST /api/orders/{id}/process      # Processar pedido
POST /api/orders/{id}/cancel       # Cancelar pedido
```

#### Query API
```
GET /api/query/orders                     # Listar pedidos
GET /api/query/orders/customer/{id}       # Pedidos por cliente
GET /api/query/orders/{id}/summary        # Resumo do pedido
GET /api/query/inventory/{id}             # Estoque do produto
GET /api/query/payments/order/{id}        # Pagamento do pedido
```

## 🧪 Testes e Validação

### Teste Local
```bash
cd unified-order-system
./mvnw test
./mvnw spring-boot:run -Dspring.profiles.active=dev
```

### Teste de Produção
```bash
# Health check
curl https://your-app.onrender.com/actuator/health

# Criar pedido de teste
curl -X POST https://your-app.onrender.com/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "test-customer",
    "items": [{
      "productId": "product-1",
      "quantity": 1,
      "price": 99.99
    }]
  }'
```

## 🔍 Troubleshooting

### Problemas Comuns

#### 1. Build Falha
```bash
# Verificar logs de build no Render
# Comum: dependências Maven ou versão Java incorreta
```

#### 2. Aplicação não Inicia
```bash
# Verificar variáveis de ambiente
# Verificar conexão com PostgreSQL e Redis
# Verificar logs da aplicação
```

#### 3. Timeout de Health Check
```bash
# Aumentar timeout no Render
# Verificar se aplicação está respondendo na porta correta
# Verificar logs de inicialização
```

#### 4. Problemas de Memória
```bash
# Ajustar configurações JVM
# Monitorar uso de memória via /actuator/metrics
# Considerar upgrade do plano
```

### Logs e Debugging

#### Acessar Logs
```bash
# No Render Dashboard
# Ir para o serviço → Logs tab
# Filtrar por nível (INFO, ERROR, etc.)
```

#### Logs Estruturados
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "logger": "com.ordersystem.unified.order.OrderService",
  "message": "Order created successfully",
  "correlationId": "abc-123-def",
  "orderId": "order-456"
}
```

## 💰 Custos Estimados

### Configuração Starter
- **Web Service**: $7/mês
- **PostgreSQL**: $7/mês  
- **Redis**: $7/mês
- **Total**: ~$21/mês

### Configuração Standard (Recomendada para Produção)
- **Web Service**: $25/mês
- **PostgreSQL**: $20/mês
- **Redis**: $15/mês
- **Total**: ~$60/mês

## 🔄 Atualizações e Manutenção

### Deploy de Atualizações
1. Commit mudanças no Git
2. Push para branch principal
3. Render fará deploy automático
4. Monitorar logs durante deploy

### Backup e Recuperação
- PostgreSQL: Backups automáticos diários
- Redis: Dados em cache, não críticos
- Código: Versionado no Git

### Monitoramento Contínuo
- Configurar alertas no Render
- Monitorar métricas de performance
- Revisar logs regularmente

## 🎉 Próximos Passos

Após o deploy bem-sucedido:

1. **Configurar Domínio Customizado** (opcional)
2. **Implementar CI/CD** com GitHub Actions
3. **Configurar Monitoramento Avançado** 
4. **Otimizar Performance** baseado em métricas
5. **Implementar Testes E2E** automatizados

## 📞 Suporte

- **Render Docs**: https://render.com/docs
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Logs da Aplicação**: Disponíveis no Render Dashboard

---

**✅ Sistema pronto para produção com alta disponibilidade, cache Redis, monitoramento completo e otimizações de performance!**