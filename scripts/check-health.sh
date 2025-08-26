#!/bin/bash
# Script para verificar health dos serviços no Render
# Autor: Sistema automatizado  
# Uso: ./scripts/check-health.sh [local|render|all]

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Função para log com timestamp
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# URLs dos serviços por ambiente
declare -A RENDER_URLS=(
    ["order-service"]="https://order-service.onrender.com"
    ["payment-service"]="https://payment-service.onrender.com"
    ["inventory-service"]="https://inventory-service.onrender.com" 
    ["order-query-service"]="https://order-query-service.onrender.com"
    ["frontend"]="https://order-management-frontend.onrender.com"
)

declare -A LOCAL_URLS=(
    ["order-service"]="http://localhost:8081"
    ["payment-service"]="http://localhost:8082"
    ["inventory-service"]="http://localhost:8083"
    ["order-query-service"]="http://localhost:8084"
    ["frontend"]="http://localhost:3000"
)

# Função para verificar health de um serviço
check_service_health() {
    local service_name="$1"
    local base_url="$2"
    local max_retries="${3:-3}"
    local retry_interval="${4:-5}"
    
    log "Verificando $service_name em $base_url..."
    
    for ((i=1; i<=max_retries; i++)); do
        if [[ "$service_name" == "frontend" ]]; then
            # Para o frontend, apenas verifica se carrega (status 200)
            if curl -f -s -I --connect-timeout 10 --max-time 30 "$base_url" > /dev/null 2>&1; then
                success "$service_name está saudável (tentativa $i/$max_retries)"
                return 0
            fi
        else
            # Para serviços backend, verifica /actuator/health
            local health_url="$base_url/actuator/health"
            local response=$(curl -f -s --connect-timeout 10 --max-time 30 "$health_url" 2>/dev/null || echo "")
            
            if [[ -n "$response" ]] && echo "$response" | grep -q '"status":"UP"'; then
                success "$service_name está saudável (tentativa $i/$max_retries)"
                log "  Status: $(echo "$response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)"
                return 0
            fi
        fi
        
        if [[ $i -lt $max_retries ]]; then
            warning "$service_name não respondeu (tentativa $i/$max_retries). Tentando novamente em ${retry_interval}s..."
            sleep "$retry_interval"
        fi
    done
    
    error "$service_name não está saudável após $max_retries tentativas"
    return 1
}

