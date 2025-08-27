# Guia de Deploy - Arquitetura Unificada

## Visão Geral

O sistema suporta agora dois tipos de deployment:
1. **Unified**: Todos os serviços em containers isolados usando o mesmo Dockerfile
2. **Legacy**: Dockerfiles separados por serviço (apenas para desenvolvimento)

## Tipos de Serviço

### `SERVICE_TYPE=web`
**Web Service**: nginx + query-service + frontend
- **Porta**: 80
- **Componentes**: 
  - Nginx servindo frontend e proxy para API
  - Query Service (port 8084)
  - Frontend React built
- **Uso**: Ponto de entrada único para usuários

### `SERVICE_TYPE=order`
**Order Service Isolado**
- **Porta**: 8081
- **Componentes**: Order Service apenas
- **Uso**: Processamento de pedidos e event sourcing

### `SERVICE_TYPE=payment`
**Payment Service Isolado**
- **Porta**: 8082
- **Componentes**: Payment Service apenas
- **Uso**: Processamento de pagamentos

### `SERVICE_TYPE=inventory`
**Inventory Service Isolado**
- **Porta**: 8083
- **Componentes**: Inventory Service apenas
- **Uso**: Gestão de estoque e reservas

## Como Usar

### Desenvolvimento Local

```bash
# Usando arquitetura unificada
docker-compose --profile unified up

# Ou legacy (desenvolvimento)
docker-compose --profile dev up
```

### Deploy Individual de Serviços

```bash
# Web service apenas
docker run -e SERVICE_TYPE=web -p 80:80 order-system

# Order service apenas
docker run -e SERVICE_TYPE=order -p 8081:8081 order-system

# Payment service apenas
docker run -e SERVICE_TYPE=payment -p 8082:8082 order-system

# Inventory service apenas
docker run -e SERVICE_TYPE=inventory -p 8083:8083 order-system
```

### Deploy em Cloud Providers

#### Railway/Render
```yaml
# railway.json para web service
{
  "build": {
    "builder": "DOCKERFILE"
  },
  "deploy": {
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 10
  }
}

# Variáveis de ambiente:
SERVICE_TYPE=web
PORT=80
DATABASE_URL=postgresql://...
REDIS_URL=redis://...
RABBITMQ_URL=amqp://...
```

#### Dockerfile único para todos
```dockerfile
# O mesmo Dockerfile constrói todos os serviços
# SERVICE_TYPE determina qual executar
```

## Configurações Específicas

### Web Service
- Requer DATABASE_URL para query-service
- Requer REDIS_URL para cache
- Configura nginx automaticamente
- Health check: `/health`

### Backend Services
- Requer REDIS_URL para messaging
- Requer RABBITMQ_URL para events
- Health check: `/actuator/health`

## Troubleshooting

### Logs do Supervisord
```bash
docker exec -it <container> tail -f /var/log/supervisor/supervisord.log
```

### Logs Específicos por Serviço
```bash
# Web service
docker exec -it web-service tail -f /var/log/supervisor/nginx.out.log
docker exec -it web-service tail -f /var/log/supervisor/query-service.out.log

# Backend services
docker exec -it order-service-unified tail -f /var/log/supervisor/order-service.out.log
```

### Verificação de Configuração
```bash
# Ver qual config está sendo usada
docker exec -it <container> cat /etc/supervisor/configs/*.conf
```

## Vantagens da Arquitetura Unificada

1. **Build único**: Uma image para todos os serviços
2. **Deploy flexível**: SERVICE_TYPE determina comportamento
3. **Resource efficiency**: Cada container roda apenas o necessário
4. **Scaling independente**: Scale cada tipo conforme demanda
5. **Deploy simples**: Mesmo Dockerfile, configs diferentes