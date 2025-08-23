# Deploy Fixes - Sistema de Gestão de Pedidos Distribuído

## 🎯 Problemas Identificados e Soluções

### 1. **BOM/CRLF no .dockerignore**
- **Problema**: Arquivo .dockerignore com BOM (﻿) causando problemas na interpretação
- **Solução**: Removido BOM e reorganizada estrutura de ignore

### 2. **CRLF Line Endings no start-all-services.sh**  
- **Problema**: Script com line endings CRLF (Windows) causando "no such file or directory"
- **Solução**: Convertido para LF usando `sed -i 's/\r$//'` + `dos2unix` no Dockerfile

### 3. **Configuração Incorreta do supervisord**
- **Problema**: api-gateway usando order-service.jar (duplicado)
- **Solução**: Removido api-gateway, configurados 4 serviços corretos com logs e retry

### 4. **Problemas no Dockerfile**
- **Problema**: CMD ao invés de ENTRYPOINT exec, falta /var/log, sem health check
- **Solução**: ENTRYPOINT exec, criação /var/log, health check com netcat

### 5. **Shebang e Script Robustez**
- **Problema**: #!/bin/sh sem error handling
- **Solução**: #!/usr/bin/env bash + set -euo pipefail + verificações

## 🔧 Arquivos Modificados

### .dockerignore
```diff
-﻿# Ignorar arquivos desnecessarios no build
+# Ignorar arquivos desnecessarios no build (removido BOM)
-target
+# Ignorar target apenas durante build, mas permitir copia no estágio final
+target
```

### Dockerfile
```diff
-# Install process manager and utilities
-RUN apk add --no-cache supervisor netcat-openbsd
+# Install process manager, utilities and debugging tools  
+RUN apk add --no-cache supervisor netcat-openbsd dos2unix bash

+# Create log directories
+RUN mkdir -p /var/log/supervisor /var/log

-# Create startup script
-COPY start-all-services.sh /app/start-all-services.sh
-RUN chmod +x /app/start-all-services.sh
+# Copy and prepare startup script
+COPY start-all-services.sh /app/start-all-services.sh
+RUN dos2unix /app/start-all-services.sh && chmod +x /app/start-all-services.sh

+# Health check
+HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
+  CMD nc -z localhost 8081 && nc -z localhost 8082 && nc -z localhost 8083 && nc -z localhost 8084 || exit 1

-CMD ["/app/start-all-services.sh"]
+ENTRYPOINT ["/app/start-all-services.sh"]
```

### start-all-services.sh
```diff
-#!/bin/sh
+#!/usr/bin/env bash
+set -euo pipefail

+# Debug: Verificar arquivos e diretórios
+echo "📁 Current directory: $(pwd)"
+echo "📄 Available files:"
+ls -la /app/

+# Verificar se arquivos JAR existem
+echo "🔍 Verificando arquivos JAR..."
+for jar in order-service.jar payment-service.jar inventory-service.jar query-service.jar; do
+    if [[ -f "/app/$jar" ]]; then
+        echo "✅ $jar encontrado"
+    else
+        echo "❌ $jar NÃO encontrado"
+        exit 1
+    fi
+done

-exec supervisord -c /etc/supervisor/conf.d/supervisord.conf
+exec /usr/bin/supervisord -n -c /etc/supervisor/conf.d/supervisord.conf
```

### supervisord.conf
```diff
-[program:api-gateway]
-command=java -Xmx200m -Dserver.port=8080 -jar /app/order-service.jar
-(removido - configuração incorreta)

+# Para todos os serviços:
-command=java -Xmx200m -Dserver.port=8081 -jar /app/order-service.jar
+command=java -Xmx150m -XX:+UseContainerSupport -Dserver.port=8081 -Dspring.profiles.active=render -jar /app/order-service.jar
+startretries=3
+startsecs=10
+stdout_logfile_maxbytes=50MB
+stdout_logfile_backups=2
```

## 🚀 Comandos de Verificação

### Build Local
```bash
docker build -t gestao:fix .
```

### Teste de Execução
```bash
# Verificar script e arquivos
docker run --rm -it gestao:fix /bin/sh -c "ls -la /app && file /app/start-all-services.sh"

# Teste de inicialização (dry run)
docker run --rm -it gestao:fix /bin/sh -c "/app/start-all-services.sh &"

# Verificar processos supervisord
docker run --rm -it gestao:fix /bin/sh -c "ps aux | grep supervisord"
```

### Health Check Test
```bash
# Testar health check manualmente
docker run --rm -p 8081:8081 -p 8082:8082 -p 8083:8083 -p 8084:8084 gestao:fix
# Em outro terminal:
curl http://localhost:8081/health || echo "Service not ready yet"
```

## 📋 Checklist Pós-Deploy

