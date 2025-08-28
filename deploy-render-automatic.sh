#!/bin/bash

# 🚀 Deploy Automático no Render - Sistema de Gestão de Pedidos
# Este script usa o Render CLI para deploy automático
# Requer: render-cli instalado e autenticado

set -e

echo "🚀 Iniciando deploy automático no Render..."
echo "================================================"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para log colorido
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[WARN] $1${NC}"
}

info() {
    echo -e "${BLUE}[INFO] $1${NC}"
}

# Verificar se render CLI está instalado
if ! command -v render &> /dev/null; then
    error "Render CLI não encontrado. Instalando..."
    
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        brew install render-cli
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        curl -fsSL https://cli.render.com/install | sh
    else
        error "OS não suportado. Instale manualmente: https://render.com/docs/cli"
        exit 1
    fi
fi

# Verificar autenticação
log "Verificando autenticação Render CLI..."
if ! render auth whoami &> /dev/null; then
    warn "Não autenticado. Execute: render auth login"
    echo "Após autenticar, execute este script novamente."
    exit 1
fi

# Informações do usuário
RENDER_USER=$(render auth whoami | grep "Email" | cut -d: -f2 | xargs)
log "Usuário autenticado: $RENDER_USER"

# Configurações
REPO_URL="https://github.com/Lucasantunesribeiro/Sistema_de_Gestao_de_Pedidos_Distribu-do_com_Event_Sourcing"
REGION="oregon"
BRANCH="main"

log "Configurações:"
info "Repository: $REPO_URL"
info "Region: $REGION"
info "Branch: $BRANCH"

# Verificar se os bancos já existem
log "Verificando recursos existentes..."

# 1. Criar PostgreSQL (se não existir)
log "1️⃣ Configurando PostgreSQL..."
if render services list | grep -q "order-system-postgres"; then
    info "PostgreSQL já existe: order-system-postgres"
    POSTGRES_ID=$(render services list | grep "order-system-postgres" | awk '{print $1}')
else
    log "Criando PostgreSQL..."
    POSTGRES_ID=$(render services create --type postgresql \
        --name "order-system-postgres" \
        --plan free \
        --region $REGION \
        --database-name "order_system_db" \
        --database-user "order_system_user" \
        --output json | jq -r '.id')
    log "PostgreSQL criado: $POSTGRES_ID"
fi

# 2. Criar Redis (se não existir)
log "2️⃣ Configurando Redis..."
if render services list | grep -q "order-system-redis"; then
    info "Redis já existe: order-system-redis"
    REDIS_ID=$(render services list | grep "order-system-redis" | awk '{print $1}')
else
    log "Criando Redis..."
    REDIS_ID=$(render services create --type redis \
        --name "order-system-redis" \
        --plan free \
        --region $REGION \
        --output json | jq -r '.id')
    log "Redis criado: $REDIS_ID"
fi

# 3. Criar Web Service
log "3️⃣ Criando Web Service (Frontend + Query API)..."
WEB_SERVICE_ID=$(render services create --type web \
    --name "gestao-pedidos-web" \
    --runtime docker \
    --repo $REPO_URL \
    --branch $BRANCH \
    --plan free \
    --region $REGION \
    --health-check-path "/health" \
    --env "SERVICE_TYPE=web" \
    --env "MESSAGING_TYPE=redis" \
    --env "SPRING_PROFILES_ACTIVE=render" \
    --env "JAVA_OPTS=-Xmx128m -XX:+UseContainerSupport" \
    --output json | jq -r '.id')

log "Web Service criado: $WEB_SERVICE_ID"

# 4. Criar Worker Services
log "4️⃣ Criando Worker Services..."

# Order Service Worker
log "Criando Order Service Worker..."
ORDER_SERVICE_ID=$(render services create --type worker \
    --name "gestao-pedidos-order" \
    --runtime docker \
    --repo $REPO_URL \
    --branch $BRANCH \
    --plan free \
    --region $REGION \
    --env "SERVICE_TYPE=order" \
    --env "MESSAGING_TYPE=redis" \
    --env "SPRING_PROFILES_ACTIVE=render" \
    --env "JAVA_OPTS=-Xmx96m -XX:+UseContainerSupport" \
    --env "SERVER_PORT=8081" \
    --output json | jq -r '.id')

log "Order Service criado: $ORDER_SERVICE_ID"

