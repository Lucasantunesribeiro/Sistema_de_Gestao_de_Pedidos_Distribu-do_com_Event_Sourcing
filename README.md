# Sistema de Gestão de Pedidos Distribuído com Event Sourcing

Este projeto implementa um sistema de gestão de pedidos distribuído usando Event Sourcing, CQRS (Command Query Responsibility Segregation), e arquitetura orientada a eventos (EDA) com Java 17, Spring Boot 3.x, RabbitMQ, e PostgreSQL.

## Arquitetura

### Microsserviços

1. **Order Service** (Porta 8081)
   - Gerencia criação e atualização de pedidos
   - Implementa Event Sourcing com Event Store
   - Persiste eventos em PostgreSQL
   - Publica eventos para RabbitMQ

2. **Payment Service** (Porta 8082)
   - Processa pagamentos de pedidos
   - Consome eventos de pedidos criados
   - Simula aprovação/recusa de pagamentos
   - Publica eventos de pagamento processado

3. **Inventory Service** (Porta 8083)
   - Gerencia estoque de produtos
   - Reserva/confirma itens do estoque
   - Consome eventos de pedidos e pagamentos
   - Mantém estoque em memória (para demonstração)

4. **Order Query Service** (Porta 8084)
   - Implementa CQRS Read Model
   - Mantém visão denormalizada de pedidos
   - Consome todos os eventos do sistema
   - Fornece APIs de consulta otimizadas

### Tecnologias Utilizadas

- **Java 17** - Linguagem de programação
- **Spring Boot 3.1.5** - Framework principal
- **PostgreSQL** - Banco de dados (Event Store e Read Model)
- **RabbitMQ** - Message broker para comunicação assíncrona
- **Docker** - Containerização
- **Maven** - Gerenciamento de dependências
- **Jackson** - Serialização JSON

### Padrões Implementados

- **Event Sourcing** - Armazenamento de eventos como fonte da verdade
- **CQRS** - Separação entre comandos e consultas
- **Saga Pattern** - Coordenação de transações distribuídas
- **Fanout Exchange** - Distribuição de eventos para múltiplos consumidores

## Configuração e Execução

### Pré-requisitos

- Java 17+
- Maven 3.6+
- Docker e Docker Compose

### Execução Local

1. Clone o repositório:
```bash
git clone <repository-url>
cd Sistema_de_Gestão_de_Pedidos_Distribuído_com_Event_Sourcing
```

2. Construa os componentes compartilhados:
```bash
cd shared-events
mvn clean install
cd ..
```

3. Execute o sistema com Docker Compose:
```bash
docker-compose up --build
```

4. Aguarde todos os serviços ficarem saudáveis (pode levar alguns minutos)

### Verificação de Saúde

Após inicializar, verifique se todos os serviços estão funcionando:

```bash
curl http://localhost:8081/api/orders/health    # Order Service
curl http://localhost:8082/api/payments/health  # Payment Service
curl http://localhost:8083/api/inventory/health # Inventory Service
curl http://localhost:8084/api/orders/health    # Order Query Service
```

### Interfaces de Administração

- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **PostgreSQL Order DB**: localhost:5432 (postgres/password)
- **PostgreSQL Query DB**: localhost:5433 (postgres/password)

## Uso da API

### Criar um Pedido

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [
      {
        "productId": "product-1",
        "productName": "Laptop",
        "quantity": 1,
        "price": 999.99
      },
      {
        "productId": "product-2",
        "productName": "Mouse",
        "quantity": 2,
        "price": 29.99
      }
    ]
  }'
```

### Consultar Pedidos

```bash
# Listar todos os pedidos
curl http://localhost:8084/api/orders

# Buscar pedido específico
curl http://localhost:8084/api/orders/{orderId}

# Buscar pedidos por cliente
curl http://localhost:8084/api/orders/customer/{customerId}

# Buscar pedidos por status
curl http://localhost:8084/api/orders/status/PENDING
```

### Verificar Estoque

```bash
# Ver estoque de um produto
curl http://localhost:8083/api/inventory/product-1

