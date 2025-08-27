# 🚀 Guia de Deploy - Render.com

Este documento fornece instruções detalhadas para deploy do sistema no Render.com.

## 📋 Pré-requisitos

- Conta no [Render.com](https://render.com)
- Repositório no GitHub
- Redis service configurado
- Domínio/subdomínio (opcional)

## 🏗️ Arquitetura de Deploy

### Serviços no Render.com

```
┌─────────────────────────────────────────────────────────┐
│                    Web Service                          │
│  https://gestao-de-pedidos.onrender.com                │
│                                                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │
│  │   Nginx     │ │  Frontend   │ │  Query Service  │   │
│  │ (Proxy)     │ │   (SPA)     │ │   (Port 8084)   │   │
│  │ Port ${PORT}│ │   Static    │ │     Java        │   │
│  └─────────────┘ └─────────────┘ └─────────────────┘   │
└─────────────────────────────────────────────────────────┘
                              │
                        ┌─────▼─────┐
                        │   Redis   │
                        │  Service  │
                        │ (Streams) │
                        └─────┬─────┘
                              │
┌─────────────────────────────┴─────────────────────────────┐
│              Background Workers                           │
│                                                           │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐  │
│ │Order Service│ │Payment      │ │ Inventory Service   │  │
│ │(Port 8081)  │ │Service      │ │   (Port 8083)       │  │
│ │   Java      │ │(Port 8082)  │ │     Java            │  │
│ └─────────────┘ └─────────────┘ └─────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## ⚙️ Configuração dos Serviços

### 1. Redis Service

```bash
# Criar Redis service
Service Name: order-system-redis
Plan: Free (25MB)
```

**Obter URL do Redis**: Copie a URL interna para usar nos outros serviços.

### 2. Web Service (Principal)

```yaml
Name: gestao-de-pedidos
Runtime: Docker
Repository: seu-repo-github
Branch: main
Root Directory: .

# Build Command
docker build -t order-system .

# Start Command  
./startup.sh

# Environment Variables:
SERVICE_TYPE=web
PORT=10000
REDIS_URL=redis://order-system-redis:6379
```

### 3. Background Workers

#### Order Service
```yaml
Name: order-service-worker
Runtime: Docker
Repository: seu-repo-github
Branch: main
Root Directory: .

# Environment Variables:
SERVICE_TYPE=order
REDIS_URL=redis://order-system-redis:6379
```

#### Payment Service
```yaml
Name: payment-service-worker  
Runtime: Docker
Repository: seu-repo-github
Branch: main
Root Directory: .

# Environment Variables:
SERVICE_TYPE=payment
REDIS_URL=redis://order-system-redis:6379
```

#### Inventory Service
```yaml
Name: inventory-service-worker
Runtime: Docker  
Repository: seu-repo-github
Branch: main
Root Directory: .

# Environment Variables:
SERVICE_TYPE=inventory
REDIS_URL=redis://order-system-redis:6379
```

## 🔧 Configurações Avançadas

### Health Checks

```yaml
# Web Service Health Check
Path: /health
Port: ${PORT}
Initial Delay: 60s
Period: 30s
Timeout: 10s
Failure Threshold: 3
```

### Resource Limits

```yaml
# Por serviço
Memory: 512MB
CPU: 0.5 cores
Disk: 1GB

# Total do sistema
Memory: ~2GB (4 services)
```

### Auto Deploy

```yaml
# Configurar no GitHub
Auto Deploy: Yes
Build Command: docker build -t order-system .
```

## 📁 Estrutura de Arquivos Críticos

### Dockerfile (Multi-stage)
```dockerfile
# Stage 1: Build shared events
FROM maven:3.9-openjdk-21 as shared-events
# ... build logic

# Stage 2: Build Java services  
FROM maven:3.9-openjdk-21 as java-builder
# ... build all services

# Stage 3: Build frontend
FROM node:18-alpine as frontend-builder
# ... build static frontend

# Stage 4: Runtime
FROM openjdk:21-jre-slim
# ... copy artifacts and setup
```

### startup.sh (Service Detection)
```bash
#!/bin/bash
SERVICE_TYPE=${SERVICE_TYPE:-${RENDER_SERVICE_TYPE:-web}}

case $SERVICE_TYPE in
  "web")
    envsubst < /app/nginx.conf.template > /etc/nginx/nginx.conf
    nginx &
    exec supervisord -c /app/deploy/supervisord/web.conf
    ;;
  "order")
    exec supervisord -c /app/deploy/supervisord/order.conf  
    ;;
  # ... outros serviços
