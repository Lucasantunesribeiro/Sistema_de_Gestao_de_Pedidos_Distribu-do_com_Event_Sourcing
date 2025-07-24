# Guia de Deploy no Railway.app

Este guia fornece instruções passo a passo para fazer deploy do Sistema de Gestão de Pedidos Distribuído no Railway.app.

## Pré-requisitos

1. Conta no Railway.app (gratuita)
2. Repositório no GitHub com o código do projeto
3. Conhecimento básico de variáveis de ambiente

## Passo 1: Preparar o Repositório

1. Certifique-se de que todos os arquivos estão no GitHub:
   - `shared-events/` - Componentes compartilhados
   - `services/` - Todos os 4 microsserviços
   - `docker-compose.yml` - Para referência
   - `README.md` - Documentação

2. Cada microsserviço deve ter seu `Dockerfile` na raiz do serviço

## Passo 2: Criar Projeto no Railway

1. Acesse https://railway.app
2. Faça login com sua conta GitHub
3. Clique em "New Project"
4. Selecione "Deploy from GitHub repo"
5. Escolha o repositório do projeto

## Passo 3: Configurar Bancos de Dados

### PostgreSQL para Order Service (Event Store)

1. No dashboard do Railway, clique em "New"
2. Selecione "Database" → "PostgreSQL"
3. Nomeie como "order-eventstore-db"
4. Anote as variáveis de ambiente geradas:
   - `PGHOST`
   - `PGPORT`
   - `PGDATABASE`
   - `PGUSER`
   - `PGPASSWORD`

### PostgreSQL para Order Query Service (Read Model)

1. Adicione outro PostgreSQL
2. Nomeie como "order-query-db"
3. Anote as variáveis de ambiente (diferentes das primeiras)

### RabbitMQ

1. Clique em "New" → "Database" → "RabbitMQ"
2. Nomeie como "order-rabbitmq"
3. Anote as variáveis de ambiente:
   - `RABBITMQ_HOST`
   - `RABBITMQ_PORT`
   - `RABBITMQ_USER`
   - `RABBITMQ_PASSWORD`

## Passo 4: Configurar Microsserviços

### Order Service

1. Clique em "New" → "Empty Service"
2. Nomeie como "order-service"
3. Conecte ao repositório GitHub
4. Configure "Root Directory" como `services/order-service`
5. Configure as variáveis de ambiente:

```
DATABASE_URL=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
DATABASE_USERNAME=${PGUSER}
DATABASE_PASSWORD=${PGPASSWORD}
RABBITMQ_HOST=${RABBITMQ_HOST}
RABBITMQ_PORT=${RABBITMQ_PORT}
RABBITMQ_USERNAME=${RABBITMQ_USER}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
```

### Payment Service

1. Adicione novo serviço
2. Nomeie como "payment-service"
3. Root Directory: `services/payment-service`
4. Variáveis de ambiente:

```
RABBITMQ_HOST=${RABBITMQ_HOST}
RABBITMQ_PORT=${RABBITMQ_PORT}
RABBITMQ_USERNAME=${RABBITMQ_USER}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
```

### Inventory Service

1. Adicione novo serviço
2. Nomeie como "inventory-service"
3. Root Directory: `services/inventory-service`
4. Variáveis de ambiente:

```
RABBITMQ_HOST=${RABBITMQ_HOST}
RABBITMQ_PORT=${RABBITMQ_PORT}
RABBITMQ_USERNAME=${RABBITMQ_USER}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
```

### Order Query Service

1. Adicione novo serviço
2. Nomeie como "order-query-service"
3. Root Directory: `services/order-query-service`
4. Variáveis de ambiente:

```
DATABASE_URL=jdbc:postgresql://${PGHOST_QUERY}:${PGPORT_QUERY}/${PGDATABASE_QUERY}
DATABASE_USERNAME=${PGUSER_QUERY}
DATABASE_PASSWORD=${PGPASSWORD_QUERY}
RABBITMQ_HOST=${RABBITMQ_HOST}
RABBITMQ_PORT=${RABBITMQ_PORT}
RABBITMQ_USERNAME=${RABBITMQ_USER}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
```

## Passo 5: Configurar Dependências entre Serviços

O Railway automaticamente gerencia as dependências baseado nas variáveis de ambiente compartilhadas. Certifique-se de que:

