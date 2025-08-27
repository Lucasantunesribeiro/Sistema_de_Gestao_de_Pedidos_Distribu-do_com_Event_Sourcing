# Configuração de Secrets para Deploy Multi-Serviços no Render

## ⚠️ ATENÇÃO: Configuração Obrigatória

Para que o CI/CD funcione corretamente com a nova arquitetura de 4 serviços separados, você deve configurar os seguintes secrets e variáveis no GitHub.

## 🔐 Secrets do GitHub (Repository Secrets)

Acesse: `Settings` → `Secrets and variables` → `Actions` → `Repository secrets`

### 1. RENDER_API_KEY (OBRIGATÓRIO)
- **Valor**: Sua API key do Render
- **Como obter**: 
  1. Vá para https://dashboard.render.com/account
  2. Clique em "API Keys" 
  3. Gere uma nova API key
  4. Copie o valor para este secret

```bash
# Exemplo de teste da API key:
curl -H "Authorization: Bearer YOUR_API_KEY" \
     "https://api.render.com/v1/services"
```

## 🏷️ Variables do GitHub (Repository Variables)

Acesse: `Settings` → `Secrets and variables` → `Actions` → `Repository variables`

### Service IDs (Configure após criar os serviços no Render)

#### 2. RENDER_WEB_SERVICE_ID
- **Valor**: Service ID do Web Service (`gestao-de-pedidos-web`)
- **Como obter**: No dashboard do Render → Selecione o serviço → Copie o ID da URL

#### 3. RENDER_ORDER_SERVICE_ID  
- **Valor**: Service ID do Order Worker (`gestao-de-pedidos-order`)

#### 4. RENDER_PAYMENT_SERVICE_ID
- **Valor**: Service ID do Payment Worker (`gestao-de-pedidos-payment`)

#### 5. RENDER_INVENTORY_SERVICE_ID
- **Valor**: Service ID do Inventory Worker (`gestao-de-pedidos-inventory`)

## 📋 Passo a Passo de Configuração

### Etapa 1: Deploy Inicial
1. Configure apenas `RENDER_API_KEY`
2. Faça push para `main` - o deploy falhará nos triggers, mas isso é esperado
3. Acesse o Render Dashboard e observe os serviços sendo criados automaticamente

### Etapa 2: Obter Service IDs
Após a primeira execução, os serviços estarão criados no Render:

```bash
# Use a API do Render para listar todos os serviços:
curl -H "Authorization: Bearer YOUR_API_KEY" \
     "https://api.render.com/v1/services" | jq '.services[] | {name, id}'
```

Ou manualmente no dashboard:
- `gestao-de-pedidos-web` → Copie o ID da URL
- `gestao-de-pedidos-order` → Copie o ID da URL  
- `gestao-de-pedidos-payment` → Copie o ID da URL
- `gestao-de-pedidos-inventory` → Copie o ID da URL

### Etapa 3: Configurar Variables
Adicione os 4 Service IDs como repository variables no GitHub.

### Etapa 4: Teste Completo
Faça um novo push para `main` - agora o pipeline deve executar completamente e fazer deploy de todos os 4 serviços.

## 🎯 Arquitetura dos Serviços

```
┌─────────────────┐  ┌──────────────────┐
│   Web Service   │  │  Order Worker    │
│ (nginx+frontend │  │   (port 8081)    │
│  +query-service)│  │                  │
│   (port 80)     │  │                  │
└─────────────────┘  └──────────────────┘
         │                     │
         └─────────┬───────────┘
                   │
    ┌──────────────▼───────────────┐
    │     PostgreSQL Database      │
    │    order-postgres-db        │
    └──────────────┬───────────────┘
                   │
         ┌─────────▼───────────┐
         │   Payment Worker    │
         │    (port 8082)      │
         └─────────┬───────────┘
                   │
         ┌─────────▼───────────┐
         │  Inventory Worker   │
         │    (port 8083)      │
         └─────────────────────┘
                   │
         ┌─────────▼───────────┐
         │    Redis Cache      │
         │  order-redis-cache  │
         └─────────────────────┘
```

## 🔍 Verificação de Deploy

### URLs de Produção
- **Frontend**: https://gestao-de-pedidos-web.onrender.com
- **API Health**: https://gestao-de-pedidos-web.onrender.com/health
- **Workers**: Sem URL pública (background workers)

### Logs e Monitoramento
```bash
# Verificar status via API:
curl -H "Authorization: Bearer YOUR_API_KEY" \
     "https://api.render.com/v1/services/SERVICE_ID"

# Verificar deploys:
curl -H "Authorization: Bearer YOUR_API_KEY" \
     "https://api.render.com/v1/services/SERVICE_ID/deploys"
```

## ⚡ Otimizações de Performance

- **Web Service**: 128MB RAM (nginx + query-service + frontend)
- **Workers**: 96MB RAM cada (otimizado para background processing)
- **JVM Options**: Configurado para containers com heap limitado
- **Build Cache**: Maven cache otimizado no CI/CD

## 🚨 Troubleshooting

### Deploy falha com "Service ID not found"
- Verifique se os Service IDs estão corretos nas variables
- Confirme que os serviços existem no Render Dashboard

### Worker não inicia
- Verifique logs no Render Dashboard → Service → Logs
- Confirme se DATABASE_URL e REDIS_URL estão sendo injetadas
- Verifique se SERVICE_TYPE está correto

### Frontend não carrega
- Confirme se nginx está servindo os arquivos estáticos
- Verifique se a build do React foi bem-sucedida
- Teste endpoints da API diretamente

## 📞 Suporte

Em caso de problemas:
1. Verifique os logs no Render Dashboard
2. Execute o pipeline de CI/CD localmente
3. Teste containers Docker individualmente
4. Consulte documentação do Render: https://render.com/docs