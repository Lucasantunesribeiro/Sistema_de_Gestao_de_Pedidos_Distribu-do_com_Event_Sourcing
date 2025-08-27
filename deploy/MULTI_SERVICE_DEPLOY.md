# Deploy Multi-Serviços no Render - Configuração Completa

## 🎯 Arquitetura Implementada

A nova configuração separa a aplicação em **4 serviços independentes**:

```
┌─────────────────────────────────────────────────────┐
│                RENDER CLOUD                         │
│                                                     │
│  ┌─────────────────┐    ┌──────────────────────────┐ │
│  │   Web Service   │    │     PostgreSQL DB       │ │
│  │ - Nginx         │◄──►│   order-postgres-db     │ │
│  │ - React Frontend│    │                          │ │
│  │ - Query Service │    └──────────────────────────┘ │
│  │   (port 80)     │                                 │
│  └─────────────────┘    ┌──────────────────────────┐ │
│                         │       Redis Cache        │ │
│  ┌─────────────────┐◄──►│   order-redis-cache      │ │
│  │  Order Worker   │    │                          │ │
│  │   (port 8081)   │    └──────────────────────────┘ │
│  └─────────────────┘                                 │
│                                                     │
│  ┌─────────────────┐                                 │
│  │ Payment Worker  │                                 │
│  │   (port 8082)   │                                 │
│  └─────────────────┘                                 │
│                                                     │
│  ┌─────────────────┐                                 │
│  │Inventory Worker │                                 │
│  │   (port 8083)   │                                 │
│  └─────────────────┘                                 │
└─────────────────────────────────────────────────────┘
```

## 📁 Arquivos Modificados/Criados

### ✅ Configurações de Deploy
- **`render.yaml`** - Configuração com 4 serviços separados
- **`.github/workflows/render-deploy-multi.yml`** - Pipeline CI/CD completo
- **`.github/workflows/render-ci.yml`** - Desabilitado (legacy)

### ✅ Scripts de Configuração  
- **`deploy/setup-render-secrets.sh`** - Automação de configuração de secrets
- **`deploy/RENDER_SECRETS_CONFIG.md`** - Instruções detalhadas
- **`deploy/MULTI_SERVICE_DEPLOY.md`** - Esta documentação

### ✅ Configurações do Supervisor
- **`deploy/supervisord/web.conf`** - Nginx + Query Service
- **`deploy/supervisord/order.conf`** - Order Worker isolado
- **`deploy/supervisord/payment.conf`** - Payment Worker isolado
- **`deploy/supervisord/inventory.conf`** - Inventory Worker isolado

## 🚀 Como Fazer o Deploy

### Método 1: Automático (Recomendado)

```bash
# 1. Obtenha sua API Key do Render
# https://dashboard.render.com/account → API Keys

# 2. Execute o script de configuração
./deploy/setup-render-secrets.sh <SUA_RENDER_API_KEY>

# 3. Faça push para main
git add .
git commit -m "feat: implement multi-service render architecture"
git push origin main

# 4. Acompanhe o deploy
# GitHub: https://github.com/seu-usuario/seu-repo/actions
# Render: https://dashboard.render.com
```

### Método 2: Manual

1. **Configure Secrets no GitHub**:
   - `Settings` → `Secrets and variables` → `Actions`
   - Adicione `RENDER_API_KEY` em "Repository secrets"

2. **Faça o primeiro deploy**:
   ```bash
   git push origin main
   ```

3. **Após criar serviços, configure Service IDs**:
   - Vá para Render Dashboard
   - Copie os IDs dos 4 serviços criados
   - Adicione em "Repository variables":
     - `RENDER_WEB_SERVICE_ID`
     - `RENDER_ORDER_SERVICE_ID`
     - `RENDER_PAYMENT_SERVICE_ID`
     - `RENDER_INVENTORY_SERVICE_ID`

## 📊 Monitoramento e Verificação

### URLs de Produção
- **Frontend**: https://gestao-de-pedidos-web.onrender.com
- **API Health**: https://gestao-de-pedidos-web.onrender.com/health
- **API Orders**: https://gestao-de-pedidos-web.onrender.com/api/orders

### Workers (Sem URL pública)
- `gestao-de-pedidos-order` - Processamento de pedidos
- `gestao-de-pedidos-payment` - Processamento de pagamentos  
- `gestao-de-pedidos-inventory` - Gerenciamento de estoque

