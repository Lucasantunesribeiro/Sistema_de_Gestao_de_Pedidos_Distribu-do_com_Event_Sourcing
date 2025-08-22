# Sistema de Gestão de Pedidos Distribuído

Um sistema completo de gestão de pedidos implementado com arquitetura de microsserviços, demonstrando padrões avançados como Event Sourcing, CQRS e comunicação orientada a eventos.

## 🚀 Características

- **Arquitetura de Microsserviços**: 4 serviços independentes com responsabilidades bem definidas
- **Event Sourcing**: Armazenamento completo do histórico de eventos
- **CQRS**: Separação entre comandos e consultas para otimização
- **Frontend Moderno**: Interface React 18 + TypeScript + shadcn/ui
- **Comunicação Assíncrona**: RabbitMQ para mensageria entre serviços
- **Resiliência**: Circuit breakers, retry e timeout patterns
- **Observabilidade**: Health checks, métricas e logs estruturados

## 🏗️ Arquitetura

### Serviços Backend

- **Order Service** (8081): Gerenciamento de pedidos com Event Sourcing
- **Payment Service** (8082): Processamento de pagamentos
- **Inventory Service** (8083): Controle de estoque
- **Order Query Service** (8084): Consultas otimizadas e dashboard

### Frontend

- **React Application** (3000): Interface de usuário moderna e responsiva
- **shadcn/ui**: Componentes de UI consistentes e acessíveis
- **TanStack Query**: Gerenciamento de estado do servidor
- **React Router**: Navegação client-side

### Infraestrutura

- **PostgreSQL**: Bancos de dados para event store e read models
- **RabbitMQ**: Message broker para comunicação entre serviços
- **Redis**: Cache para otimização de consultas
- **Nginx**: Reverse proxy e load balancer

## 🛠️ Tecnologias

### Backend
- Java 17
- Spring Boot 3.1.5
- Spring Data JPA
- Spring AMQP
- PostgreSQL 15
- RabbitMQ
- Redis
- Resilience4j
- Docker

### Frontend
- React 18
- TypeScript
- Vite
- shadcn/ui
- TanStack Query
- React Router
- Tailwind CSS
- Lucide Icons

## 🚀 Como Executar

### Pré-requisitos

- Docker e Docker Compose
- Java 17 (para desenvolvimento local)
- Node.js 18+ (para desenvolvimento do frontend)
- Maven 3.6+

### Execução Completa com Docker

1. **Clone o repositório**
   ```bash
   git clone <repository-url>
   cd order-management-system
   ```

2. **Build e execute todos os serviços**
   ```bash
   # Build da biblioteca compartilhada
   cd shared-events
   mvn clean install
   cd ..

   # Iniciar todo o sistema
   docker-compose up --build
   ```

3. **Acesse as aplicações**
   - **Frontend**: http://localhost:3000
   - **API Gateway**: http://localhost:8080
   - **RabbitMQ Management**: http://localhost:15672 (guest/guest)

### Desenvolvimento Local

#### Backend

1. **Iniciar infraestrutura**
   ```bash
   docker-compose up order-db query-db rabbitmq redis
   ```

2. **Build da biblioteca compartilhada**
   ```bash
   cd shared-events
   mvn clean install
   cd ..
   ```

3. **Executar serviços individualmente**
   ```bash
   # Order Service
   cd services/order-service
   mvn spring-boot:run

   # Payment Service
   cd services/payment-service
   mvn spring-boot:run

   # Inventory Service
   cd services/inventory-service
   mvn spring-boot:run

   # Query Service
   cd services/order-query-service
   mvn spring-boot:run
   ```

#### Frontend

1. **Instalar dependências**
   ```bash
   cd frontend
   npm install
   ```

2. **Executar em modo desenvolvimento**
   ```bash
   npm run dev
   ```

## 📊 Endpoints da API

### Order Service (8081)
- `GET /api/orders` - Listar pedidos
- `POST /api/orders` - Criar pedido
- `GET /api/orders/{id}` - Obter pedido específico
- `DELETE /api/orders/{id}` - Cancelar pedido
- `POST /api/orders/{id}/reserve-inventory` - Reservar estoque

### Payment Service (8082)
- `GET /api/payments` - Listar pagamentos
- `GET /api/payments/{id}` - Obter pagamento específico
- `POST /api/payments/{id}/retry` - Retentar pagamento

### Inventory Service (8083)
- `GET /api/inventory` - Listar itens do estoque

### Query Service (8084)
- `GET /api/dashboard/metrics` - Métricas do dashboard
- `GET /api/orders` - Consultas otimizadas de pedidos

## 🔧 Configuração

### Variáveis de Ambiente

#### Serviços Backend
```env
DATABASE_URL=jdbc:postgresql://localhost:5432/order_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=password
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
REDIS_HOST=localhost
REDIS_PORT=6379
```

#### Frontend
```env
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```

## 🧪 Testes

### Backend
```bash
# Executar todos os testes
mvn clean test

# Testes de um serviço específico
cd services/order-service
mvn test
```

### Frontend
```bash
cd frontend
npm run test
```

## 📦 Deploy

### Railway.app

1. **Conecte o repositório ao Railway**
2. **Configure as variáveis de ambiente**
3. **Deploy automático será acionado**

### Docker Production

```bash
# Build para produção
docker-compose -f docker-compose.prod.yml up --build
```

## 🔍 Monitoramento

### Health Checks
- Order Service: http://localhost:8081/api/orders/health
- Payment Service: http://localhost:8082/api/payments/health
- Inventory Service: http://localhost:8083/api/inventory/health
- Query Service: http://localhost:8084/api/orders/health

### Logs
```bash
# Ver logs de todos os serviços
docker-compose logs -f

# Logs de um serviço específico
docker-compose logs -f order-service
```

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📝 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 🆘 Suporte

Para suporte e dúvidas:
- Abra uma issue no GitHub
- Consulte a documentação técnica em `/docs`
- Verifique os logs dos serviços para troubleshooting