# 🐳 Deploy Docker no Render.com - Guia Definitivo

## 📋 Configuração Atual do Serviço

**Serviço**: Gestao_de_Pedidos  
**Tipo**: Web Service (Docker)  
**Plano**: Free  
**URL**: https://gestao-de-pedidos.onrender.com  
**Service ID**: srv-d2kbhnruibrs73emmc8g  

## ⚙️ Configurações Docker no Render

### 1. **Build & Deploy Settings**

```
Repository: Lucasantunesribeiro/Sistema_de_Gestao_de_Pedidos_Distribu-do_com_Event_Sourcing
Branch: main
Root Directory: unified-order-system
Dockerfile Path: ./Dockerfile
Docker Build Context Directory: ./
Docker Command: (vazio - usa ENTRYPOINT do Dockerfile)
Pre-Deploy Command: (vazio)
Auto-Deploy: On Commit
```

### 2. **Environment Variables Necessárias**

Configure estas variáveis no painel Environment do Render:

```bash
# Configuração do banco PostgreSQL (OBRIGATÓRIO)
DATABASE_URL=[URL completa do PostgreSQL do Render]

# Profile Spring (OBRIGATÓRIO) 
SPRING_PROFILES_ACTIVE=render

# Configurações de cache (OBRIGATÓRIO)
REDIS_ENABLED=false
CACHE_TYPE=simple

# Configurações opcionais de logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_ORDERSYSTEM=DEBUG
```

### 3. **Health Check Configuration**

```
Health Check Path: /actuator/health
```

## 🔧 Como o Docker Funciona

### 1. **Multi-Stage Build**
```dockerfile
# Stage 1: Build com Maven
FROM maven:3.9-eclipse-temurin-17 AS build
# Download dependencies e build da aplicação

# Stage 2: Runtime otimizado
FROM eclipse-temurin:17-jre-alpine
# Apenas JRE para menor tamanho da imagem
```

### 2. **Aplicação Unificada**
O sistema roda como uma única aplicação Spring Boot:

```bash
Port: 8080 (padrão, Render sobrescreve com PORT env var)
Profile: render (configurado via SPRING_PROFILES_ACTIVE)
Health Check: /actuator/health
```

### 3. **Configuração Automática**
- **PORT**: Render define automaticamente (ex: 10000)
- **Database**: Configuração automática via RenderDatabaseConfig
- **Cache**: In-memory quando Redis não disponível
- **Security**: Usuário não-root para segurança

## 🚀 Processo de Deploy

### 1. **Verificar Configurações**
```bash
# No Render Dashboard → Settings → Build & Deploy
Root Directory: unified-order-system
Dockerfile Path: ./Dockerfile
Docker Build Context Directory: ./
```

### 2. **Configurar Environment Variables**
```bash
# Mínimo necessário:
DATABASE_URL=postgresql://user:pass@host:port/db
SPRING_PROFILES_ACTIVE=render
REDIS_ENABLED=false
CACHE_TYPE=simple
```

### 3. **Fazer Deploy Manual**
1. Acesse o Dashboard do Render
2. Vá para o serviço "Gestao_de_Pedidos"
3. Clique em "Manual Deploy"
4. Selecione "Deploy latest commit"

### 4. **Monitorar Build**
O build Docker passa por estas etapas:
```
1. Clone do repositório
2. Build Maven (Java services)
3. Build Node.js (Frontend)
4. Criação da imagem final
5. Deploy e startup
```

## 🔍 Verificações Pós-Deploy

### 1. **Logs de Startup**
Procure por estas mensagens nos logs:
```
Configuring database connection to: [host-do-render]
Started Application in X.XXX seconds (JVM running for Y.YYY)
Tomcat started on port(s): 10000 (http)
```

### 2. **Health Check**
```bash
curl https://gestao-de-pedidos.onrender.com/actuator/health
```

**Resposta esperada:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "healthConfig": {"status": "UP"}
  }
}
```

### 3. **Teste de API**
```bash
# Listar pedidos
curl https://gestao-de-pedidos.onrender.com/api/orders

# Criar pedido
curl -X POST https://gestao-de-pedidos.onrender.com/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "test-001",
    "customerName": "João Silva",
    "items": [{
      "productId": "product-1",
      "productName": "Produto Teste",
      "quantity": 2,
      "price": 99.99
    }]
  }'
```

### 4. **Frontend**
```bash
# Acessar interface web
https://gestao-de-pedidos.onrender.com
```

## 🚨 Troubleshooting

### **Problema: Build falha no Maven**
```
Solução: Verificar se o Dockerfile está no caminho correto
Dockerfile Path: unified-order-system/
```

### **Problema: "Application failed to start"**
```
Erro: Failed to configure a DataSource
Solução: Verificar se DATABASE_URL está configurada corretamente
```

### **Problema: "Port already in use"**
```
Erro: Address already in use
Solução: O Render define PORT automaticamente, não definir manualmente
```

### **Problema: Database connection failed**
```
Erro: Connection refused
Solução: Verificar DATABASE_URL e se PostgreSQL está ativo
```

### **Problema: Health check failing**
```
Erro: Health check timeout
Solução: Verificar se /actuator/health responde na porta correta
```

## 📊 Arquitetura do Deploy

```
┌─────────────────────────────────────────┐
│              Render.com                 │
├─────────────────────────────────────────┤
│  🐳 Docker Container                    │
│  ┌─────────────────────────────────────┐│
│  │  📦 Unified Order System           ││
│  │  ├─ Spring Boot App (PORT=10000)   ││
│  │  ├─ REST APIs (/api/*)             ││
│  │  ├─ Actuator (/actuator/*)         ││
│  │  └─ Static Resources               ││
│  └─────────────────────────────────────┘│
├─────────────────────────────────────────┤
│  🗄️ PostgreSQL Database                │
│  └─ Managed by Render                  │
└─────────────────────────────────────────┘
```

## 🎯 Configuração Completa

### **Environment Variables (Render Dashboard)**
```bash
DATABASE_URL=postgresql://order_system_postgres_user:RFIkVFFageJjBC0i7yUZO6IGiepHl42D@dpg-d2nr367fte5s7381n0n0-a/order_system_postgres
SPRING_PROFILES_ACTIVE=render
REDIS_ENABLED=false
CACHE_TYPE=simple
LOGGING_LEVEL_ROOT=INFO
```

### **Build Settings (Render Dashboard)**
```bash
Repository: Lucasantunesribeiro/Sistema_de_Gestao_de_Pedidos_Distribu-do_com_Event_Sourcing
Branch: main
Root Directory: unified-order-system
Dockerfile Path: ./Dockerfile
Docker Build Context Directory: ./
Health Check Path: /actuator/health
```

## ✅ Checklist Final

- [ ] **Repository**: Configurado corretamente
- [ ] **Root Directory**: `unified-order-system`
- [ ] **Dockerfile Path**: `./Dockerfile`
- [ ] **Environment Variables**: Todas configuradas
- [ ] **DATABASE_URL**: URL completa do PostgreSQL
- [ ] **Health Check**: `/actuator/health`
- [ ] **Auto-Deploy**: Habilitado

## 🎉 Resultado Esperado

Após o deploy bem-sucedido:

1. **API funcionando**: `https://gestao-de-pedidos.onrender.com/api/orders`
2. **Frontend funcionando**: `https://gestao-de-pedidos.onrender.com`
3. **Health check OK**: `https://gestao-de-pedidos.onrender.com/actuator/health`
4. **Database conectado**: PostgreSQL do Render funcionando
5. **Logs limpos**: Sem erros de startup

---

**🚀 Sistema pronto para produção no Render.com com Docker!**