# Ver todo o estoque
curl http://localhost:8083/api/inventory/all
```

## Fluxo de Eventos

1. **Criação de Pedido**:
   - Cliente envia requisição para Order Service
   - Order Service persiste `OrderCreatedEvent` no Event Store
   - Evento é publicado para RabbitMQ FanoutExchange

2. **Processamento de Pagamento**:
   - Payment Service consome `OrderCreatedEvent`
   - Simula processamento de pagamento
   - Publica `PaymentProcessedEvent`

3. **Gestão de Estoque**:
   - Inventory Service consome `OrderCreatedEvent` e reserva estoque
   - Consome `PaymentProcessedEvent` e confirma/libera reserva

4. **Atualização de Read Model**:
   - Order Query Service consome todos os eventos
   - Atualiza tabela denormalizada para consultas otimizadas

## Deploy no Railway.app

### Pré-requisitos

1. Conta no Railway.app
2. Repositório no GitHub
3. Configuração das variáveis de ambiente

### Passos para Deploy

1. **Crie um novo projeto no Railway**:
   - Acesse railway.app
   - Clique em "New Project"
   - Selecione "Deploy from GitHub repo"
   - Escolha este repositório

2. **Configure os bancos de dados**:
   - Adicione PostgreSQL (para Order Service)
   - Adicione outro PostgreSQL (para Order Query Service)
   - Adicione RabbitMQ

3. **Configure os serviços**:
   Para cada microsserviço, adicione um novo serviço:
   - Order Service: `services/order-service`
   - Payment Service: `services/payment-service`
   - Inventory Service: `services/inventory-service`
   - Order Query Service: `services/order-query-service`

4. **Configure as variáveis de ambiente**:

   **Order Service**:
   ```
   DATABASE_URL=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
   DATABASE_USERNAME=${PGUSER}
   DATABASE_PASSWORD=${PGPASSWORD}
   RABBITMQ_HOST=${RABBITMQ_HOST}
   RABBITMQ_PORT=${RABBITMQ_PORT}
   RABBITMQ_USERNAME=${RABBITMQ_USER}
   RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
   ```

   **Payment Service**:
   ```
   RABBITMQ_HOST=${RABBITMQ_HOST}
   RABBITMQ_PORT=${RABBITMQ_PORT}
   RABBITMQ_USERNAME=${RABBITMQ_USER}
   RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
   ```

   **Inventory Service**:
   ```
   RABBITMQ_HOST=${RABBITMQ_HOST}
   RABBITMQ_PORT=${RABBITMQ_PORT}
   RABBITMQ_USERNAME=${RABBITMQ_USER}
   RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
   ```

   **Order Query Service**:
   ```
   DATABASE_URL=jdbc:postgresql://${PGHOST_QUERY}:${PGPORT_QUERY}/${PGDATABASE_QUERY}
   DATABASE_USERNAME=${PGUSER_QUERY}
   DATABASE_PASSWORD=${PGPASSWORD_QUERY}
   RABBITMQ_HOST=${RABBITMQ_HOST}
   RABBITMQ_PORT=${RABBITMQ_PORT}
   RABBITMQ_USERNAME=${RABBITMQ_USER}
   RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
   ```

5. **Deploy**:
   - O Railway iniciará automaticamente o build e deploy
   - Monitore os logs para verificar a inicialização
   - Teste os endpoints públicos fornecidos

### Configuração de Domínio

O Railway fornecerá URLs públicas para cada serviço:
- `https://order-service-xxx.railway.app`
- `https://payment-service-xxx.railway.app`
- `https://inventory-service-xxx.railway.app`
- `https://order-query-service-xxx.railway.app`

## Testes

### Executar Testes Locais

```bash
# Testes unitários para cada serviço
cd services/order-service && mvn test
cd services/payment-service && mvn test
cd services/inventory-service && mvn test
cd services/order-query-service && mvn test
```

### CI/CD

O projeto inclui configuração do GitHub Actions que:
- Executa testes automatizados
- Constrói os artefatos
- Cria imagens Docker
- Publica no Docker Hub (configuração opcional)

## Monitoramento

### Métricas Disponíveis

Cada serviço expõe métricas via Spring Boot Actuator:
- `/actuator/health` - Status de saúde
- `/actuator/metrics` - Métricas da aplicação
- `/actuator/info` - Informações da aplicação

### Logs

Os logs são estruturados e incluem:
- Eventos processados
- Erros de processamento
- Métricas de performance
- Rastreamento de transações

## Considerações de Produção

### Melhorias Recomendadas

1. **Segurança**:
   - Autenticação e autorização
   - Criptografia de dados sensíveis
   - Rate limiting

2. **Observabilidade**:
   - Distributed tracing (Jaeger/Zipkin)
   - Métricas avançadas (Prometheus)
   - Alertas automatizados

3. **Resiliência**:
   - Circuit breakers
   - Retry policies
   - Bulkhead pattern

4. **Escalabilidade**:
   - Particionamento de dados
   - Load balancing
   - Caching strategies

### Limitações Atuais

- Estoque em memória (usar banco de dados em produção)
- Processamento de pagamento simulado
- Sem autenticação/autorização
- Sem persistência de estado de saga

## Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature
3. Commit suas mudanças
4. Push para a branch
5. Crie um Pull Request

## Licença

Este projeto está sob a licença MIT. Veja o arquivo LICENSE para mais detalhes.