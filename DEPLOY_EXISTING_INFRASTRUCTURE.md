# Deploy com Infraestrutura Existente - Render.com

## 🎯 Situação Atual

Você já possui os seguintes serviços criados no Render.com:
- **Web Service**: `Gestao_de_Pedidos` (srv-d2kbhnruibrs73emmc8g)
- **PostgreSQL**: `order-system-postgres` (dpg-d2nr367fte5s7381n0n0-a)
- **Redis**: `order-system-redis` (red-d2nr3795pdvs7394onhg)

## 🔄 Estratégia de Migração

Vamos **reutilizar a infraestrutura existente** e apenas atualizar o Web Service para usar o novo sistema unificado.

## 📋 Passo a Passo

### 1. 🔧 Atualizar Web Service Existente

Acesse o Web Service `Gestao_de_Pedidos` no Render Dashboard e faça as seguintes alterações:

#### 1.1 Build & Deploy Settings
```
Root Directory: unified-order-system
Build Command: ./mvnw clean package -DskipTests
Start Command: java -Dspring.profiles.active=prod -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -jar target/unified-order-system-1.0.0.jar
```

#### 1.2 Environment Variables
Adicione/atualize as seguintes variáveis de ambiente:

**Configurações Básicas:**
```
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
JPA_DDL_AUTO=update
JPA_SHOW_SQL=false
```

**Database (conectar ao PostgreSQL existente):**
```
DATABASE_URL=[usar conexão do order-system-postgres]
DATABASE_USERNAME=[usar usuário do order-system-postgres]  
DATABASE_PASSWORD=[usar senha do order-system-postgres]
```

**Redis (conectar ao Redis existente):**
```
REDIS_HOST=[usar host do order-system-redis]
REDIS_PORT=[usar porta do order-system-redis]
REDIS_PASSWORD=[usar senha do order-system-redis]
```

#### 1.3 Health Check
```
Health Check Path: /actuator/health
```

### 2. 📦 Preparar o Código para Deploy

Execute o script de preparação:

```bash
# Tornar o script executável
chmod +x deploy-unified.sh

# Executar preparação
./deploy-unified.sh
```

### 3. 🚀 Fazer o Deploy

#### 3.1 Commit e Push
```bash
git add .
git commit -m "feat: deploy sistema unificado para infraestrutura existente"
git push origin main
```

#### 3.2 Trigger Deploy Manual
No Render Dashboard:
1. Vá para o Web Service `Gestao_de_Pedidos`
2. Clique em "Manual Deploy"
3. Selecione a branch `main`
4. Clique em "Deploy"

### 4. 🔍 Monitorar o Deploy

#### 4.1 Acompanhar Logs
- Acesse a aba "Logs" do Web Service
- Monitore o processo de build e inicialização
- Procure por mensagens de sucesso:
  ```
  Started UnifiedOrderSystemApplication
  Tomcat started on port(s): 8080
  ```

#### 4.2 Verificar Health Check
Após o deploy, teste:
```bash
curl https://gestao-de-pedidos.onrender.com/actuator/health
```

Resposta esperada:
```json
{
  "status": "UP",
  "components": {
    "database": {"status": "UP"},
    "redis": {"status": "UP"},
    "healthConfig": {
      "status": "UP",
      "details": {
        "service": "unified-order-system",
        "status": "operational"
      }
    }
  }
}
```

## 🧪 Testes Pós-Deploy

### 1. Testar API de Pedidos
```bash
# Criar pedido
curl -X POST https://gestao-de-pedidos.onrender.com/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "test-customer-001",
    "items": [{
      "productId": "product-1",
      "quantity": 2,
      "price": 99.99
    }]
  }'
```

### 2. Testar API de Query
```bash
# Buscar pedidos
curl https://gestao-de-pedidos.onrender.com/api/query/orders

# Verificar estoque
curl https://gestao-de-pedidos.onrender.com/api/query/inventory/product-1
```

### 3. Testar Documentação
Acesse: https://gestao-de-pedidos.onrender.com/swagger-ui.html

## 🔧 Configurações Específicas do Render

### Environment Variables Detalhadas

Com base na sua infraestrutura existente, configure:

```bash
# Aplicação
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# Database (PostgreSQL existente)
DATABASE_URL=postgresql://[host]:[port]/[database]
DATABASE_USERNAME=[username]
DATABASE_PASSWORD=[password]
JPA_DDL_AUTO=update
JPA_SHOW_SQL=false
JPA_DIALECT=org.hibernate.dialect.PostgreSQLDialect

# Redis (Redis existente)  
REDIS_HOST=[redis-host]
REDIS_PORT=6379
REDIS_PASSWORD=[redis-password]

# Cache
SPRING_CACHE_TYPE=redis
SPRING_CACHE_REDIS_TIME_TO_LIVE=600000

# Logging
LOGGING_LEVEL_COM_ORDERSYSTEM=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CACHE=WARN

# Performance
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5
```

## 🚨 Troubleshooting

### Problema: Build Falha
**Solução:**
1. Verificar se `Root Directory` está definido como `unified-order-system`
2. Verificar se o Maven wrapper existe: `unified-order-system/mvnw`
3. Verificar logs de build para erros específicos

### Problema: Aplicação não Inicia
**Solução:**
1. Verificar variáveis de ambiente do database
2. Verificar conectividade com PostgreSQL e Redis
3. Verificar logs da aplicação para stack traces

### Problema: Health Check Falha
**Solução:**
1. Verificar se aplicação está rodando na porta 8080
2. Verificar se endpoint `/actuator/health` está acessível
3. Aumentar timeout do health check se necessário

### Problema: Conexão Database
**Solução:**
1. Verificar se PostgreSQL está ativo
2. Verificar credenciais de conexão
3. Verificar se aplicação está na mesma região

## 📊 Monitoramento Contínuo

### Métricas Importantes
- **CPU Usage**: Deve ficar abaixo de 80%
- **Memory Usage**: Deve ficar abaixo de 400MB
- **Response Time**: APIs devem responder em < 500ms
- **Error Rate**: Deve ser < 1%

### Logs Estruturados
Os logs agora estão em formato JSON para melhor análise:
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "logger": "com.ordersystem.unified.order.OrderService",
  "message": "Order created successfully",
  "correlationId": "abc-123-def"
}
```

## 🎯 Benefícios da Migração

### ✅ Mantém Infraestrutura
- Reutiliza PostgreSQL e Redis existentes
- Mantém URLs e configurações atuais
- Zero downtime na migração

### ⚡ Melhora Performance
- Eliminação de chamadas HTTP entre serviços
- Cache Redis otimizado
- Transações ACID reais

### 💰 Reduz Custos
- De 4 Web Services para 1
- Mantém apenas 1 instância ativa
- Economia de ~75% nos custos

## 🎉 Próximos Passos

Após deploy bem-sucedido:

1. **Monitorar por 24h**: Verificar estabilidade
2. **Testar Funcionalidades**: Validar todos os endpoints
3. **Configurar Alertas**: Para falhas e performance
4. **Documentar APIs**: Atualizar documentação
5. **Otimizar Performance**: Baseado em métricas reais

---

**🚀 Com essa abordagem, você mantém toda a infraestrutura existente e apenas migra para o sistema unificado mais eficiente!**