### Verificação via API
```bash
# Status dos serviços
curl -H "Authorization: Bearer $API_KEY" \
     "https://api.render.com/v1/services" | \
     jq '.services[] | select(.name | startswith("gestao-de-pedidos")) | {name, status}'

# Health check
curl https://gestao-de-pedidos-web.onrender.com/health
```

## ⚡ Otimizações Implementadas

### Performance
- **Web Service**: 128MB RAM (nginx + query + frontend)
- **Workers**: 96MB RAM cada (otimizado para background)
- **JVM**: `-Xmx96m -XX:+UseContainerSupport`
- **Cache**: Maven cache no CI/CD

### CI/CD
- **Build paralelo** de frontend e backend
- **Testes com serviços reais** (PostgreSQL + Redis)
- **Deploy simultâneo** de todos os serviços
- **Security scan** automático
- **Rollback automático** em caso de falha

### Monitoramento
- **Health checks** configurados
- **Logs centralizados** no Render Dashboard  
- **Deploy status** no GitHub Actions
- **Error tracking** com stack traces

## 🔧 Configurações Avançadas

### Variáveis de Ambiente por Serviço

**Web Service** (gestao-de-pedidos-web):
```yaml
SERVICE_TYPE: web
JAVA_OPTS: "-Xmx128m -XX:+UseContainerSupport"
```

**Order Worker** (gestao-de-pedidos-order):
```yaml
SERVICE_TYPE: order  
JAVA_OPTS: "-Xmx96m -XX:+UseContainerSupport"
```

**Payment Worker** (gestao-de-pedidos-payment):
```yaml
SERVICE_TYPE: payment
JAVA_OPTS: "-Xmx96m -XX:+UseContainerSupport"  
```

**Inventory Worker** (gestao-de-pedidos-inventory):
```yaml
SERVICE_TYPE: inventory
JAVA_OPTS: "-Xmx96m -XX:+UseContainerSupport"
```

### Dockerfile Multi-Stage
- **Stage 1**: Build shared events library
- **Stage 2**: Build Java services com cache Maven  
- **Stage 3**: Build React frontend
- **Stage 4**: Runtime unificado com seleção dinâmica

### Supervisor Configuration
Cada serviço usa um `supervisord.conf` específico baseado em `SERVICE_TYPE`:
- **web**: nginx + query-service
- **order**: order-service isolado
- **payment**: payment-service isolado  
- **inventory**: inventory-service isolado

## 🚨 Troubleshooting

### Deploy Falha
```bash
# Verificar logs do GitHub Actions
# Verificar logs no Render Dashboard → Service → Logs

# Testar container localmente
docker build -t test .
docker run -e SERVICE_TYPE=web -p 8080:80 test
```

### Worker não Inicia
```bash
# Verificar variáveis de ambiente
# Confirmar DATABASE_URL e REDIS_URL
# Testar SERVICE_TYPE correto

# Debug container
docker run -it -e SERVICE_TYPE=order test /bin/sh
```

### Performance Issues
```bash
# Monitorar métricas no Render
# Ajustar JAVA_OPTS se necessário
# Verificar query performance no PostgreSQL
```

## 📈 Custos Estimados (Render Starter Plan)

- **PostgreSQL DB**: $7/mês
- **Redis Cache**: $7/mês  
- **Web Service**: $7/mês (80% CPU/memoria)
- **Order Worker**: $7/mês (20% CPU/memoria)
- **Payment Worker**: $7/mês (20% CPU/memoria)
- **Inventory Worker**: $7/mês (20% CPU/memoria)

**Total**: ~$42/mês (muito otimizado para produção)

## 🎉 Benefícios da Nova Arquitetura

### Escalabilidade
- **Isolamento** de serviços por responsabilidade
- **Scaling independente** de cada worker
- **Zero downtime** para atualizações isoladas

### Manutenibilidade
- **Deploy independente** de cada serviço
- **Logs isolados** para debugging
- **Rollback granular** por serviço

### Performance
- **Web Service** otimizado para requisições HTTP
- **Workers** otimizados para processamento em background
- **Cache Redis** compartilhado eficientemente

### Monitoramento
- **Health checks** individuais
- **Métricas isoladas** por serviço
- **Alertas granulares** por componente

---

🔗 **Links Úteis**:
- [Render Dashboard](https://dashboard.render.com)
- [GitHub Actions](../../actions)
- [API Documentation](./API.md)