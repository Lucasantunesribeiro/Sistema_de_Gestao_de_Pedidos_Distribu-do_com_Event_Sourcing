#!/bin/bash

# Sistema de Gestão de Pedidos Distribuído - Script de Desenvolvimento
# Localização: /mnt/d/Programacao/Sistema_de_Gestão_de_Pedidos_Distribuído_com_Event_Sourcing

PROJECT_DIR="/mnt/d/Programacao/Sistema_de_Gestão_de_Pedidos_Distribuído_com_Event_Sourcing"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Navegar para diretório do projeto
cd "$PROJECT_DIR" || { print_error "Não foi possível acessar $PROJECT_DIR"; exit 1; }

# Verificar se está no diretório correto
if [[ ! -f "docker-compose.yml" ]]; then
    print_error "docker-compose.yml não encontrado. Verifique se está no diretório correto do projeto."
    exit 1
fi

# Comandos principais
case "$1" in
    "full")
        print_status "🚀 Iniciando sistema completo..."
        
        print_status "📦 Construindo biblioteca compartilhada..."
        cd shared-events && mvn clean install -q && cd ..
        
        print_status "🔧 Iniciando infraestrutura..."
        docker-compose up -d order-db query-db rabbitmq redis
        
        print_status "⏳ Aguardando serviços ficarem prontos..."
        sleep 15
        
        print_status "🚀 Iniciando microsserviços..."
        cd services/order-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/order.log 2>&1 &
        cd ../payment-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/payment.log 2>&1 &
        cd ../inventory-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/inventory.log 2>&1 &
        cd ../order-query-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/query.log 2>&1 &
        cd ../..
        
        print_status "⚛️ Iniciando frontend..."
        cd frontend && npm run dev > /tmp/frontend.log 2>&1 &
        cd ..
        
        print_success "✅ Sistema completo iniciado!"
        print_status "📊 Aguarde 30s e execute 'pedidos health' para verificar status"
        ;;
        
    "infra")
        print_status "🔧 Iniciando apenas infraestrutura..."
        docker-compose up -d order-db query-db rabbitmq redis
        print_success "✅ Infraestrutura iniciada (PostgreSQL, RabbitMQ, Redis)"
        ;;
        
    "backend")
        print_status "📦 Construindo biblioteca compartilhada..."
        cd shared-events && mvn clean install -q && cd ..
        
        print_status "🚀 Iniciando microsserviços backend..."
        cd services/order-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/order.log 2>&1 &
        cd ../payment-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/payment.log 2>&1 &
        cd ../inventory-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/inventory.log 2>&1 &
        cd ../order-query-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/query.log 2>&1 &
        cd ../..
        print_success "✅ Microsserviços backend iniciados"
        ;;
        
    "frontend")
        print_status "⚛️ Iniciando frontend React..."
        cd frontend
        if [ ! -d "node_modules" ]; then
            print_status "📦 Instalando dependências..."
            npm install
        fi
        npm run dev &
        cd ..
        print_success "✅ Frontend iniciado em http://localhost:3000"
        ;;
        
    "health")
        print_status "🔍 Verificando saúde dos serviços..."
        
        services=("Order:8081" "Payment:8082" "Inventory:8083" "Query:8084")
        
        for service_info in "${services[@]}"; do
            IFS=':' read -r name port <<< "$service_info"
            
            if curl -s --max-time 5 http://localhost:$port/actuator/health | grep -q '"status":"UP"'; then
                print_success "✅ $name Service (port $port) - HEALTHY"
            else
                print_error "❌ $name Service (port $port) - UNHEALTHY"
            fi
        done
        
        # Verificar frontend
        if curl -s --max-time 5 http://localhost:3000 > /dev/null 2>&1; then
            print_success "✅ Frontend (port 3000) - HEALTHY"
        else
            print_error "❌ Frontend (port 3000) - UNHEALTHY"
        fi
        
        # Verificar infraestrutura
        print_status "🔧 Verificando infraestrutura..."
        
        if docker-compose exec order-db pg_isready -U postgres > /dev/null 2>&1; then
            print_success "✅ PostgreSQL - CONNECTED"
        else
            print_error "❌ PostgreSQL - DISCONNECTED"
        fi
        
        if curl -s --max-time 5 http://localhost:15672 > /dev/null 2>&1; then
            print_success "✅ RabbitMQ - AVAILABLE"
        else
            print_error "❌ RabbitMQ - UNAVAILABLE"
        fi
        
        if docker-compose exec redis redis-cli ping > /dev/null 2>&1; then
            print_success "✅ Redis - CONNECTED"
        else
            print_error "❌ Redis - DISCONNECTED"
        fi
        ;;
        
    "logs")
        service=${2:-"all"}
        if [ "$service" = "all" ]; then
            print_status "📋 Monitorando logs de todos os serviços..."
            docker-compose logs -f
        else
            print_status "📋 Monitorando logs do $service..."
            case $service in
                "order") tail -f /tmp/order.log ;;
                "payment") tail -f /tmp/payment.log ;;
                "inventory") tail -f /tmp/inventory.log ;;
                "query") tail -f /tmp/query.log ;;
                "frontend") tail -f /tmp/frontend.log ;;
                *) print_error "Serviço inválido. Use: order, payment, inventory, query, frontend, ou all" ;;
            esac
        fi
        ;;
        
    "test")
        print_status "🧪 Executando todos os testes..."
        
        # Testes backend
        for service in order-service payment-service inventory-service order-query-service; do
            print_status "Testing $service..."
            cd "services/$service"
            mvn test -q
            cd ../..
        done
        
        # Testes frontend
        print_status "Testing frontend..."
        cd frontend
        npm run test -- --run --reporter=verbose
        cd ..
        
        print_success "✅ Todos os testes executados"
        ;;
        
    "stop")
        print_status "🛑 Parando todos os serviços..."
        
        # Parar processos Java
        pkill -f "spring-boot:run" 2>/dev/null || true
        
        # Parar frontend
        pkill -f "npm run dev" 2>/dev/null || true
        
        # Parar containers
        docker-compose down
        
        # Limpar logs temporários
        rm -f /tmp/{order,payment,inventory,query,frontend}.log
        
        print_success "✅ Todos os serviços parados"
        ;;
        
    "clean")
        print_status "🧹 Limpando ambiente..."
        
        # Parar tudo
        bash "$0" stop
        
        # Limpar volumes Docker
        docker-compose down -v --remove-orphans
        
        # Limpar builds Maven
        find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
        
        # Limpar node_modules se necessário
        if [ "$2" = "full" ]; then
            rm -rf frontend/node_modules
        fi
        
        print_success "✅ Limpeza concluída"
        ;;
        
    "events")
        print_status "🐰 Verificando filas RabbitMQ..."
        if curl -s -u guest:guest http://localhost:15672/api/queues > /dev/null 2>&1; then
            curl -s -u guest:guest http://localhost:15672/api/queues | jq -r '.[] | "\(.name): \(.messages) messages"' 2>/dev/null || echo "jq não encontrado, usando curl raw:"
            curl -s -u guest:guest http://localhost:15672/api/queues
        else
            print_error "RabbitMQ não está acessível"
        fi
        ;;
        
    *)
        echo "🏗️ Sistema de Gestão de Pedidos Distribuído - Comandos de Desenvolvimento"
        echo ""
        echo "📦 INICIALIZAÇÃO:"
        echo "  $0 full        - Sistema completo (infra + backend + frontend)"
        echo "  $0 infra       - Apenas infraestrutura (DB, RabbitMQ, Redis)"
        echo "  $0 backend     - Apenas microsserviços backend"
        echo "  $0 frontend    - Apenas frontend React"
        echo ""
        echo "🔍 MONITORAMENTO:"
        echo "  $0 health      - Verificar saúde de todos os serviços"
        echo "  $0 logs [service] - Monitorar logs (all, order, payment, inventory, query, frontend)"
        echo "  $0 events      - Verificar filas RabbitMQ"
        echo ""
        echo "🧪 TESTES:"
        echo "  $0 test        - Executar todos os testes"
        echo ""
        echo "🛑 CONTROLE:"
        echo "  $0 stop        - Parar todos os serviços"
        echo "  $0 clean [full] - Limpar ambiente (full remove node_modules)"
        echo ""
        echo "🌐 INTERFACES:"
        echo "  Frontend:      http://localhost:3000"
        echo "  RabbitMQ UI:   http://localhost:15672 (guest/guest)"
        echo "  Order API:     http://localhost:8081/api/orders"
        echo "  Payment API:   http://localhost:8082/api/payments"
        echo "  Inventory API: http://localhost:8083/api/inventory"
        echo "  Query API:     http://localhost:8084/api/orders"
        ;;
esac