# Payment Service Worker
log "Criando Payment Service Worker..."
PAYMENT_SERVICE_ID=$(render services create --type worker \
    --name "gestao-pedidos-payment" \
    --runtime docker \
    --repo $REPO_URL \
    --branch $BRANCH \
    --plan free \
    --region $REGION \
    --env "SERVICE_TYPE=payment" \
    --env "MESSAGING_TYPE=redis" \
    --env "SPRING_PROFILES_ACTIVE=render" \
    --env "JAVA_OPTS=-Xmx96m -XX:+UseContainerSupport" \
    --env "SERVER_PORT=8082" \
    --output json | jq -r '.id')

log "Payment Service criado: $PAYMENT_SERVICE_ID"

# Inventory Service Worker
log "Criando Inventory Service Worker..."
INVENTORY_SERVICE_ID=$(render services create --type worker \
    --name "gestao-pedidos-inventory" \
    --runtime docker \
    --repo $REPO_URL \
    --branch $BRANCH \
    --plan free \
    --region $REGION \
    --env "SERVICE_TYPE=inventory" \
    --env "MESSAGING_TYPE=redis" \
    --env "SPRING_PROFILES_ACTIVE=render" \
    --env "JAVA_OPTS=-Xmx96m -XX:+UseContainerSupport" \
    --env "SERVER_PORT=8083" \
    --output json | jq -r '.id')

log "Inventory Service criado: $INVENTORY_SERVICE_ID"

# 5. Configurar conexões entre serviços
log "5️⃣ Configurando conexões entre serviços..."

# Obter connection strings
POSTGRES_CONNECTION=$(render services env get $POSTGRES_ID DATABASE_URL)
REDIS_CONNECTION=$(render services env get $REDIS_ID REDIS_URL)

# Atualizar variáveis de ambiente para todos os serviços
for SERVICE_ID in $WEB_SERVICE_ID $ORDER_SERVICE_ID $PAYMENT_SERVICE_ID $INVENTORY_SERVICE_ID; do
    log "Configurando conexões para serviço: $SERVICE_ID"
    render services env set $SERVICE_ID DATABASE_URL="$POSTGRES_CONNECTION"
    render services env set $SERVICE_ID REDIS_URL="$REDIS_CONNECTION"
done

# 6. Aguardar deploy
log "6️⃣ Aguardando conclusão do deploy..."
log "Isso pode levar 15-20 minutos..."

# Monitorar status dos serviços
SERVICES=($WEB_SERVICE_ID $ORDER_SERVICE_ID $PAYMENT_SERVICE_ID $INVENTORY_SERVICE_ID)
ALL_READY=false
ATTEMPTS=0
MAX_ATTEMPTS=60  # 30 minutos (30s cada)

while [ "$ALL_READY" = false ] && [ $ATTEMPTS -lt $MAX_ATTEMPTS ]; do
    ALL_READY=true
    
    for SERVICE_ID in "${SERVICES[@]}"; do
        STATUS=$(render services status $SERVICE_ID | grep "Status:" | cut -d: -f2 | xargs)
        
        if [ "$STATUS" != "live" ]; then
            ALL_READY=false
            info "Aguardando serviço $SERVICE_ID (status: $STATUS)"
            break
        fi
    done
    
    if [ "$ALL_READY" = false ]; then
        sleep 30
        ((ATTEMPTS++))
        info "Tentativa $ATTEMPTS/$MAX_ATTEMPTS..."
    fi
done

if [ "$ALL_READY" = true ]; then
    log "🎉 Deploy concluído com sucesso!"
    echo ""
    echo "================================================"
    echo "🌐 URLs dos Serviços:"
    echo "================================================"
    
    WEB_URL=$(render services get $WEB_SERVICE_ID | grep "URL:" | cut -d: -f2- | xargs)
    echo "🖥️  Frontend: $WEB_URL"
    echo "🔍 Health Check: $WEB_URL/health"
    echo "📊 Dashboard Render: https://dashboard.render.com"
    
    echo ""
    echo "📋 Recursos criados:"
    echo "- PostgreSQL: order-system-postgres ($POSTGRES_ID)"
    echo "- Redis: order-system-redis ($REDIS_ID)"
    echo "- Web Service: gestao-pedidos-web ($WEB_SERVICE_ID)"
    echo "- Order Worker: gestao-pedidos-order ($ORDER_SERVICE_ID)"
    echo "- Payment Worker: gestao-pedidos-payment ($PAYMENT_SERVICE_ID)"
    echo "- Inventory Worker: gestao-pedidos-inventory ($INVENTORY_SERVICE_ID)"
    
    echo ""
    log "✅ Sistema de Gestão de Pedidos implantado com sucesso!"
    log "Acesse: $WEB_URL"
    
else
    error "Deploy não concluído no tempo esperado."
    error "Verifique os logs no dashboard: https://dashboard.render.com"
    exit 1
fi