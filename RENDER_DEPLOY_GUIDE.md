# 🚀 Guia de Deploy no Render - Sistema de Gestão de Pedidos

## 📋 Pré-requisitos
- Conta no [Render](https://render.com)
- Repositório Git público no GitHub
- Código commitado na branch `main`

## 🎯 Deploy Automático com Blueprint

### Opção 1: Deploy via Blueprint (Recomendado)

1. **Acesse o Render Dashboard**
   ```
   https://dashboard.render.com
   ```

2. **Clique em "New" → "Blueprint"**

3. **Cole o conteúdo do arquivo `render-blueprint.yaml`**
   - O arquivo está na raiz do projeto
   - Contém toda a configuração necessária

4. **Clique em "Deploy"**
   - Render criará automaticamente:
     - PostgreSQL Database (free tier)
     - Redis Cache (free tier)
     - Web Service (frontend + API)
     - 3 Worker Services (order, payment, inventory)

### Opção 2: Deploy Manual por Serviço

Se preferir criar cada serviço manualmente:

#### 1. Criar PostgreSQL Database
```
Name: order-postgres-db
Plan: Free
Region: Oregon
Database Name: order_system_db
Database User: order_system_user
```

#### 2. Criar Redis Cache
```
Name: order-redis-cache
Plan: Free
Region: Oregon
```

#### 3. Criar Web Service (Frontend + API)
```
Name: gestao-pedidos-web
Environment: Docker
Repository: https://github.com/Lucasantunesribeiro/Sistema_de_Gestao_de_Pedidos_Distribu-do_com_Event_Sourcing.git
Branch: main
Build Command: (leave empty - uses Dockerfile)
Start Command: (leave empty - uses Dockerfile CMD)

Environment Variables:
SERVICE_TYPE=web
MESSAGING_TYPE=redis
SPRING_PROFILES_ACTIVE=render
JAVA_OPTS=-Xmx128m -XX:+UseContainerSupport
DATABASE_URL=[Connect to PostgreSQL]
REDIS_URL=[Connect to Redis]
```

#### 4. Criar Worker Services

**Order Service:**
```
Name: gestao-pedidos-order
Environment: Docker
[Same repo and branch]
Environment Variables:
SERVICE_TYPE=order
[Same DATABASE_URL and REDIS_URL]
```

**Payment Service:**
```
Name: gestao-pedidos-payment
SERVICE_TYPE=payment
[Same config pattern]
```

**Inventory Service:**
```
Name: gestao-pedidos-inventory  
SERVICE_TYPE=inventory
[Same config pattern]
```

## ⚙️ Configurações de Ambiente

### Variáveis Essenciais
```bash
# Tipo de serviço (determina qual componente executar)
SERVICE_TYPE=web|order|payment|inventory

# Conexão com banco PostgreSQL (auto-conectado via Blueprint)  
DATABASE_URL=postgresql://user:pass@host:port/dbname

# Conexão com Redis (auto-conectado via Blueprint)
REDIS_URL=redis://default:pass@host:port

# Configurações Java
JAVA_OPTS=-Xmx128m -XX:+UseContainerSupport
SPRING_PROFILES_ACTIVE=render
MESSAGING_TYPE=redis
```

## 🔍 Verificação do Deploy

### 1. Status dos Serviços
Verifique no Dashboard que todos os serviços estão com status "Live":
- ✅ order-postgres-db (Database)
- ✅ order-redis-cache (Redis)
- ✅ gestao-pedidos-web (Web Service)
- ✅ gestao-pedidos-order (Worker)
- ✅ gestao-pedidos-payment (Worker)
- ✅ gestao-pedidos-inventory (Worker)

### 2. Teste do Frontend
```bash
# URL será algo como:
https://gestao-pedidos-web.onrender.com
```

### 3. Health Checks
```bash
# Web Service Health Check
curl https://gestao-pedidos-web.onrender.com/health

# Deve retornar status 200 OK
```

### 4. Logs de Deploy
- Vá em cada serviço no Dashboard
- Clique na aba "Logs" 
- Verifique se não há erros críticos

## 🐛 Troubleshooting

### Deploy Failed?
1. **Verifique os logs** no Dashboard do Render
2. **Problemas comuns:**
   - Build timeout: Render free tier tem limite de 20min
   - Out of memory: Ajuste JAVA_OPTS se necessário
   - Port binding: Web service deve usar $PORT do Render

### Serviço não inicia?
```bash
# Verifique SERVICE_TYPE
echo $SERVICE_TYPE

# Deve ser: web, order, payment, ou inventory
```

### Frontend não carrega?
1. Verifique se `frontend/dist` foi criado no build
2. Nginx deve estar servindo em `/app/frontend`
3. Health check deve responder em `/health`

### Conectividade entre serviços?
- Database e Redis URLs são auto-injetados via Blueprint
- Workers se comunicam via Redis messaging
- Web service expõe API pública

## 📊 Monitoramento

### Métricas no Dashboard
- **CPU Usage**: Deve ficar < 50% em free tier
- **Memory Usage**: Configurado para < 128MB por serviço
- **Response Time**: Objetivo < 2s para requisições
- **Uptime**: 99.9% esperado

### URLs de Produção
```
Frontend: https://gestao-pedidos-web.onrender.com
API: https://gestao-pedidos-web.onrender.com/api
Health: https://gestao-pedidos-web.onrender.com/health
```

## 🎉 Deploy Completo!

Após seguir este guia, você terá:
- ✅ Sistema distribuído completo no ar
- ✅ PostgreSQL configurado  
- ✅ Redis para messaging
- ✅ Frontend React com novo design
- ✅ 4 microserviços Java Spring Boot
- ✅ Event Sourcing funcional
- ✅ Health checks configurados

**URL da aplicação:** https://gestao-pedidos-web.onrender.com

---

*Tempo estimado de deploy: 15-20 minutos*
*Custo: Free tier (PostgreSQL + Redis + 4 serviços)*