# ✅ Checklist Deploy Render.com - Docker

## 🎯 Configurações Obrigatórias

### 1. **Build & Deploy Settings**
```
✅ Repository: Lucasantunesribeiro/Sistema_de_Gestao_de_Pedidos_Distribu-do_com_Event_Sourcing
✅ Branch: main
✅ Root Directory: unified-order-system
✅ Dockerfile Path: ./Dockerfile
✅ Docker Build Context Directory: ./
✅ Auto-Deploy: On Commit
```

### 2. **Environment Variables**
```bash
✅ DATABASE_URL=postgresql://[user]:[pass]@[host]:[port]/[db]
✅ SPRING_PROFILES_ACTIVE=render
✅ REDIS_ENABLED=false
✅ CACHE_TYPE=simple
```

### 3. **Health Check**
```
✅ Health Check Path: /actuator/health
```

## 🚀 Passos para Deploy

### **Passo 1: Verificar Configurações no Dashboard**
1. Acesse: https://dashboard.render.com/web/srv-d2kbhnruibrs73emmc8g
2. Vá em **Settings** → **Build & Deploy**
3. Confirme:
   - Root Directory: `unified-order-system`
   - Dockerfile Path: `./Dockerfile`
   - Docker Build Context Directory: `./`

### **Passo 2: Configurar Environment Variables**
1. Vá em **Environment**
2. Adicione as variáveis:
   ```
   DATABASE_URL=[copiar do PostgreSQL do Render]
   SPRING_PROFILES_ACTIVE=render
   REDIS_ENABLED=false
   CACHE_TYPE=simple
   ```

### **Passo 3: Fazer Deploy**
1. Clique em **Manual Deploy**
2. Selecione **Deploy latest commit**
3. Aguarde o build completar (5-10 minutos)

### **Passo 4: Verificar Deploy**
1. **Health Check**: https://gestao-de-pedidos.onrender.com/actuator/health
2. **API**: https://gestao-de-pedidos.onrender.com/api/orders
3. **Logs**: Verificar se não há erros

## 🔍 Verificações de Sucesso

### ✅ **Logs Esperados**
```
Configuring database connection to: dpg-xxxxx
Started Application in 45.123 seconds
Tomcat started on port(s): 10000 (http)
```

### ✅ **Health Check Response**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "healthConfig": {"status": "UP"}
  }
}
```

### ✅ **API Funcionando**
```bash
curl https://gestao-de-pedidos.onrender.com/api/orders
# Deve retornar: []
```

## 🚨 Problemas Comuns

### **Build Falha**
```
❌ Problema: "No such file or directory: ./Dockerfile"
✅ Solução: Verificar Root Directory = unified-order-system
```

### **Database Error**
```
❌ Problema: "Failed to configure a DataSource"
✅ Solução: Verificar DATABASE_URL nas Environment Variables
```

### **Health Check Fail**
```
❌ Problema: "Health check failed"
✅ Solução: Aguardar startup completo (pode levar 2-3 minutos)
```

### **Port Error**
```
❌ Problema: "Port 8080 already in use"
✅ Solução: Render define PORT automaticamente, não configurar manualmente
```

## 📋 Configuração Atual do Serviço

```
Service Name: Gestao_de_Pedidos
Service ID: srv-d2kbhnruibrs73emmc8g
Type: Web Service
Plan: Free
URL: https://gestao-de-pedidos.onrender.com
```

## 🎉 Deploy Bem-Sucedido

Quando tudo estiver funcionando:

1. ✅ **Build completo** sem erros
2. ✅ **Health check** retornando UP
3. ✅ **API** respondendo
4. ✅ **Database** conectado
5. ✅ **Logs** limpos

---

**🚀 Sistema pronto para uso em produção!**