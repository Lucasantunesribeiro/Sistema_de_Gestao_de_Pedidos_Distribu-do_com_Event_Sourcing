#!/bin/bash

# 🚀 Deploy Automático no Render via API REST
# Requer apenas curl e jq
# Configure sua API key do Render antes de executar

set -e

echo "🚀 Deploy Automático - Sistema de Gestão de Pedidos"
echo "=================================================="

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${GREEN}[$(date +'%H:%M:%S')] $1${NC}"; }
error() { echo -e "${RED}[ERROR] $1${NC}"; }
warn() { echo -e "${YELLOW}[WARN] $1${NC}"; }
info() { echo -e "${BLUE}[INFO] $1${NC}"; }

# Verificar dependências
for cmd in curl jq; do
    if ! command -v $cmd &> /dev/null; then
        error "$cmd não encontrado. Instale: sudo apt install $cmd"
        exit 1
    fi
done

# Configurar API Key
if [ -z "$RENDER_API_KEY" ]; then
    error "Configure a variável RENDER_API_KEY"
    echo "Obtenha sua API key em: https://dashboard.render.com/account/api-keys"
    echo "Execute: export RENDER_API_KEY=your_api_key_here"
    exit 1
fi

RENDER_API="https://api.render.com/v1"
HEADERS="Authorization: Bearer $RENDER_API_KEY"

log "API Key configurada ✓"

# Configurações do projeto
REPO_URL="https://github.com/Lucasantunesribeiro/Sistema_de_Gestao_de_Pedidos_Distribu-do_com_Event_Sourcing"
BRANCH="main"
REGION="oregon"

# Função para criar serviço
create_service() {
    local name=$1
    local type=$2
    local service_type=$3
    local plan=$4
    local port=$5
    
    log "Criando $name ($type)..."
    
    local payload
    if [ "$type" = "web" ]; then
        payload='{
            "type": "web_service",
            "name": "'$name'",
            "ownerId": "'$(curl -s -H "$HEADERS" "$RENDER_API/owners" | jq -r '.[0].id')'",
            "repo": "'$REPO_URL'",
            "branch": "'$BRANCH'",
            "serviceDetails": {
                "env": "docker",
                "region": "'$REGION'",
                "plan": "'$plan'",
                "healthCheckPath": "/health",
                "envVars": [
                    {"key": "SERVICE_TYPE", "value": "'$service_type'"},
                    {"key": "MESSAGING_TYPE", "value": "redis"},
                    {"key": "SPRING_PROFILES_ACTIVE", "value": "render"},
                    {"key": "JAVA_OPTS", "value": "-Xmx128m -XX:+UseContainerSupport"},
                    {"key": "PORT", "value": "'$port'"}
                ]
            }
        }'
    else
        payload='{
            "type": "background_worker",
            "name": "'$name'",
            "ownerId": "'$(curl -s -H "$HEADERS" "$RENDER_API/owners" | jq -r '.[0].id')'",
            "repo": "'$REPO_URL'",
            "branch": "'$BRANCH'",
            "serviceDetails": {
                "env": "docker",
                "region": "'$REGION'",
                "plan": "'$plan'",
                "envVars": [
                    {"key": "SERVICE_TYPE", "value": "'$service_type'"},
                    {"key": "MESSAGING_TYPE", "value": "redis"},
                    {"key": "SPRING_PROFILES_ACTIVE", "value": "render"},
                    {"key": "JAVA_OPTS", "value": "-Xmx96m -XX:+UseContainerSupport"},
                    {"key": "SERVER_PORT", "value": "'$port'"}
                ]
            }
        }'
    fi
    
    local response
    response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -H "$HEADERS" \
        -d "$payload" \
        "$RENDER_API/services")
    
    local service_id
    service_id=$(echo "$response" | jq -r '.id // empty')
    
    if [ -z "$service_id" ]; then
        error "Falha ao criar $name"
        echo "$response" | jq .
        return 1
    fi
    
    log "$name criado: $service_id"
    echo "$service_id"
}

# Função para aguardar serviço ficar online
wait_for_service() {
    local service_id=$1
    local name=$2
    local max_attempts=40
    local attempt=0
    
    log "Aguardando $name ficar online..."
    
    while [ $attempt -lt $max_attempts ]; do
        local status
        status=$(curl -s -H "$HEADERS" "$RENDER_API/services/$service_id" | jq -r '.status // "unknown"')
        
        case $status in
            "live")
                log "$name está online ✓"
                return 0
                ;;
            "build_failed"|"suspended"|"build_timeout")
                error "$name falhou: $status"
                return 1
                ;;
            *)
                info "$name: $status (tentativa $((attempt+1))/$max_attempts)"
                sleep 30
                ;;
        esac
        
        ((attempt++))
    done
    
    error "$name timeout após $((max_attempts * 30)) segundos"
    return 1
}

