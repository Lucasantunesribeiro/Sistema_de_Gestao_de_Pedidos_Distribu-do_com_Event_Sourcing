# 🐳 DOCKER FIX GUIDE - Render.com

## ❌ PROBLEMA ATUAL
A imagem `openjdk:17-jre-slim` não existe mais no Docker Hub.

## ✅ SOLUÇÃO

### 1. Execute o fix:
```powershell
.\fix-dockerfile.ps1
```

### 2. Commit e push:
```bash
git add .
git commit -m "Fix Docker images and simplify deployment"
git push origin main
```

### 3. Manual Deploy no Render
Clique em "Manual Deploy" no dashboard.

---

## 🎯 ESTRATÉGIA SIMPLIFICADA

**Em vez de 4 microsserviços**, vamos começar com **apenas 1**:

### ✅ O QUE FOI CORRIGIDO:

1. **Docker Image válida**: `maven:3.9.4-openjdk-17` → `openjdk:17-jdk-slim`
2. **Apenas Order Service**: Foco em 1 serviço funcionando
3. **PostgreSQL configurado**: Database automático do Render
4. **Health check**: `/actuator/health` para monitoring
5. **Build otimizado**: `.dockerignore` para speed

### 📋 RENDER.YAML SIMPLIFICADO:
```yaml
services:
  - type: web
    name: gestao-pedidos
    env: docker
    dockerfilePath: ./Dockerfile
    healthCheckPath: /actuator/health

databases:
  - name: ordersystem-db
    databaseName: ordersystem
```

---

## 🚀 APÓS O DEPLOY FUNCIONAR

Quando o primeiro serviço estiver rodando:

1. ✅ **Teste**: https://gestao-pedidos.onrender.com/actuator/health
2. ✅ **Adicione outros serviços** gradualmente
3. ✅ **Frontend** por último

---

## 🔍 TROUBLESHOOTING

### Se ainda der erro:
1. **Verifique logs** no Render dashboard
2. **Teste build local**: `docker build .`
3. **Simplifique ainda mais** se necessário

### Build local para testar:
```bash
docker build -t test-app .
docker run -p 8080:8080 test-app
```

---

## 💡 PRÓXIMOS PASSOS

1. **Order Service funcionando** ✅
2. **Adicionar Payment Service**
3. **Adicionar Inventory Service** 
4. **Adicionar Query Service**
5. **Frontend React**

**Um por vez = maior chance de sucesso!**