esac
```

### nginx.conf.template (Dynamic Port)
```nginx
server {
    listen $PORT;  # Dynamic port from Render
    
    location /health {
        proxy_pass http://localhost:8084/health;
        error_page 502 503 504 = @health_fallback;
    }
    
    location /api/ {
        proxy_pass http://localhost:8084/api/;
        error_page 502 503 504 = @api_fallback;
    }
    
    location / {
        root /app/frontend;
        try_files $uri $uri/ /index.html;
    }
}
```

## 🎯 Processo de Deploy

### Deploy Inicial

1. **Preparar Repositório**
   ```bash
   git add .
   git commit -m "feat: prepare for Render deploy"
   git push origin main
   ```

2. **Criar Redis Service**
   - Render Dashboard → New → Redis
   - Nome: `order-system-redis`
   - Plan: Free

3. **Criar Web Service**
   - Render Dashboard → New → Web Service
   - Conectar GitHub repo
   - Configurar variables de ambiente
   - Deploy automático

4. **Criar Background Workers**
   - Para cada serviço (order, payment, inventory)
   - Mesmo repo, diferentes SERVICE_TYPE
   - Mesmo REDIS_URL

### Deploy de Atualizações

1. **Commit Changes**
   ```bash
   git add .
   git commit -m "feat: update functionality"
   git push origin main
   ```

2. **Auto Deploy**
   - Render detecta mudanças automaticamente
   - Build e deploy em ~3-5 minutos
   - Health checks validam funcionamento

## 🔍 Monitoramento e Debug

### Logs por Serviço

```bash
# Web Service Logs
Render Dashboard → gestao-de-pedidos → Logs

# Background Workers Logs  
Render Dashboard → order-service-worker → Logs
```

### Health Check URLs

```bash
# Sistema geral
https://gestao-de-pedidos.onrender.com/health

# API endpoints
https://gestao-de-pedidos.onrender.com/api/orders
```

### Métricas de Performance

- **Cold Start**: ~60-90s (primeira requisição)
- **Warm Response**: ~100-300ms
- **Memory Usage**: ~400MB (web service)
- **Auto-sleep**: Após 15min inatividade (Free plan)

## 🚨 Troubleshooting

### Problemas Comuns

1. **Port Binding Error**
   ```bash
   # Verificar se nginx está usando ${PORT}
   envsubst < nginx.conf.template > nginx.conf
   ```

2. **Service Discovery**
   ```bash
   # Verificar SERVICE_TYPE detection
   echo "SERVICE_TYPE=${SERVICE_TYPE}"
   ```

3. **Memory Limits**
   ```bash  
   # JVM tuning para 512MB
   -Xmx128m -XX:+UseContainerSupport
   ```

4. **Health Check Failure**
   ```bash
   # Verificar fallback endpoints
   curl https://app.onrender.com/health
   ```

### Comandos Debug

```bash
# Container status
supervisorctl status

# Logs em tempo real
tail -f /var/log/supervisor/*.log

# Processes running
ps aux | grep java
```

## 🔒 Segurança

### Environment Variables
```bash
# Não expor em logs
REDIS_URL=***
DATABASE_URL=***

# Headers de segurança no Nginx
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
```

### Network Policies
```bash
# Apenas web service exposto publicamente
# Background workers em rede privada
# Redis interno apenas
```

## 📊 Custos Estimados

### Free Tier
```
Web Service: Free (750h/mês)
Background Workers: Free (750h/mês cada)
Redis: Free (25MB)
Bandwidth: Free (100GB/mês)

Total: $0/mês
```

### Production Tier
```
Web Service: $7/mês (Starter)
Background Workers: $7/mês cada (3x)
Redis: $7/mês (Starter)

Total: ~$35/mês
```

---

## 🎉 Deploy Completo!

Após seguir este guia, seu sistema estará executando em:

**🌐 https://gestao-de-pedidos.onrender.com**

### Validação do Deploy
- ✅ Health check respondendo
- ✅ Frontend carregando
- ✅ API endpoints funcionando
- ✅ Todos os microsserviços ativos
- ✅ Event sourcing operacional