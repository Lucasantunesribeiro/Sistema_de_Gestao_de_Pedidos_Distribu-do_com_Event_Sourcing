# Plano de Consolidação - URL Única no Render

## 🚨 PROBLEMA IDENTIFICADO

**Situação Atual**: Deploy foi feito em `https://gestao-pedidos-frontend.onrender.com/`
**Objetivo**: Consolidar tudo em `https://gestao-de-pedidos.onrender.com/`

### Causa Raiz:
Provavelmente temos **dois serviços separados** no Render:
1. `gestao-de-pedidos` - Pode estar configurado como Background Worker ou Private Service
2. `gestao-pedidos-frontend` - Configurado como Web Service (recebendo tráfego)

## 🎯 SOLUÇÃO: CONSOLIDAÇÃO EM UM ÚNICO SERVIÇO

### Estratégia Recomendada:
**Reconfigurar o serviço original** (`gestao-de-pedidos`) para ser um Web Service único que serve frontend + API.

## 📋 PLANO DE AÇÃO PASSO A PASSO

### FASE 1: VERIFICAÇÃO NO RENDER DASHBOARD

1. **Acesse o Render Dashboard**: https://dashboard.render.com/
2. **Identifique os serviços existentes**:
   - Procure por `gestao-de-pedidos`
   - Procure por `gestao-pedidos-frontend`
   - Anote o tipo de cada serviço (Web Service, Background Worker, etc.)

### FASE 2: RECONFIGURAÇÃO DO SERVIÇO ORIGINAL

#### Opção A: Se `gestao-de-pedidos` existe mas está como Background Worker

1. **Vá para o serviço `gestao-de-pedidos`**
2. **Settings → General**
3. **Mude o tipo para "Web Service"**
4. **Configure as seguintes variáveis de ambiente**:
   ```
   SERVICE_TYPE=web
   SPRING_PROFILES_ACTIVE=render
   ```
5. **Salve e faça redeploy**

#### Opção B: Se `gestao-de-pedidos` não existe ou está mal configurado

1. **Crie um novo Web Service**
2. **Nome**: `gestao-de-pedidos` (para manter a URL original)
3. **Repository**: Seu repositório GitHub
4. **Branch**: `main`
5. **Root Directory**: `.` (raiz do projeto)
6. **Build Command**: `docker build -t app .`
7. **Start Command**: `docker run -p $PORT:$PORT app`
8. **Environment Variables**:
   ```
   SERVICE_TYPE=web
   SPRING_PROFILES_ACTIVE=render
   DATABASE_URL=[sua_database_url]
   ```

### FASE 3: LIMPEZA

1. **Delete o serviço `gestao-pedidos-frontend`** (se existir)
2. **Confirme que apenas `gestao-de-pedidos` está ativo**

## 🔧 CONFIGURAÇÃO CORRETA ESPERADA

### Serviço Único: `gestao-de-pedidos`
- **Tipo**: Web Service
- **URL**: `https://gestao-de-pedidos.onrender.com/`
- **Dockerfile**: O que acabamos de corrigir
- **Processos**: Nginx (frontend) + Unified Spring Boot (API)

### Resultado Final:
```
https://gestao-de-pedidos.onrender.com/     → Frontend React
https://gestao-de-pedidos.onrender.com/api/ → API Spring Boot
```

## ⚡ AÇÃO IMEDIATA RECOMENDADA

### Se você tem acesso ao Render Dashboard:

1. **Vá para**: https://dashboard.render.com/
2. **Encontre o serviço**: `gestao-de-pedidos`
3. **Verifique se é "Web Service"**
4. **Se não for, mude para Web Service**
5. **Configure variável**: `SERVICE_TYPE=web`
6. **Faça redeploy**

### Se o serviço original não existe:

1. **Crie novo Web Service** com nome `gestao-de-pedidos`
2. **Use as configurações acima**
3. **Delete o serviço `gestao-pedidos-frontend`**

## ✅ CRITÉRIOS DE SUCESSO

- ✅ Apenas UM serviço ativo: `gestao-de-pedidos`
- ✅ URL funcional: `https://gestao-de-pedidos.onrender.com/`
- ✅ Frontend carrega na raiz
- ✅ API funciona em `/api/`
- ✅ Nenhum serviço duplicado

## 🚨 IMPORTANTE

**NÃO delete o serviço original** se ele contém configurações importantes (database, environment variables). **Reconfigure-o** ao invés de criar um novo.

---

**PRÓXIMO PASSO**: Verificar configuração atual no Render Dashboard e aplicar a reconfiguração apropriada.