### ✅ Verificações Locais
- [ ] `docker build` funciona sem erros
- [ ] Script `/app/start-all-services.sh` existe e é executável  
- [ ] Arquivos JAR estão em `/app/`
- [ ] `file start-all-services.sh` não mostra CRLF
- [ ] Health check responde em todas as portas (8081-8084)

### ✅ Verificações Render
- [ ] Deploy não falha com "no such file or directory"
- [ ] Logs mostram "🚀 Starting Distributed Order Management System..."
- [ ] Logs mostram "✅ order-service.jar encontrado" (para todos JARs)
- [ ] Supervisord inicia todos os 4 serviços
- [ ] Health checks passam após 40s

### ✅ Verificações Funcionais
- [ ] Order Service responde em :8081
- [ ] Payment Service responde em :8082  
- [ ] Inventory Service responde em :8083
- [ ] Query Service responde em :8084
- [ ] Frontend conecta aos services corretamente

## 🔄 Rollback Plan

Se o deploy falhar:
```bash
git revert HEAD~3  # Reverter últimos 3 commits
git push origin main
```

## 📝 Mensagens de Commit

```bash
git add .dockerignore
git commit -m "fix(deploy): remove BOM from .dockerignore and restructure target ignore"

git add Dockerfile  
git commit -m "fix(deploy): add dos2unix, bash, health check and debug steps to Dockerfile"

git add start-all-services.sh
git commit -m "fix(deploy): convert to bash with error handling and JAR verification"

git add supervisord.conf
git commit -m "fix(deploy): remove duplicate api-gateway and improve service configs"

# Correção final
git add Dockerfile
git commit -m "fix(deploy): remove debug steps that require 'file' command"
```

## ⚠️ Problema Identificado no Deploy

**Erro:** `file: not found` na etapa de debug do Dockerfile

**Causa:** Comando `file` não disponível por padrão no Alpine Linux

**Solução:** Removidas etapas de debug temporárias (commit `fce7ae3`)

**Status:** ✅ Corrigido - logs mostram que script está sendo copiado corretamente:
```
-rwxr-xr-x 1 root root 1960 Aug 23 18:44 start-all-services.sh
```

## ⚠️ PROBLEMA CRÍTICO IDENTIFICADO: Port Binding Conflicts

**Erro:** Serviços crashando com `exit status 1` + "Port scan timeout, no open ports detected"

**Causa Raiz:** 
1. Todos os services configurados com `${PORT:8080}` no application-render.yml
2. Supervisord forçando portas específicas com `-Dserver.port=808X`
3. **CONFLITO**: Spring não consegue bind na porta devido a configurações conflitantes
4. **Render só detecta $PORT**, outros serviços ficam inacessíveis externamente

**Solução Implementada (commits `71c3755` + `9b3d851`):**

### Arquitetura Corrigida:
- **Order Service**: `${PORT:8080}` (porta principal do Render) - GATEWAY
- **Payment Service**: `8082` (porta fixa interna)
- **Inventory Service**: `8083` (porta fixa interna)  
- **Query Service**: `8084` (porta fixa interna)

### Configurações Spring:
- H2 databases separados (orderdb, paymentdb, inventorydb, querydb)
- Management endpoints com health details habilitados
- Configuração consistente de datasource e JPA
- Removido BOM de todos application-render.yml

### Supervisord:
- Removido `-Dserver.port` forçado (conflitava)
- Order Service recebe environment `PORT` do Render
- Permite Spring Boot configurar portas naturalmente

### Health Check:
- Ajustado para verificar apenas `${PORT:-8080}` principal

## 🎯 Performance Monitoring

### Render Metrics to Watch
- **Memory Usage**: < 512MB per service (150MB limite atual)  
- **CPU Usage**: < 50% sustained
- **Health Check Response**: < 10s
- **Service Startup Time**: < 40s

### Key Log Patterns
```
✅ order-service.jar encontrado          # JAR files detected
🔄 Iniciando todos os serviços...        # Starting services  
✅ order-service está pronto na porta    # Service ready
INFO spawned: 'order-service'           # Supervisord success
```

## 📞 Troubleshooting

### Problema: "no such file or directory" 
- Verificar line endings: `file /app/start-all-services.sh`
- Deve mostrar: "Bourne-Again shell script, UTF-8 text executable" (sem CRLF)

### Problema: JAR não encontrado
- Verificar build stage: `docker run --rm build-stage ls -la /app/services/*/target/`
- Verificar COPY paths no Dockerfile

### Problema: Supervisord não inicia
- Verificar logs: `docker logs <container>`
- Verificar config: `supervisorctl status` dentro do container

---
*Correções aplicadas por Claude Code para deploy no Render - Sistema de Gestão de Pedidos Distribuído*