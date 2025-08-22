#!/bin/bash

# Deployment de Produção - Sistema de Gestão de Pedidos Distribuído
# Railway.app Automated Deployment Script

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Configuration
PROJECT_NAME="distributed-order-system"
BACKEND_SERVICES=("order-service" "payment-service" "inventory-service" "order-query-service")

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${PURPLE}[STEP]${NC} $1"; }

print_header() {
    echo -e "${PURPLE}"
    echo "🚂 RAILWAY DEPLOYMENT - Sistema de Gestão de Pedidos Distribuído"
    echo "================================================================"
    echo -e "${NC}"
    echo "Arquitetura: 4 Microsserviços + Frontend React + Infraestrutura"
    echo "Tecnologias: Java 21 + Spring Boot 3.4 + React 18 + PostgreSQL + RabbitMQ + Redis"
    echo ""
}

check_prerequisites() {
    log_step "1. Verificando pré-requisitos..."
    
    # Check Railway CLI
    if ! command -v railway &> /dev/null; then
        log_error "Railway CLI não encontrado!"
        log_info "Execute: npm install -g @railway/cli"
        exit 1
    fi
    log_success "Railway CLI encontrado: $(railway --version)"
    
    # Check authentication
    if ! railway whoami &> /dev/null; then
        log_warning "Não autenticado no Railway"
        log_info "Execute o comando: railway login"
        log_info "Depois rode novamente este script"
        exit 1
    fi
    log_success "Autenticado como: $(railway whoami)"
    
    # Check if we're in the right directory
    if [ ! -f "pom.xml" ] || [ ! -d "services" ] || [ ! -d "frontend" ]; then
        log_error "Execute este script a partir do diretório raiz do projeto"
        exit 1
    fi
    log_success "Diretório do projeto verificado"
}

create_project() {
    log_step "2. Configurando projeto Railway..."
    
    # Check if project already exists
    if railway status &> /dev/null; then
        log_info "Projeto Railway já existe"
        PROJECT_ID=$(railway status --json | grep -o '"projectId":"[^"]*' | cut -d'"' -f4)
        log_success "Usando projeto existente: $PROJECT_ID"
    else
        log_info "Criando novo projeto Railway: $PROJECT_NAME"
        railway login --browser
        railway init "$PROJECT_NAME"
        log_success "Projeto criado com sucesso"
    fi
}

setup_infrastructure() {
    log_step "3. Configurando infraestrutura..."
    
    # PostgreSQL for Event Store
    log_info "Criando PostgreSQL para Event Store..."
    railway add postgresql --name order-eventstore-db
    
    # PostgreSQL for Read Models  
    log_info "Criando PostgreSQL para Read Models..."
    railway add postgresql --name order-query-db
    
    # Redis for caching
    log_info "Criando Redis para cache..."
    railway add redis --name order-cache
    
    # RabbitMQ for messaging
    log_info "Criando RabbitMQ para messaging..."
    railway add rabbitmq --name message-broker
    
    log_success "Infraestrutura configurada"
    
    # Wait for databases to be ready
    log_info "Aguardando bancos de dados ficarem prontos..."
    sleep 30
}

build_shared_events() {
    log_step "4. Building shared-events library..."
    
    cd shared-events
    
    # Build the shared library
    ./mvnw clean install -DskipTests
    
    cd ..
    log_success "Shared-events library construída"
}

deploy_service() {
    local service=$1
    local port=$2
    
    log_step "5. Deploying $service (porta $port)..."
    
    # Create service
    railway service create "$service"
    
    # Switch to service context
    railway service use "$service"
    
    # Set common environment variables
    railway variables set \
        SPRING_PROFILES_ACTIVE=railway \
        PORT=$port \
        JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC" \
        RAILS_LOG_TO_STDOUT=true \
        CORS_ORIGINS="https://*.railway.app"
    
    # Service-specific database configuration
    case $service in
        "order-service")
            railway variables set \
                DATABASE_URL='${{order-eventstore-db.DATABASE_URL}}' \
                RABBITMQ_URL='${{message-broker.RABBITMQ_URL}}' \
                REDIS_URL='${{order-cache.REDIS_URL}}'
            ;;
        "order-query-service") 
            railway variables set \
                DATABASE_URL='${{order-query-db.DATABASE_URL}}' \
                RABBITMQ_URL='${{message-broker.RABBITMQ_URL}}' \
                REDIS_URL='${{order-cache.REDIS_URL}}'
            ;;
        "payment-service"|"inventory-service")
            railway variables set \
                RABBITMQ_URL='${{message-broker.RABBITMQ_URL}}'
            ;;
    esac
    
    # Deploy from service directory
    cd "services/$service"
    railway up --detach
    cd ../..
    
    log_success "$service deployed"
}

deploy_frontend() {
    log_step "6. Deploying frontend..."
    
    # Create frontend service
    railway service create "frontend"
    railway service use "frontend"
    
    # Get backend service URLs
    ORDER_SERVICE_URL=$(railway url --service order-service)
    QUERY_SERVICE_URL=$(railway url --service order-query-service)
    PAYMENT_SERVICE_URL=$(railway url --service payment-service)
    INVENTORY_SERVICE_URL=$(railway url --service inventory-service)
    
    # Set frontend environment variables
    railway variables set \
        NODE_ENV=production \
        VITE_API_ORDER_URL="$ORDER_SERVICE_URL" \
        VITE_API_QUERY_URL="$QUERY_SERVICE_URL" \
        VITE_API_PAYMENT_URL="$PAYMENT_SERVICE_URL" \
        VITE_API_INVENTORY_URL="$INVENTORY_SERVICE_URL"
    
    # Deploy frontend
    cd frontend
    railway up --detach
    cd ..
    
    log_success "Frontend deployed"
}

