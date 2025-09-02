# Correção Configuração Docker no Render - URGENTE

## 🚨 PROBLEMA IDENTIFICADO

**Logs mostram**: Spring Boot rodando diretamente na porta 10000, mas Render não detecta porta aberta.

**Causa**: Render está executando apenas o JAR Spring Boot, **NÃO está usando o Dockerfile** que configuramos com Nginx + Supervisord.

## 🔧 CORREÇÃO IMEDIATA NO RENDER DASHBOARD

### PASSO 1: Acessar Configuração do Serviço

1. **Vá para**: https://dashboard.render.com/
2. **Clique no serviço**: `gestao-de-pedidos`
3. **Vá em**: Settings → Build & Deploy

### PASSO 2: Alterar para Configuração Docker

**CONFIGURAÇÃO ATUAL (INCORRETA)**:
```
Build Command: mvn clean package -DskipTests
Start Command: java -jar unified-order-system/target/unified-order-system-1.0.0.jar
```

**NOVA CONFIGURAÇÃO (CORRETA)**:
```
Build Command: docker build -t app .
Start Command: docker run -p $PORT:$PORT -e PORT=$PORT -e SERVICE_TYPE=web -e DATABASE_URL="$DATABASE_URL" app
```

### PASSO 3: Configurar Environment Variables

Certifique-se que estas variáveis estão definidas:
```
SERVICE_TYPE=web
SPRING_PROFILES_ACTIVE=render
DATABASE_URL=[sua_database_url_atual]
```

### PASSO 4: Redeploy

1. **Salve as configurações**
2. **Clique em "Manual Deploy"**
3. **Aguarde o build Docker**

## 🎯 RESULTADO ESPERADO APÓS CORREÇÃO

### Logs que devem aparecer:
```
[program:nginx] RUNNING
[program:unified-order-system] RUNNING
nginx: configuration file /etc/nginx/nginx.conf test is successful
Tomcat started on port(s): 10000 (http)
Your service is live 🎉
```

### Funcionalidade:
- ✅ `https://gestao-de-pedidos.onrender.com/` → Frontend React
- ✅ `https://gestao-de-pedidos.onrender.com/api/orders` → API Spring Boot
- ✅ Render detecta porta aberta (Nginx na porta $PORT)

## 🚨 ALTERNATIVA RÁPIDA (Se Docker não funcionar)

Se houver problemas com Docker, configure temporariamente:

**Build Command**:
```bash
mvn clean package -DskipTests && apt-get update && apt-get install -y nginx
```

**Start Command**:
```bash
nginx && java -Dserver.port=$PORT -jar unified-order-system/target/unified-order-system-1.0.0.jar
```

Mas a **solução Docker é a correta** e deve ser priorizada.

## ✅ CHECKLIST DE VALIDAÇÃO

- [ ] Render configurado para usar Docker
- [ ] Environment variables definidas
- [ ] Build command: `docker build -t app .`
- [ ] Start command: `docker run -p $PORT:$PORT -e PORT=$PORT -e SERVICE_TYPE=web -e DATABASE_URL="$DATABASE_URL" app`
- [ ] Redeploy executado
- [ ] Logs mostram nginx + spring boot rodando
- [ ] Frontend carrega na URL raiz
- [ ] API funciona em /api/

---

**AÇÃO IMEDIATA**: Alterar configuração do Render para usar Docker ao invés de executar JAR diretamente.