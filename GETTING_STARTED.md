# Guia de Início Rápido

Este guia irá ajudá-lo a configurar e executar o Sistema de Gestão de Pedidos Distribuído em sua máquina local.

## 🚀 Início Rápido (Recomendado)

### Opção 1: Docker-Only (Sem Maven) - RECOMENDADO

```powershell
# Windows - Não precisa do Maven instalado
.\scripts\setup-docker-only.ps1
```

### Opção 2: Com Maven (Windows)

```powershell
# Se você tem Maven instalado
.\scripts\setup.ps1

# Se não tem Maven, veja: INSTALL_MAVEN.md
```

### Opção 3: Linux/Mac

```bash
# Torne o script executável
chmod +x scripts/setup.sh

# Execute o script de setup
./scripts/setup.sh
```

### Opção 4: Execução Manual

1. **Construa a biblioteca compartilhada**
   ```bash
   cd shared-events
   mvn clean install
   cd ..
   ```

2. **Instale dependências do frontend**
   ```bash
   cd frontend
   npm install
   cd ..
   ```

3. **Inicie todos os serviços**
   ```bash
   docker-compose up --build
   ```

## 📱 Acessando a Aplicação

Após a inicialização completa (aguarde cerca de 2-3 minutos), acesse:

- **🌐 Frontend (Interface Principal)**: http://localhost:3000
- **🔌 API Gateway**: http://localhost:8080
- **🐰 RabbitMQ Management**: http://localhost:15672 (usuário: `guest`, senha: `guest`)

## 🎯 Funcionalidades Principais

### Dashboard
- Visão geral do sistema
- Métricas em tempo real
- Status dos serviços
- Gráficos de performance

### Gestão de Pedidos
- ✅ Criar novos pedidos
- 👁️ Visualizar detalhes dos pedidos
- ❌ Cancelar pedidos pendentes
- 📊 Filtrar e buscar pedidos

### Pagamentos
- 💳 Visualizar status dos pagamentos
- 🔄 Retentar pagamentos falhados
- 📈 Métricas de taxa de sucesso
- 🔍 Filtros avançados

### Estoque
- 📦 Visualizar itens em estoque
- ⚠️ Alertas de estoque baixo
- 📊 Status de disponibilidade
- 🔍 Busca por produtos

## 🧪 Testando o Sistema

### 1. Criar um Pedido

1. Acesse http://localhost:3000/orders
2. Clique em "Novo Pedido"
3. Preencha os dados:
   - **Cliente**: `customer-123`
   - **Produto**: `product-001`
   - **Nome**: `Produto Teste`
   - **Quantidade**: `2`
   - **Preço**: `50.00`
4. Clique em "Criar Pedido"

### 2. Acompanhar o Processamento

1. Observe o pedido sendo processado no dashboard
2. Verifique o status na página de pedidos
3. Monitore o pagamento na página de pagamentos
4. Veja a reserva de estoque na página de inventário

### 3. Verificar Logs

```bash
# Ver logs de todos os serviços
docker-compose logs -f

# Ver logs de um serviço específico
docker-compose logs -f order-service
```

## 🔧 Desenvolvimento Local

### Backend

Para desenvolvimento dos serviços Java:

1. **Inicie apenas a infraestrutura**
   ```bash
   docker-compose up order-db query-db rabbitmq redis
   ```

2. **Execute um serviço localmente**
   ```bash
   cd services/order-service
   mvn spring-boot:run
   ```

### Frontend

Para desenvolvimento do frontend React:

1. **Inicie o servidor de desenvolvimento**
   ```bash
   cd frontend
   npm run dev
   ```

2. **Acesse**: http://localhost:5173

## 🐛 Solução de Problemas

### Problema: Serviços não inicializam

**Solução:**
```bash
# Pare todos os containers
docker-compose down

# Remova volumes (cuidado: apaga dados)
docker-compose down -v

# Reconstrua e inicie
docker-compose up --build
```

### Problema: Porta já em uso

**Solução:**
```bash
# Verifique processos usando as portas
netstat -tulpn | grep :3000
netstat -tulpn | grep :8080

# Mate o processo se necessário
kill -9 <PID>
```

### Problema: Erro de memória no Docker

**Solução:**
- Aumente a memória disponível para o Docker Desktop
- Ou reduza o número de serviços executando simultaneamente

### Problema: Frontend não conecta com backend

**Verificações:**
1. Confirme que o nginx-proxy está rodando na porta 8080
2. Verifique se todos os serviços backend estão saudáveis
3. Teste diretamente: http://localhost:8080/health

## 📊 Monitoramento

### Health Checks

Verifique a saúde dos serviços:

```bash
# Order Service
curl http://localhost:8081/actuator/health

# Payment Service  
curl http://localhost:8082/actuator/health

# Inventory Service
curl http://localhost:8083/actuator/health

# Query Service
curl http://localhost:8084/actuator/health
```

### Métricas

Acesse métricas detalhadas:
- http://localhost:8081/actuator/metrics
- http://localhost:8082/actuator/metrics
- http://localhost:8083/actuator/metrics
- http://localhost:8084/actuator/metrics

## 🚀 Deploy em Produção

### Railway.app

1. Conecte seu repositório ao Railway
2. Configure as variáveis de ambiente necessárias
3. O deploy será automático a cada push

### Docker Swarm

```bash
# Inicialize o swarm
docker swarm init

# Deploy da stack
docker stack deploy -c docker-compose.yml order-system
```

## 📚 Próximos Passos

1. **Explore a API**: Use o Postman ou curl para testar os endpoints
2. **Customize o Frontend**: Modifique os componentes React conforme necessário
3. **Adicione Funcionalidades**: Implemente novos recursos nos serviços
4. **Configure Monitoramento**: Adicione Prometheus/Grafana para métricas avançadas
5. **Implemente Testes**: Adicione testes automatizados para garantir qualidade

## 🆘 Suporte

Se encontrar problemas:

1. **Verifique os logs**: `docker-compose logs -f`
2. **Consulte a documentação**: Leia o README.md completo
3. **Abra uma issue**: No repositório GitHub
4. **Verifique o status**: `docker-compose ps`

---

**Dica**: Mantenha o Docker Desktop atualizado e com pelo menos 4GB de RAM disponível para melhor performance.