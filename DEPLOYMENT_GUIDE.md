# 🚀 Deployment Guide - Distributed Order System

Este guia fornece instruções completas para deploy do sistema de gestão de pedidos distribuído tanto localmente quanto no Railway.app.

## 📋 Índice

- [Pré-requisitos](#pré-requisitos)
- [Deploy Local com Docker](#deploy-local-com-docker)
- [Deploy no Railway.app](#deploy-no-railwayapp)
- [Verificação de Saúde](#verificação-de-saúde)
- [Troubleshooting](#troubleshooting)
- [Monitoramento](#monitoramento)

## 🛠️ Pré-requisitos

### Para Deploy Local
- **Docker** 20.10+ e **Docker Compose** 2.0+
- **Java 17** (para desenvolvimento)
- **Maven 3.6+** (para build local)
- **Git** para clonagem do repositório
- **curl** para testes de API
- **netcat (nc)** para testes de conectividade

### Para Deploy no Railway
- **Railway CLI** instalado e configurado
- Conta no **Railway.app**
- **Git** configurado com acesso ao repositório

## 🐳 Deploy Local com Docker

### 1. Preparação do Ambiente

```bash
# Clone o repositório
git clone <repository-url>
cd distributed-order-system

# Verifique se o Docker está rodando
docker --version
docker-compose --version
```

### 2. Build e Inicialização

```bash
# Limpe deployments anteriores (opcional)
docker-compose down -v --remove-orphans

# Inicie todos os serviços
docker-compose up --build -d
```

### 3. Verificação da Inicialização

```bash
# Execute o script de teste automatizado
./scripts/test-local-deployment.sh

# Ou faça verificação rápida
./scripts/quick-health-check.sh

# Ou verifique manualmente
docker-compose ps
```

### 4. Endpoints Locais

| Serviço | URL | Health Check |
|---------|-----|--------------|
| Order Service | http://localhost:8081 | http://localhost:8081/api/orders/actuator/health |
| Payment Service | http://localhost:8082 | http://localhost:8082/api/payments/actuator/health |
| Inventory Service | http://localhost:8083 | http://localhost:8083/api/inventory/actuator/health |
| Query Service | http://localhost:8084 | http://localhost:8084/api/orders/actuator/health |
| RabbitMQ Management | http://localhost:15672 | guest/guest |

### 5. Teste de Integração

```bash
# Criar um pedido
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-001",
    "items": [
      {
        "productId": "product-001",
        "productName": "Test Product",
        "quantity": 2,
        "price": 29.99
      }
    ]
  }'

# Consultar pedidos
curl http://localhost:8084/api/orders
```

## 🚂 Deploy no Railway.app

### 1. Configuração Inicial

```bash
# Instale o Railway CLI
npm install -g @railway/cli

# Faça login
railway login

# Navegue para o diretório do projeto
cd distributed-order-system
```

### 2. Deploy Automatizado

```bash
# Execute o script de deploy
./scripts/deploy-railway.sh
```

### 3. Deploy Manual (Alternativo)

```bash
# Inicialize o projeto
railway init distributed-order-system

# Adicione bancos de dados PostgreSQL
railway add --database postgresql  # Para order-db
railway add --database postgresql  # Para query-db

# Adicione RabbitMQ
railway add --template rabbitmq

# Deploy cada serviço
cd services/order-service
railway up --detach

cd ../payment-service
railway up --detach

cd ../inventory-service
railway up --detach

cd ../order-query-service
railway up --detach
```

### 4. Configuração de Variáveis

O Railway configurará automaticamente as variáveis de ambiente, mas você pode verificar:

```bash
# Listar variáveis
railway variables

# Adicionar variável personalizada
railway variables set CUSTOM_VAR=value
```

### 5. Verificação do Deploy

```bash
# Verificar status
railway status

# Ver logs
railway logs --service order-service

# Obter URLs dos serviços
railway url --service order-service
```

## 🏥 Verificação de Saúde

### Health Checks Automáticos

Todos os serviços incluem health checks abrangentes:

```bash
# Verificação rápida de todos os serviços
./scripts/quick-health-check.sh

# Verificação detalhada com relatório
./scripts/test-local-deployment.sh
```

### Health Checks Manuais

```bash
# Verificar saúde individual dos serviços
curl http://localhost:8081/api/orders/actuator/health
curl http://localhost:8082/api/payments/actuator/health
curl http://localhost:8083/api/inventory/actuator/health
curl http://localhost:8084/api/orders/actuator/health

# Verificar métricas
curl http://localhost:8081/api/orders/actuator/metrics
```

### Indicadores de Saúde

Cada serviço monitora:
- ✅ **Database**: Conectividade e pool de conexões
- ✅ **RabbitMQ**: Conectividade e filas
- ✅ **Application**: Status de inicialização
- ✅ **Dependencies**: Verificação de dependências

## 🔧 Troubleshooting

### Problemas Comuns

#### 1. Serviços não inicializam

```bash
# Verificar logs
docker-compose logs order-service
docker-compose logs payment-service

# Verificar recursos
docker stats

# Reiniciar serviços específicos
docker-compose restart order-service
```

#### 2. Problemas de conectividade

```bash
# Verificar rede Docker
docker network ls
docker network inspect distributed-order-system_order-network

# Testar conectividade entre containers
docker exec order-service ping order-db
docker exec order-service ping rabbitmq
```

#### 3. Problemas de banco de dados

```bash
# Conectar ao banco diretamente
docker exec -it order-db psql -U postgres -d order_db
docker exec -it query-db psql -U postgres -d order_query_db

# Verificar logs do banco
docker-compose logs order-db
docker-compose logs query-db
```

#### 4. Problemas com RabbitMQ

```bash
# Verificar status do RabbitMQ
docker exec rabbitmq rabbitmq-diagnostics status

# Acessar management interface
# http://localhost:15672 (guest/guest)

# Verificar filas
docker exec rabbitmq rabbitmqctl list_queues
```

### Logs e Debugging

```bash
# Ver logs em tempo real
docker-compose logs -f

# Logs de serviço específico
docker-compose logs -f order-service

# Logs com timestamp
docker-compose logs -t order-service

# Últimas 100 linhas
docker-compose logs --tail=100 order-service
```

### Limpeza e Reset

```bash
# Parar todos os serviços
docker-compose down

# Remover volumes (CUIDADO: apaga dados)
docker-compose down -v

# Limpeza completa
docker-compose down -v --remove-orphans
docker system prune -f
```

## 📊 Monitoramento

### Métricas Disponíveis

Cada serviço expõe métricas via Actuator:

```bash
# Métricas gerais
curl http://localhost:8081/api/orders/actuator/metrics

# Métricas específicas
curl http://localhost:8081/api/orders/actuator/metrics/jvm.memory.used
curl http://localhost:8081/api/orders/actuator/metrics/hikaricp.connections.active
```

### Endpoints de Monitoramento

| Endpoint | Descrição |
|----------|-----------|
| `/actuator/health` | Status de saúde |
| `/actuator/info` | Informações da aplicação |
| `/actuator/metrics` | Métricas da aplicação |
| `/actuator/prometheus` | Métricas no formato Prometheus |

### Logs Estruturados

Os logs são configurados com:
- ✅ **Formato JSON** para parsing automático
- ✅ **Correlation IDs** para rastreamento
- ✅ **Níveis apropriados** por ambiente
- ✅ **Rotação automática** de arquivos

## 🚨 Alertas e Notificações

### Configuração de Alertas

Para produção, configure alertas para:

- ❌ **Health checks falhando**
- ❌ **Alta latência** (>5s)
- ❌ **Erros de conectividade**
- ❌ **Pool de conexões esgotado**
- ❌ **Filas RabbitMQ crescendo**
- ❌ **Uso de memória >80%**

### Monitoramento Contínuo

```bash
# Script de monitoramento contínuo
watch -n 30 './scripts/quick-health-check.sh'

# Monitoramento de recursos
watch -n 10 'docker stats --no-stream'
```

## 📚 Recursos Adicionais

### Documentação Técnica
- [Arquitetura do Sistema](ARCHITECTURE.md)
- [Guia de Desenvolvimento](README.md)
- [Configuração de CI/CD](.github/workflows/)

### Scripts Úteis
- `scripts/test-local-deployment.sh` - Teste completo de deployment
- `scripts/quick-health-check.sh` - Verificação rápida de saúde
- `scripts/deploy-railway.sh` - Deploy automatizado no Railway

### Suporte
- 📧 **Issues**: Use o sistema de issues do repositório
- 📖 **Wiki**: Documentação adicional no wiki do projeto
- 🔧 **Logs**: Sempre inclua logs relevantes ao reportar problemas

---

## ✅ Checklist de Deploy

### Deploy Local
- [ ] Docker e Docker Compose instalados
- [ ] Repositório clonado
- [ ] `docker-compose up --build -d` executado
- [ ] Health checks passando
- [ ] Teste de integração realizado

### Deploy Railway
- [ ] Railway CLI instalado e autenticado
- [ ] Projeto criado no Railway
- [ ] Bancos de dados configurados
- [ ] RabbitMQ configurado
- [ ] Serviços deployados
- [ ] URLs funcionando
- [ ] Health checks passando

---

**🎉 Parabéns! Seu sistema distribuído está rodando com sucesso!**