# Função para verificar todos os serviços
check_all_services() {
    local env="$1"
    local urls_ref="$2"
    local max_retries="${3:-3}"
    local retry_interval="${4:-5}"
    
    log "=== VERIFICAÇÃO DE SAÚDE - AMBIENTE: $(echo $env | tr '[:lower:]' '[:upper:]') ==="
    
    # Cria referência aos arrays
    declare -n urls=$urls_ref
    
    local failed_services=0
    local total_services=${#urls[@]}
    
    # Verifica serviços em paralelo (mas mostra sequencial para clareza)
    for service in "${!urls[@]}"; do
        if ! check_service_health "$service" "${urls[$service]}" "$max_retries" "$retry_interval"; then
            ((failed_services++))
        fi
    done
    
    echo ""
    log "=== RESUMO DA VERIFICAÇÃO ==="
    log "Total de serviços: $total_services"
    log "Serviços saudáveis: $((total_services - failed_services))"
    log "Serviços com problemas: $failed_services"
    
    if [[ $failed_services -eq 0 ]]; then
        success "Todos os serviços estão saudáveis! 🎉"
        return 0
    else
        error "$failed_services serviço(s) apresentaram problemas"
        return 1
    fi
}

# Função para smoke test completo (testa operações básicas)
run_smoke_test() {
    local env="$1"
    
    if [[ "$env" != "render" ]]; then
        warning "Smoke test completo disponível apenas para ambiente Render"
        return 0
    fi
    
    log "=== EXECUTANDO SMOKE TEST COMPLETO ==="
    
    local order_service_url="${RENDER_URLS[order-service]}"
    local query_service_url="${RENDER_URLS[order-query-service]}"
    
    # Teste 1: Criar um pedido
    log "Testando criação de pedido..."
    local create_order_payload='{
        "customerId": "smoke-test-customer",
        "items": [
            {"productId": "product-1", "quantity": 2, "price": 29.99}
        ]
    }'
    
    local order_response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$create_order_payload" \
        "$order_service_url/api/orders" 2>/dev/null || echo "")
    
    if [[ -n "$order_response" ]] && echo "$order_response" | grep -q '"orderId"'; then
        local order_id=$(echo "$order_response" | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
        success "Pedido criado com sucesso: $order_id"
        
        # Teste 2: Consultar o pedido criado
        log "Testando consulta de pedido..."
        sleep 2  # Aguarda propagação
        
        local query_response=$(curl -s "$query_service_url/api/orders/$order_id" 2>/dev/null || echo "")
        
        if [[ -n "$query_response" ]] && echo "$query_response" | grep -q "$order_id"; then
            success "Consulta de pedido funcional"
        else
            warning "Consulta de pedido não retornou resultado esperado"
        fi
    else
        warning "Criação de pedido não funcionou como esperado"
    fi
    
    # Teste 3: Listar pedidos
    log "Testando listagem de pedidos..."
    local list_response=$(curl -s "$query_service_url/api/orders?limit=5" 2>/dev/null || echo "")
    
    if [[ -n "$list_response" ]] && (echo "$list_response" | grep -q '\[\]' || echo "$list_response" | grep -q '"orderId"'); then
        success "Listagem de pedidos funcional"
    else
        warning "Listagem de pedidos não retornou resultado esperado"
    fi
    
    success "Smoke test completo finalizado"
}

# Função principal
main() {
    local env="${1:-render}"
    local max_retries="${2:-5}"
    local retry_interval="${3:-10}"
    
    log "Iniciando verificação de saúde dos serviços"
    log "Ambiente: $env | Max retries: $max_retries | Intervalo: ${retry_interval}s"
    
    case "$env" in
        "local")
            check_all_services "local" "LOCAL_URLS" "$max_retries" "$retry_interval"
            ;;
        "render")
            check_all_services "render" "RENDER_URLS" "$max_retries" "$retry_interval"
            if [[ $? -eq 0 ]]; then
                run_smoke_test "render"
            fi
            ;;
        "all")
            log "Verificando ambiente local primeiro..."
            check_all_services "local" "LOCAL_URLS" 2 3
            echo ""
            log "Verificando ambiente Render..."
            check_all_services "render" "RENDER_URLS" "$max_retries" "$retry_interval"
            if [[ $? -eq 0 ]]; then
                run_smoke_test "render"
            fi
            ;;
        *)
            error "Ambiente inválido: $env"
            echo "Uso: $0 [local|render|all] [max_retries] [retry_interval]"
            echo "  local  - Verifica serviços locais (localhost)"
            echo "  render - Verifica serviços no Render"
            echo "  all    - Verifica ambos os ambientes"
            echo ""
            echo "Exemplos:"
            echo "  $0 render           # Verifica Render com defaults (5 retries, 10s interval)"
            echo "  $0 local 3 5        # Verifica local com 3 retries, 5s interval"
            echo "  $0 all              # Verifica ambos com defaults"
            exit 1
            ;;
    esac
    
    exit_code=$?
    
    if [[ $exit_code -eq 0 ]]; then
        success "Verificação de saúde concluída com sucesso! 🎊"
        log "Todos os serviços estão operacionais no ambiente $env"
    else
        error "Alguns serviços apresentaram problemas no ambiente $env"
        log "Verifique os logs acima para detalhes"
    fi
    
    exit $exit_code
}

# Verifica se está sendo executado diretamente
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi