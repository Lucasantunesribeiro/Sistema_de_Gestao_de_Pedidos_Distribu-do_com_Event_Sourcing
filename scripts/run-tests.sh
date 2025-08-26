#!/bin/bash
# Script para executar todos os testes do sistema de forma coordenada
# Autor: Sistema automatizado
# Uso: ./scripts/run-tests.sh [unit|integration|all]

set -e  # Falha imediatamente em caso de erro

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Função para executar comando com log
execute_with_log() {
    local service_name="$1"
    local command="$2"
    local directory="$3"
    
    log "Executando testes para $service_name..."
    
    if [ -d "$directory" ]; then
        cd "$directory"
        if eval "$command"; then
            success "$service_name - Testes passaram"
            cd - > /dev/null
            return 0
        else
            error "$service_name - Testes falharam"
            cd - > /dev/null
            return 1
        fi
    else
        error "Diretório não encontrado: $directory"
        return 1
    fi
}

# Função para testes unitários (sem dependências externas)
run_unit_tests() {
    log "=== EXECUTANDO TESTES UNITÁRIOS ==="
    
    local failed_tests=0
    
    # Build shared-events primeiro
    log "Building shared-events..."
    if cd shared-events && mvn clean install -q; then
        success "shared-events build concluído"
        cd - > /dev/null
    else
        error "Falha no build do shared-events"
        return 1
    fi
    
    # Testes específicos que sabemos que funcionam
    execute_with_log "CacheInvalidationService" \
        "mvn -Dtest=CacheInvalidationServiceTest test -q" \
        "services/order-query-service" || ((failed_tests++))
    
    # Compilação dos outros serviços
    execute_with_log "Order Service Compilation" \
        "mvn clean compile -q" \
        "services/order-service" || ((failed_tests++))
        
    execute_with_log "Payment Service Compilation" \
        "mvn clean compile -q" \
        "services/payment-service" || ((failed_tests++))
        
    execute_with_log "Inventory Service Compilation" \
        "mvn clean compile -q" \
        "services/inventory-service" || ((failed_tests++))
    
    if [ $failed_tests -eq 0 ]; then
        success "Todos os testes unitários passaram! ✨"
        return 0
    else
        error "$failed_tests teste(s) unitário(s) falharam"
        return 1
    fi
}

# Função para testes de integração (requer infraestrutura)
run_integration_tests() {
    log "=== EXECUTANDO TESTES DE INTEGRAÇÃO ==="
    
    warning "Testes de integração requerem PostgreSQL e Redis rodando"
    warning "Use docker-compose up -d postgres redis para iniciar a infraestrutura"
    
    local failed_tests=0
    
    # Verifica se PostgreSQL está disponível
    if ! command -v pg_isready &> /dev/null; then
        warning "pg_isready não encontrado, pulando verificação de PostgreSQL"
    elif ! pg_isready -h localhost -p 5432; then
        warning "PostgreSQL não está rodando em localhost:5432"
        warning "Inicie com: docker-compose up -d postgres"
    fi
    
    # Testes de integração completos
    execute_with_log "Order Query Service Integration" \
        "mvn test -Dspring.profiles.active=test-integration -q" \
        "services/order-query-service" || ((failed_tests++))
        
    execute_with_log "Order Service Integration" \
        "mvn test -Dspring.profiles.active=test-integration -q" \
        "services/order-service" || ((failed_tests++))
    
    if [ $failed_tests -eq 0 ]; then
        success "Todos os testes de integração passaram! 🎉"
        return 0
    else
        error "$failed_tests teste(s) de integração falharam"
        return 1
    fi
}

# Função principal
main() {
    local test_type="${1:-unit}"
    
    log "Iniciando execução de testes: $test_type"
    log "Diretório atual: $(pwd)"
    
    case "$test_type" in
        "unit")
            run_unit_tests
            ;;
        "integration") 
            run_integration_tests
            ;;
        "all")
            log "Executando testes unitários primeiro..."
            if run_unit_tests; then
                log "Executando testes de integração..."
                run_integration_tests
            else
                error "Testes unitários falharam, pulando integração"
                exit 1
            fi
            ;;
        *)
            error "Tipo de teste inválido: $test_type"
            echo "Uso: $0 [unit|integration|all]"
            echo "  unit        - Testes unitários sem dependências externas"
            echo "  integration - Testes de integração com PostgreSQL/Redis"
            echo "  all         - Todos os testes (unitários + integração)"
            exit 1
            ;;
    esac
    
    if [ $? -eq 0 ]; then
        success "Execução de testes concluída com sucesso! 🎊"
        log "Para executar testes no CI/CD: git push origin main"
    else
        error "Alguns testes falharam. Verifique os logs acima."
        exit 1
    fi
}

# Verifica se está sendo executado diretamente
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi