# 🐳 DOCKERFILE MULTI-SERVICE - GUIA DE USO

## 📋 Visão Geral

Dockerfile otimizado com **multi-stage build** e **startup inteligente** baseado na variável `SERVICE_TYPE`. Uma única imagem Docker serve para todos os 4 serviços do sistema distribuído.

## 🏗️ Arquitetura Multi-Stage

```dockerfile
Stage 1: shared-events   → Build biblioteca compartilhada
Stage 2: java-services   → Build todos os 4 serviços Java  
Stage 3: frontend-build  → Build React frontend (Vite)
Stage 4: runtime-image   → Imagem final otimizada
```

## 🚀 BUILD da Imagem

```bash
# Build da imagem base (todos os serviços)
docker build -t distributed-order-system:latest .

# Build com cache otimizado
docker build --no-cache -t distributed-order-system:latest .
```

## 🔧 USO por SERVICE_TYPE

### 1. Web Service (Frontend + Query API)
```bash
docker run -d \
  --name web-service \
  -e SERVICE_TYPE=web \
  -e PORT=8080 \
  -p 8080:8080 \
  distributed-order-system:latest
```

**Inclui:**
- ✅ Nginx (frontend React)
- ✅ Query Service (port 8084 interno)
- ✅ Health endpoint: `/health`
- ✅ API proxy: `/api/*` → query-service

### 2. Order Service
```bash
docker run -d \
  --name order-service \
  -e SERVICE_TYPE=order \
  -e DATABASE_URL=... \
  -e RABBITMQ_URL=... \
  -p 8081:8081 \
  distributed-order-system:latest
```

**Recursos:**
- 🎯 Memory: -Xmx96m  
- 🚀 Port: 8081
- 📦 JAR: order-service.jar

### 3. Payment Service  
```bash
docker run -d \
  --name payment-service \
  -e SERVICE_TYPE=payment \
  -e STRIPE_API_KEY=... \
  -e RABBITMQ_URL=... \
  -p 8082:8082 \
  distributed-order-system:latest
```

**Recursos:**
- 🎯 Memory: -Xmx96m
- 🚀 Port: 8082  
- 📦 JAR: payment-service.jar

### 4. Inventory Service
```bash
docker run -d \
  --name inventory-service \
  -e SERVICE_TYPE=inventory \
  -e DATABASE_URL=... \
  -e RABBITMQ_URL=... \
  -p 8083:8083 \
  distributed-order-system:latest
```

**Recursos:**
- 🎯 Memory: -Xmx96m
- 🚀 Port: 8083
- 📦 JAR: inventory-service.jar

## 📋 VARIÁVEIS DE AMBIENTE

### Obrigatórias
| Variável | Valores | Descrição |
|----------|---------|-----------|
| `SERVICE_TYPE` | `web`, `order`, `payment`, `inventory` | **OBRIGATÓRIO** - Tipo do serviço |

### Opcionais  
| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `PORT` | `8080` | Port do nginx (apenas SERVICE_TYPE=web) |
| `JAVA_OPTS` | `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` | Opções JVM |

### Específicas por Serviço
```bash
# Database (order, inventory)
DATABASE_URL=postgresql://user:pass@host:5432/db
DATABASE_USERNAME=user
DATABASE_PASSWORD=pass

# RabbitMQ (todos os services)  
RABBITMQ_URL=amqp://user:pass@host:5672/vhost
RABBITMQ_USERNAME=user
RABBITMQ_PASSWORD=pass

# Payment específico
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Redis (query service)
REDIS_URL=redis://host:6379
```

## 🔍 STARTUP INTELIGENTE

O script `/app/startup.sh` realiza:

1. **Validação SERVICE_TYPE** - Rejeita valores inválidos
2. **Seleção de Config** - Escolhe supervisord.conf apropriado
3. **Nginx Template** - Processa template com `envsubst` (apenas web)  
4. **Validação JARs** - Verifica se JARs necessários existem
5. **Health Check** - Valida configurações antes do start
6. **Exec Supervisord** - Inicia com config apropriado

## 🧪 TESTES

```bash
# Executar suite de testes
./docker-test.sh

# Teste manual individual
docker run --rm \
  -e SERVICE_TYPE=web \
  -e PORT=8080 \
  -p 8080:8080 \
  distributed-order-system:latest
```

## 📊 OTIMIZAÇÕES

### Build Performance
- ✅ **Dependency Caching** - Maven dependencies cached por layer
- ✅ **Multi-stage** - Build paralelo quando possível  
- ✅ **Shared Events** - Built once, used by all services
- ✅ **Frontend Cache** - npm ci --only=production

### Runtime Performance  
- ✅ **JVM Container Support** - `UseContainerSupport=true`
- ✅ **Memory Limits** - MaxRAMPercentage=75.0%
- ✅ **Supervisord** - Process management robusto
- ✅ **Alpine Base** - Imagem final menor

## 🏥 HEALTH CHECKS

### Web Service
```bash
curl http://localhost:8080/health
# {"status":"UP","services":["order","payment","inventory","query"],"frontend":"ok"}
```

### Services Individuais  
```bash
# Order Service
curl http://localhost:8081/actuator/health

# Payment Service  
curl http://localhost:8082/actuator/health

# Inventory Service
curl http://localhost:8083/actuator/health
```

## 🚨 TROUBLESHOOTING

### Logs por Service Type
```bash
# Web service (nginx + query)
docker logs web-service

# Service individual
docker logs order-service
```

### Debug Startup
```bash
# Run interativo para debug
docker run --rm -it \
  -e SERVICE_TYPE=web \
  distributed-order-system:latest \
  /bin/sh
```

### Validar Configs
```bash
# Verificar arquivos de config
docker run --rm \
  distributed-order-system:latest \
  ls -la /app/config/

# Testar nginx template
docker run --rm \
  -e PORT=8080 \
  distributed-order-system:latest \
  envsubst < /app/config/nginx.conf.template
```

## 🐛 ERROS COMUNS

| Erro | Causa | Solução |
|------|-------|---------|
| `SERVICE_TYPE environment variable is required` | SERVICE_TYPE não definido | Definir `SERVICE_TYPE=web/order/payment/inventory` |
| `Configuration file not found` | Config supervisord missing | Verificar se `deploy/supervisord/*.conf` existem |
| `JAR not found` | JAR do serviço não existe | Verificar build Maven e paths dos JARs |
| `Invalid nginx configuration` | Template nginx inválido | Verificar `deploy/nginx/nginx.conf.template` |

## 📈 MONITORAMENTO

```bash
# Container stats
docker stats web-service order-service payment-service inventory-service

# Resource usage
docker exec web-service ps aux
docker exec web-service free -h
docker exec web-service df -h
```

## 🔄 CI/CD Integration

```yaml
# GitHub Actions exemplo
- name: Build Multi-Service Image
  run: |
    docker build -t ${{ env.REGISTRY }}/distributed-order-system:${{ github.sha }} .
    docker tag ${{ env.REGISTRY }}/distributed-order-system:${{ github.sha }} \
               ${{ env.REGISTRY }}/distributed-order-system:latest

- name: Deploy Services
  run: |
    # Deploy each service type
    for service in web order payment inventory; do
      docker run -d \
        --name $service-service \
        -e SERVICE_TYPE=$service \
        ${{ env.REGISTRY }}/distributed-order-system:${{ github.sha }}
    done
```