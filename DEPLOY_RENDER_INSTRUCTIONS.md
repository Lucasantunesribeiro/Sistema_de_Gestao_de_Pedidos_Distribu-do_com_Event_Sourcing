# 🚀 Deploy no Render - Instruções Detalhadas

## ✅ Pré-requisitos Concluídos
- ✅ Código commitado e enviado para GitHub
- ✅ Dockerfile configurado
- ✅ Sistema compilando sem erros
- ✅ Todos os testes passando

## 📋 Passo a Passo para Deploy

### 1. Acessar o Render
1. Acesse: https://render.com
2. Faça login ou crie uma conta
3. Acesse o Dashboard

### 2. Criar Banco de Dados PostgreSQL

1. No Dashboard, clique em **"New +"**
2. Selecione **"PostgreSQL"**
3. Configure:
   - **Name**: `unified-order-system-db`
   - **Database**: `unified_orders`
   - **User**: `admin`
   - **Region**: `Oregon (US West)`
   - **PostgreSQL Version**: `15`
   - **Plan**: `Free` (para teste) ou `Starter $7/mês` (para produção)

4. Clique em **"Create Database"**
5. **IMPORTANTE**: Anote as informações de conexão:
   - **Internal Database URL** (para usar no serviço)
   - **External Database URL** (para conexões externas)

### 3. Criar Web Service

1. No Dashboard, clique em **"New +"**
2. Selecione **"Web Service"**
3. Conecte ao repositório GitHub:
   - Autorize o Render a acessar seu GitHub
   - Selecione o repositório: `Sistema_de_Gestao_de_Pedidos_Distribu-do_com_Event_Sourcing`

4. Configure o serviço:

#### Configurações Básicas:
- **Name**: `unified-order-system`
- **Region**: `Oregon (US West)`
- **Branch**: `main`
- **Root Directory**: (deixe vazio)
- **Runtime**: `Docker`

#### Build & Deploy Settings:
- **Build Command**: (deixe vazio - usa Dockerfile)
- **Start Command**: (deixe vazio - usa Dockerfile CMD)

#### Plan:
- **Starter**: $7/mês (512 MB RAM, 0.5 CPU)
- **Standard**: $25/mês (2 GB RAM, 1 CPU) - Recomendado para produção

### 4. Configurar Variáveis de Ambiente

Na seção **Environment**, adicione as seguintes variáveis:

```env
# Tipo de serviço
SERVICE_TYPE=web

# Perfil Spring
SPRING_PROFILES_ACTIVE=render

# Configurações JVM
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport

# Banco de dados (substitua pela URL do seu banco)
DATABASE_URL=postgresql://admin:SENHA@dpg-XXXXX-a.oregon-postgres.render.com/unified_orders
SPRING_DATASOURCE_URL=${DATABASE_URL}
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=SUA_SENHA_DO_BANCO

# Configurações do banco
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect

# Porta do servidor (Render define automaticamente)
SERVER_PORT=${PORT}

# Configurações de logging
LOGGING_LEVEL_COM_ORDERSYSTEM=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_BOOT=INFO
```

### 5. Configurações Avançadas

#### Auto-Deploy:
- ✅ Ative **"Auto-Deploy"** para deploy automático a cada push

#### Health Check:
- **Health Check Path**: `/health`
- **Health Check Timeout**: `30 seconds`

#### Headers:
- O nginx já está configurado com headers de segurança

### 6. Iniciar Deploy

1. Clique em **"Create Web Service"**
2. O Render iniciará o processo de build e deploy
3. Acompanhe os logs em tempo real

### 7. Monitorar o Deploy

#### Logs para Acompanhar:
```
=== Build Phase ===
Building image from Dockerfile...
Step 1/XX : FROM maven:3.9.8-eclipse-temurin-17 AS java-builder
...

=== Deploy Phase ===
Starting unified web service (nginx + unified-order-system)
Processing nginx template with PORT=10000
Testing nginx configuration
Starting supervisord...
```

#### Tempo Estimado:
- **Build**: 5-8 minutos
- **Deploy**: 2-3 minutos
- **Total**: 7-11 minutos

### 8. Verificar Deploy

Após o deploy bem-sucedido, você receberá uma URL como:
`https://unified-order-system.onrender.com`

#### Endpoints para Testar:

1. **Health Check**:
   ```
   GET https://unified-order-system.onrender.com/health
   ```
   Resposta esperada:
   ```json
   {
     "status": "UP",
     "components": {
       "db": {"status": "UP"},
       "diskSpace": {"status": "UP"}
     }
   }
   ```

2. **Dashboard**:
   ```
   https://unified-order-system.onrender.com/dashboard
   ```

3. **API de Pedidos**:
   ```
   GET https://unified-order-system.onrender.com/api/orders
   ```

4. **Interface Principal**:
   ```
   https://unified-order-system.onrender.com/
   ```

### 9. Configurações Pós-Deploy

#### Domínio Customizado (Opcional):
1. No painel do serviço, vá para **"Settings"**
2. Na seção **"Custom Domains"**, adicione seu domínio
3. Configure os DNS records conforme instruído

#### Monitoramento:
- **Logs**: Disponíveis no painel do Render
- **Métricas**: CPU, RAM, Network no dashboard
- **Alertas**: Configure notificações por email

#### Backup:
- O PostgreSQL no Render faz backup automático
- Configure backup adicional se necessário

### 10. Troubleshooting

#### Se o Deploy Falhar:

1. **Erro de Build**:
   - Verifique os logs de build
   - Confirme que o Dockerfile está correto
   - Teste o build localmente: `docker build .`

2. **Erro de Conexão com Banco**:
   - Verifique as variáveis de ambiente
   - Confirme que o banco está rodando
   - Teste a conexão com a URL externa

3. **Erro 502/503**:
   - Aguarde alguns minutos (serviço pode estar inicializando)
   - Verifique logs do aplicativo
   - Confirme que a porta está correta

4. **Health Check Falhando**:
   - Verifique se `/health` está respondendo
   - Ajuste o timeout se necessário
   - Confirme que o Spring Boot está iniciando

### 11. Comandos Úteis

#### Ver Logs:
```bash
# Via Render CLI (se instalado)
render logs unified-order-system

# Ou acesse via web no painel do serviço
```

#### Restart do Serviço:
- No painel do serviço, clique em **"Manual Deploy"**
- Ou faça um novo commit no repositório

#### Rollback:
- No painel, vá para **"Deploys"**
- Selecione um deploy anterior e clique em **"Rollback"**

## 🎉 Deploy Concluído!

Após seguir estes passos, seu sistema estará rodando em produção no Render com:

- ✅ **HTTPS automático** com certificado SSL
- ✅ **Auto-scaling** baseado na demanda
- ✅ **Monitoramento** integrado
- ✅ **Backup automático** do banco de dados
- ✅ **Deploy automático** a cada push
- ✅ **Load balancing** global
- ✅ **CDN** para assets estáticos

**URL do Sistema**: https://unified-order-system.onrender.com

O sistema está pronto para receber usuários reais! 🚀