1. Os serviços que dependem de bancos de dados referenciam as variáveis corretas
2. Todos os serviços que usam RabbitMQ referenciam as mesmas variáveis
3. As variáveis são consistentes entre os serviços

## Passo 6: Iniciar o Deploy

1. Após configurar todos os serviços, o Railway iniciará automaticamente o build
2. Monitore os logs de cada serviço no dashboard
3. Aguarde todos os serviços ficarem "Active" (verde)

## Passo 7: Verificar o Deploy

### URLs dos Serviços

O Railway fornecerá URLs públicas para cada serviço:
- Order Service: `https://order-service-xxx.railway.app`
- Payment Service: `https://payment-service-xxx.railway.app`
- Inventory Service: `https://inventory-service-xxx.railway.app`
- Order Query Service: `https://order-query-service-xxx.railway.app`

### Teste de Saúde

```bash
curl https://order-service-xxx.railway.app/api/orders/health
curl https://payment-service-xxx.railway.app/api/payments/health
curl https://inventory-service-xxx.railway.app/api/inventory/health
curl https://order-query-service-xxx.railway.app/api/orders/health
```

### Teste Funcional

```bash
# Criar um pedido
curl -X POST https://order-service-xxx.railway.app/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [
      {
        "productId": "product-1",
        "productName": "Laptop",
        "quantity": 1,
        "price": 999.99
      }
    ]
  }'

# Consultar pedidos
curl https://order-query-service-xxx.railway.app/api/orders
```

## Passo 8: Monitoramento e Logs

### Visualizar Logs

1. No dashboard, clique em cada serviço
2. Vá para a aba "Logs"
3. Monitore a inicialização e processamento de eventos

### Métricas

1. Use os endpoints de actuator para métricas:
   - `/actuator/health`
   - `/actuator/metrics`
   - `/actuator/info`

## Troubleshooting

### Problemas Comuns

1. **Serviço não inicia**:
   - Verifique os logs de build
   - Confirme se as variáveis de ambiente estão corretas
   - Certifique-se de que o Dockerfile está na raiz do serviço

2. **Conexão com banco falha**:
   - Verifique se as variáveis de ambiente do PostgreSQL estão corretas
   - Confirme se o serviço de banco está "Active"

3. **RabbitMQ não conecta**:
   - Verifique as variáveis de ambiente do RabbitMQ
   - Confirme se o serviço RabbitMQ está rodando

4. **Dependências não resolvidas**:
   - Certifique-se de que shared-events está sendo construído primeiro
   - Verifique se todas as dependências Maven estão corretas

### Comandos Úteis

```bash
# Verificar status dos serviços
railway status

# Ver logs de um serviço específico
railway logs --service order-service

# Reiniciar um serviço
railway restart --service order-service
```

## Limitações do Plano Gratuito

- **CPU**: 0.5 vCPU por serviço
- **RAM**: 512MB por serviço
- **Builds**: 500 horas por mês
- **Bandwidth**: 100GB por mês

Para projetos de produção, considere upgrade para plano pago.

## Configurações Avançadas

### Configurar Domínio Personalizado

1. No serviço, vá para "Settings"
2. Clique em "Domains"
3. Adicione seu domínio personalizado
4. Configure DNS conforme instruções

### Configurar Variáveis de Ambiente Específicas

```bash
# Exemplo de configuração específica para produção
SPRING_PROFILES_ACTIVE=production
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_ORDERSYSTEM=INFO
```

### Configurar Health Checks

Railway automaticamente usa os health checks do Spring Boot Actuator, mas você pode customizar:

```yaml
# railway.json
{
  "build": {
    "builder": "DOCKERFILE"
  },
  "deploy": {
    "healthcheckPath": "/actuator/health",
    "healthcheckTimeout": 300,
    "restartPolicyType": "ON_FAILURE"
  }
}
```

## Conclusão

O Railway.app facilita significativamente o deploy de microsserviços Spring Boot, fornecendo:
- Detecção automática de tecnologias
- Gerenciamento simplificado de bancos de dados
- Escalabilidade automática
- Monitoramento integrado
- Deploy contínuo a partir do GitHub

Para projetos de demonstração e prototipagem, é uma excelente opção gratuita que permite focar no desenvolvimento sem se preocupar com infraestrutura.