# CI/CD Configuration

Este diretório contém toda a configuração de CI/CD para o Sistema de Gestão de Pedidos Distribuído.

## 🚀 Workflows

### 1. `render-ci.yml` - Deploy Principal
Workflow completo executado no push para `main`:

- **Build & Test**: Compila Java + React, executa testes com Redis
- **Security Scan**: OWASP dependency check 
- **Docker Build**: Build e teste da imagem Docker
- **Performance Test**: Validação de bundle size
- **Deploy Notification**: Confirma deploy no Render

**Triggers**: Push para `main`
**Duração**: ~8-12 minutos

### 2. `pr-validation.yml` - Validação de PRs
Validação rápida para Pull Requests:

- Build check rápido
- Linting e type checking
- Testes críticos
- Validação de Dockerfile
- Security scan básico

**Triggers**: Pull Request para `main`
**Duração**: ~3-5 minutos

### 3. `cache-cleanup.yml` - Limpeza de Cache
Manutenção automática dos caches:

- Remove caches antigos (> 7 dias)
- Otimiza performance dos workflows
- Relatório de estatísticas

**Triggers**: Semanal (Segunda 2AM UTC) + Manual

## 📋 Configurações

### `dependabot.yml`
- Updates automáticos de dependências Maven e NPM
- Updates mensais para GitHub Actions
- PRs limitados para evitar spam

### `pull_request_template.md`
- Template padronizado para PRs
- Checklist de qualidade
- Guidelines de deploy

## 🔧 Configuração do Render

Para o deploy automático funcionar:

1. **Conectar Repositório**: No Render Dashboard, conecte este repo
2. **Auto-Deploy**: Configure para deploy automático no push para `main`
3. **Build Command**: O Render usará o Dockerfile automaticamente
4. **Variáveis de Ambiente**: Configure as variáveis necessárias no dashboard

### Variáveis de Ambiente Requeridas:
```bash
# Database
DATABASE_URL=postgresql://...
REDIS_URL=redis://...

# Services
PORT=80
JAVA_OPTS=-Xmx512m
NODE_ENV=production

# RabbitMQ (se usando)
RABBITMQ_URL=amqp://...
```

## 📊 Monitoramento

### Métricas Acompanhadas:
- ✅ Build time (target: < 8 min)
- ✅ Test coverage (target: > 80%)
- ✅ Bundle size (target: < 2MB)
- ✅ Docker image size
- ✅ Security vulnerabilities

### Health Checks:
- Frontend: `GET /`
- Backend: `GET /health`
- Services: Startup validation

## 🐛 Troubleshooting

### Build Failures Comuns:

1. **Maven Build Error**:
   ```bash
   # Local debug
   mvn clean install -X
   ```

2. **Frontend Build Error**:
   ```bash
   cd frontend
   npm ci
   npm run build
   ```

3. **Docker Build Error**:
   ```bash
   docker build --no-cache .
   ```

4. **Redis Connection Error**:
   - Verifique se o service Redis está rodando
   - Valide as variáveis de ambiente

### Performance Issues:

1. **Slow Tests**: Paralelize ou mova para integração
2. **Large Bundle**: Use code splitting no frontend
3. **Slow Docker Build**: Otimize layers no Dockerfile

## 🔒 Security

### Scans Executados:
- **OWASP Dependency Check**: Vulnerabilidades em dependências
- **TruffleHog**: Detecção de secrets em código
- **Maven Security Plugin**: Auditoria de dependências Java

### Best Practices:
- Nunca commitar secrets
- Usar variáveis de ambiente
- Manter dependências atualizadas
- Review de segurança em PRs

## 📈 Otimizações

### Cache Strategy:
- **Maven Dependencies**: Cache completo
- **Node Dependencies**: Cache por package-lock.json
- **Docker Layers**: Multi-stage build otimizado

### Performance Targets:
- **CI/CD Runtime**: < 10 minutos
- **First Paint**: < 1.5 segundos
- **API Response**: < 100ms
- **Build Size**: < 2MB frontend

## 🔄 Rollback Process

### Automatic Rollback:
O workflow detecta falhas e fornece instruções de rollback.

### Manual Rollback:
1. Render Dashboard → Deploys → Rollback
2. Ou via CLI: `render deploy rollback <service-id>`

### Emergency Rollback:
```bash
# Reverter último commit
git revert HEAD
git push origin main
```