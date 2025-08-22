# 🚀 QUICK DEPLOY GUIDE - Windows

## ⚡ EXECUÇÃO RÁPIDA

```powershell
# 1. Instalar Railway CLI (se não tiver)
npm install -g @railway/cli

# 2. Executar deployment automatizado
.\deploy-windows.ps1
```

## 🔧 SETUP MANUAL (se preferir)

### 1. Railway CLI Setup
```powershell
# Instalar Railway CLI
npm install -g @railway/cli

# Login
railway login

# Inicializar projeto (se novo)
railway init
```

### 2. Deploy Individual Services

```powershell
# Shared Events (obrigatório primeiro)
cd shared-events
mvn clean install -DskipTests
cd ..

# Order Service
cd services\order-service
mvn clean package -DskipTests
railway up --detach
cd ..\..

# Payment Service  
cd services\payment-service
mvn clean package -DskipTests
railway up --detach
cd ..\..

# Inventory Service
cd services\inventory-service  
mvn clean package -DskipTests
railway up --detach
cd ..\..

# Query Service
cd services\order-query-service
mvn clean package -DskipTests
railway up --detach
cd ..\..

# Frontend
cd frontend
npm install
npm run build
railway up --detach
cd ..
```

### 3. Verificar Status
```powershell
railway status
railway open
```

## 🌐 URLs Pós-Deploy

Após deployment você terá:
- **Frontend**: https://{seu-frontend}.railway.app
- **APIs**: https://{seu-service}.railway.app
- **Dashboard**: `railway open`

## 🔍 Comandos Úteis

```powershell
# Status dos serviços
railway status

# Logs em tempo real
railway logs

# Abrir dashboard
railway open

# Redeploy
railway up --detach

# Variáveis de ambiente
railway variables
```

## ⚠️ Troubleshooting

### Railway CLI não encontrado:
```powershell
npm install -g @railway/cli
```

### Erro de autenticação:
```powershell
railway login
```

### Build errors:
```powershell
# Limpar e rebuildar
mvn clean install -DskipTests -U
```

### Port conflicts:
Railway automaticamente atribui portas. Use variável `$PORT` no código.

## 🎯 Performance Targets Configurados

- ✅ APIs < 100ms
- ✅ Frontend < 1.5s  
- ✅ Cache hit ratio > 80%
- ✅ 99.9% availability
- ✅ Auto-scaling enabled

## 🔐 Security Features Ativas

- ✅ CORS restritivo para Railway domains
- ✅ Rate limiting 1000 req/min
- ✅ JWT rotation automático
- ✅ RBAC completo
- ✅ Audit logging
- ✅ Secrets management

## 📊 Monitoring Incluído

- ✅ Health checks: `/actuator/health`
- ✅ Metrics: `/actuator/metrics`  
- ✅ Correlation ID tracking
- ✅ Circuit breaker status
- ✅ Performance monitoring

🎉 **SISTEMA PRODUCTION-READY!**