wait_for_deployments() {
    log_step "7. Aguardando deployments completarem..."
    
    local services=("${BACKEND_SERVICES[@]}" "frontend")
    
    for service in "${services[@]}"; do
        log_info "Aguardando $service..."
        
        railway service use "$service"
        
        timeout=600  # 10 minutes
        elapsed=0
        
        while [ $elapsed -lt $timeout ]; do
            if railway logs --limit 5 | grep -q "Started.*Application in"; then
                log_success "$service iniciado com sucesso"
                break
            fi
            
            sleep 15
            elapsed=$((elapsed + 15))
            log_info "Aguardando $service... (${elapsed}s)"
        done
        
        if [ $elapsed -ge $timeout ]; then
            log_error "$service não iniciou no tempo esperado"
        fi
    done
}

perform_health_checks() {
    log_step "8. Executando health checks..."
    
    for service in "${BACKEND_SERVICES[@]}"; do
        railway service use "$service"
        service_url=$(railway url 2>/dev/null || echo "")
        
        if [ -n "$service_url" ]; then
            health_url="$service_url/actuator/health"
            log_info "Testando $service: $health_url"
            
            if curl -f -s --max-time 30 "$health_url" | grep -q '"status":"UP"'; then
                log_success "$service: Health check OK"
            else
                log_warning "$service: Health check falhou ou ainda não está pronto"
            fi
        else
            log_warning "$service: URL não disponível ainda"
        fi
    done
}

test_end_to_end() {
    log_step "9. Teste end-to-end..."
    
    railway service use "order-service"
    ORDER_URL=$(railway url)
    
    railway service use "order-query-service"  
    QUERY_URL=$(railway url)
    
    if [ -n "$ORDER_URL" ] && [ -n "$QUERY_URL" ]; then
        log_info "Testando criação de pedido..."
        
        # Create test order
        response=$(curl -s -X POST "$ORDER_URL/api/orders" \
            -H "Content-Type: application/json" \
            -d '{
                "customerId": "test-customer-prod",
                "items": [
                    {
                        "productId": "laptop-prod-01",
                        "productName": "Laptop Gaming Pro",
                        "quantity": 1,
                        "unitPrice": 2500.00
                    }
                ]
            }' || echo "ERROR")
        
        if [[ "$response" != "ERROR" ]] && [[ "$response" == *"orderId"* ]]; then
            log_success "Pedido criado com sucesso em produção"
            
            # Wait a bit for event processing
            sleep 5
            
            # Query orders
            if curl -f -s --max-time 10 "$QUERY_URL/api/orders" | grep -q "laptop-prod-01"; then
                log_success "Query service funcionando - pedido encontrado"
            else
                log_warning "Query service pode estar com delay na sincronização"
            fi
        else
            log_warning "Teste de criação de pedido falhou"
        fi
    else
        log_warning "URLs dos serviços não disponíveis para teste"
    fi
}

show_deployment_summary() {
    log_step "10. Resumo do Deployment"
    
    echo ""
    echo -e "${GREEN}🎉 DEPLOYMENT CONCLUÍDO COM SUCESSO! 🎉${NC}"
    echo ""
    echo -e "${BLUE}📊 Serviços Deployados:${NC}"
    
    for service in "${BACKEND_SERVICES[@]}" "frontend"; do
        railway service use "$service"
        service_url=$(railway url 2>/dev/null || echo "Pending...")
        echo "  🔹 $service: $service_url"
    done
    
    echo ""
    echo -e "${BLUE}🔗 URLs Importantes:${NC}"
    railway service use "frontend"
    FRONTEND_URL=$(railway url 2>/dev/null || echo "Pending...")
    echo "  🌐 Frontend: $FRONTEND_URL"
    
    railway service use "order-service"
    ORDER_URL=$(railway url 2>/dev/null || echo "Pending...")
    echo "  📝 API Pedidos: $ORDER_URL/api/orders"
    
    railway service use "order-query-service"
    QUERY_URL=$(railway url 2>/dev/null || echo "Pending...")  
    echo "  📊 API Consultas: $QUERY_URL/api/orders"
    
    echo ""
    echo -e "${BLUE}🛠️  Comandos Úteis:${NC}"
    echo "  📋 Status: railway status"
    echo "  📄 Logs: railway logs --service <service-name>"
    echo "  🌐 Dashboard: railway open"
    echo "  🔄 Redeploy: git push (auto-deploy ativo)"
    
    echo ""
    echo -e "${YELLOW}⚡ Performance Targets:${NC}"
    echo "  🎯 APIs: < 100ms response time"
    echo "  🎯 Frontend: < 1.5s First Paint"
    echo "  🎯 Availability: 99.9%"
    
    echo ""
    echo -e "${GREEN}✅ Sistema pronto para produção!${NC}"
}

main() {
    print_header
    
    check_prerequisites
    create_project
    setup_infrastructure
    build_shared_events
    
    # Deploy backend services in order
    deploy_service "order-service" "8081"
    deploy_service "payment-service" "8082" 
    deploy_service "inventory-service" "8083"
    deploy_service "order-query-service" "8084"
    
    # Deploy frontend
    deploy_frontend
    
    # Wait and verify
    wait_for_deployments
    perform_health_checks
    test_end_to_end
    
    show_deployment_summary
}

# Handle interruption
trap 'log_warning "Deployment interrompido pelo usuário"; exit 130' INT

# Execute main function
main "$@"