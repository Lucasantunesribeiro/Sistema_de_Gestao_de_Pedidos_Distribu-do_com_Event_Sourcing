# 🚀 RENDER.COM QUICK FIX

## ❌ PROBLEMA IDENTIFICADO
O Render.com está tentando usar Docker mas não encontra Dockerfile na raiz do projeto.

## ✅ SOLUÇÃO RÁPIDA

### 1. Execute o script de correção:
```powershell
.\fix-render-deploy.ps1
```

### 2. Commit e push:
```bash
git add .
git commit -m "Fix Render deploy with Dockerfile"
git push origin main
```

### 3. No dashboard do Render:
- Clique em **"Manual Deploy"**
- O deploy será refeito automaticamente

---

## 🔧 ALTERNATIVA SIMPLES

Se preferir fazer manualmente:

### 1. Criar Dockerfile na raiz:
```dockerfile
FROM openjdk:17-jdk-slim as builder
WORKDIR /app
COPY . .
RUN cd shared-events && mvn clean install -DskipTests
RUN cd services/order-service && mvn clean package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=builder /app/services/order-service/target/order-service-1.0.0.jar ./app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### 2. Simplificar render.yaml:
```yaml
services:
  - type: web
    name: order-service
    env: docker
    dockerfilePath: ./Dockerfile
```

---

## 🎯 DEPLOY SIMPLIFICADO

Como alternativa, pode usar apenas um serviço por vez:

1. **Foque apenas no order-service primeiro**
2. **Depois adicione os outros services**
3. **Frontend por último**

### Para deploy único:
1. Remova outros services do render.yaml
2. Mantenha apenas order-service
3. Teste se funciona
4. Adicione os outros gradualmente

---

## 📋 STATUS ATUAL

Você tem um projeto configurado no Render:
- **Service ID**: srv-d2kbhnruibrs73emmc8g
- **URL**: https://gestao-de-pedidos.onrender.com
- **Status**: Failed (missing Dockerfile)

Execute o fix e tente novamente!