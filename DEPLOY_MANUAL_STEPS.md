# 🚀 Deploy Manual Passo-a-Passo - Render Dashboard

O MCP do Render não suporta planos gratuitos, então segue o processo simplificado para criar via Dashboard:

## ✅ Recursos já criados automaticamente:

1. **Static Site (Frontend)**: 
   - Nome: `gestao-pedidos-frontend`
   - URL: https://gestao-pedidos-frontend.onrender.com
   - Status: ✅ Criado e fazendo build

2. **PostgreSQL Database**:
   - Nome: `order-system-postgres` 
   - ID: `dpg-d2nr367fte5s7381n0n0-a`
   - Status: ✅ Disponível

3. **Redis Cache**:
   - Nome: `order-system-redis`
   - ID: `red-d2nr3795pdvs7394onhg` 
   - Status: ✅ Disponível

## 🔧 Próximos passos manuais:

### 1. Criar Web Service (API)
Vá para https://dashboard.render.com e clique em "New" → "Web Service"

```
Name: gestao-pedidos-api
Runtime: Docker
Repository: https://github.com/Lucasantunesribeiro/Sistema_de_Gestao_de_Pedidos_Distribu-do_com_Event_Sourcing
Branch: main
Plan: Free
Region: Oregon

Environment Variables:
SERVICE_TYPE=web
MESSAGING_TYPE=redis
SPRING_PROFILES_ACTIVE=render
JAVA_OPTS=-Xmx128m -XX:+UseContainerSupport
DATABASE_URL=[Connect to order-system-postgres]
REDIS_URL=[Connect to order-system-redis]
```

### 2. Criar Worker Services
Repita para cada worker (3 vezes):

**Order Worker:**
```
Name: gestao-pedidos-order
Runtime: Docker
[Same repo]
Plan: Free
Region: Oregon

Environment Variables:
SERVICE_TYPE=order
[Same DATABASE_URL and REDIS_URL]
SERVER_PORT=8081
```

**Payment Worker:**
```
Name: gestao-pedidos-payment
SERVICE_TYPE=payment
SERVER_PORT=8082
[Same other configs]
```

**Inventory Worker:**
```
Name: gestao-pedidos-inventory
SERVICE_TYPE=inventory
SERVER_PORT=8083
[Same other configs]
```

## 🎯 Resultado esperado:

Depois de 15-20 minutos:
- ✅ Frontend: https://gestao-pedidos-frontend.onrender.com
- ✅ API: https://gestao-pedidos-api.onrender.com
- ✅ 3 Workers processando em background
- ✅ Sistema completo funcionando

## 📋 Status atual:

**✅ Completado via MCP:**
- Static Site criado e fazendo build
- PostgreSQL e Redis disponíveis

**⏳ Pendente (manual):**
- Web Service para API
- 3 Worker Services

**Tempo estimado restante:** 10 minutos de configuração + 15 minutos de build