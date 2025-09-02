# Correção Paths Docker no Render - SOLUÇÃO DEFINITIVA

## 🚨 PROBLEMA IDENTIFICADO

**Erro**: `"docker": executable file not found in $PATH`

**Causa**: Configuração de paths incorreta no Render. O serviço está tentando executar Docker no diretório errado.

## 🔧 CORREÇÃO IMEDIATA NO RENDER DASHBOARD

### CONFIGURAÇÃO ATUAL (INCORRETA):
```
Root Directory: unified-order-system
Dockerfile Path: unified-order-system/./Dockerfile
Docker Build Context: unified-order-system/ docker build -t app
```

### CONFIGURAÇÃO CORRETA:
```
Root Directory: . (deixar vazio ou ponto)
Dockerfile Path: ./Dockerfile
Docker Build Context Directory: .
Docker Command: docker run -p $PORT:$PORT -e PORT=$PORT -e SERVICE_TYPE=web -e DATABASE_URL="$DATABASE_URL" app
```

## 📋 PASSO A PASSO PARA CORRIGIR

### 1. Root Directory
- **Atual**: `unified-order-system`
- **Correto**: `.` (ou deixar vazio)
- **Clique em Edit** → Apague o conteúdo → Salve

### 2. Dockerfile Path  
- **Atual**: `unified-order-system/./Dockerfile`
- **Correto**: `./Dockerfile`
- **Clique em Edit** → Mude para `./Dockerfile` → Salve

### 3. Docker Build Context Directory
- **Atual**: `unified-order-system/ docker build -t app`
- **Correto**: `.`
- **Clique em Edit** → Mude para apenas `.` → Salve

### 4. Docker Command (já está correto)
- **Manter**: `docker run -p $PORT:$PORT -e PORT=$PORT -e SERVICE_TYPE=web app`
- **Ou melhorar para**: `docker run -p $PORT:$PORT -e PORT=$PORT -e SERVICE_TYPE=web -e DATABASE_URL="$DATABASE_URL" app`

## 🎯 POR QUE ISSO VAI FUNCIONAR

**Estrutura do Repositório**:
```
/ (raiz)
├── Dockerfile ← Precisa estar aqui
├── unified-order-system/
│   └── (código Java)
└── frontend/
    └── (código React)
```

**O Dockerfile precisa**:
- Estar na raiz para acessar ambos os diretórios
- Build context na raiz (`.`) para copiar tudo
- Root directory na raiz para encontrar o Dockerfile

## ✅ RESULTADO ESPERADO APÓS CORREÇÃO

### Build vai funcionar:
```
==> Building with Docker...
==> Successfully built Docker image
==> Starting container...
[program:nginx] RUNNING
[program:unified-order-system] RUNNING
==> Your service is live 🎉
```

### Aplicação funcionando:
- ✅ `https://gestao-de-pedidos.onrender.com/` → Frontend React
- ✅ `https://gestao-de-pedidos.onrender.com/api/orders` → API Spring Boot

## 🚨 ALTERNATIVA SE DOCKER CONTINUAR FALHANDO

Se mesmo com paths corretos o Docker falhar, use esta configuração temporária:

**Build Command**:
```bash
cd unified-order-system && mvn clean package -DskipTests && cd .. && apt-get update && apt-get install -y nginx supervisor
```

**Start Command**:
```bash
cp deploy/nginx/nginx.conf.template /tmp/nginx.conf && envsubst '$PORT' < /tmp/nginx.conf > /etc/nginx/nginx.conf && cp deploy/supervisord/web.conf /etc/supervisor/conf.d/ && supervisord -c /etc/supervisor/conf.d/web.conf
```

Mas **priorize a correção Docker** que é a solução correta.

---

**AÇÃO IMEDIATA**: Corrigir os 3 paths no Render Dashboard e fazer redeploy.