# Início do deploy
log "1️⃣ Iniciando deploy dos serviços..."

# Obter owner ID
OWNER_ID=$(curl -s -H "$HEADERS" "$RENDER_API/owners" | jq -r '.[0].id')
log "Owner ID: $OWNER_ID"

# IDs dos bancos de dados existentes
POSTGRES_ID="dpg-d2nr367fte5s7381n0n0-a"  # Já criado anteriormente
REDIS_ID="red-d2nr3795pdvs7394onhg"       # Já criado anteriormente

log "Usando bancos existentes:"
info "PostgreSQL: $POSTGRES_ID"
info "Redis: $REDIS_ID"

# Criar serviços
log "2️⃣ Criando Web Service..."
WEB_ID=$(create_service "gestao-pedidos-web" "web" "web" "free" "10000")

log "3️⃣ Criando Worker Services..."
ORDER_ID=$(create_service "gestao-pedidos-order" "worker" "order" "free" "8081")
PAYMENT_ID=$(create_service "gestao-pedidos-payment" "worker" "payment" "free" "8082")
INVENTORY_ID=$(create_service "gestao-pedidos-inventory" "worker" "inventory" "free" "8083")

# Configurar conexões entre bancos e serviços
log "4️⃣ Configurando conexões com bancos de dados..."

# Função para atualizar env vars de um serviço
update_env_vars() {
    local service_id=$1
    local name=$2
    
    log "Atualizando env vars para $name..."
    
    # Obter connection strings (simulado - na prática você obteria via API)
    local postgres_url="postgresql://order_system_postgres_user:password@dpg-d2nr367fte5s7381n0n0-a.oregon-postgres.render.com:5432/order_system_postgres"
    local redis_url="redis://default:password@red-d2nr3795pdvs7394onhg.oregon-redis.render.com:6379"
    
    # Atualizar variáveis
    curl -s -X PATCH \
        -H "Content-Type: application/json" \
        -H "$HEADERS" \
        -d '{
            "envVars": [
                {"key": "DATABASE_URL", "value": "'$postgres_url'"},
                {"key": "REDIS_URL", "value": "'$redis_url'"}
            ]
        }' \
        "$RENDER_API/services/$service_id" > /dev/null
    
    log "$name configurado ✓"
}

# Atualizar todos os serviços
for service in "web:$WEB_ID" "order:$ORDER_ID" "payment:$PAYMENT_ID" "inventory:$INVENTORY_ID"; do
    IFS=':' read -r name id <<< "$service"
    update_env_vars "$id" "$name"
done

# Aguardar todos os serviços ficarem online
log "5️⃣ Aguardando deploy concluir..."

declare -A services=(
    ["$WEB_ID"]="Web Service"
    ["$ORDER_ID"]="Order Worker"  
    ["$PAYMENT_ID"]="Payment Worker"
    ["$INVENTORY_ID"]="Inventory Worker"
)

all_success=true
for service_id in "${!services[@]}"; do
    if ! wait_for_service "$service_id" "${services[$service_id]}"; then
        all_success=false
    fi
done

# Resultado final
if [ "$all_success" = true ]; then
    log "🎉 Deploy concluído com sucesso!"
    echo ""
    echo "================================================"
    echo "🌐 URLs e Informações:"
    echo "================================================"
    
    # Obter URL do web service
    WEB_URL=$(curl -s -H "$HEADERS" "$RENDER_API/services/$WEB_ID" | jq -r '.serviceDetails.url // "https://gestao-pedidos-web.onrender.com"')
    
    echo "🖥️  Frontend: $WEB_URL"
    echo "🔍 Health: $WEB_URL/health"
    echo "📊 Dashboard: https://dashboard.render.com"
    echo ""
    echo "📋 Serviços criados:"
    echo "- Web Service: $WEB_ID"
    echo "- Order Worker: $ORDER_ID"
    echo "- Payment Worker: $PAYMENT_ID" 
    echo "- Inventory Worker: $INVENTORY_ID"
    echo ""
    log "✅ Sistema implantado com sucesso!"
    log "Acesse: $WEB_URL"
    
else
    error "Alguns serviços falharam no deploy"
    error "Verifique os logs no dashboard: https://dashboard.render.com"
    exit 1
fi