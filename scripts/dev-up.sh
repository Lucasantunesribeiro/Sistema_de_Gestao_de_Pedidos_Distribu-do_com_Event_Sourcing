#!/bin/bash

# Sistema de Gestão de Pedidos - Development Setup Script
# Inicializa o ambiente completo de desenvolvimento

set -e

echo "🚀 Sistema de Gestão de Pedidos - Iniciando Ambiente de Desenvolvimento"
echo "=================================================================="

# Check required tools
echo "📋 Verificando dependências..."

check_command() {
    if ! command -v $1 &> /dev/null; then
        echo "❌ $1 não está instalado. Instale-o primeiro."
        exit 1
    fi
    echo "✅ $1 encontrado"
}

check_command docker
check_command docker-compose
check_command mvn
check_command node
check_command npm

# Set environment variables
export MESSAGING_TYPE=${MESSAGING_TYPE:-redis}
export ENVIRONMENT=docker

echo ""
echo "🔧 Configuração do ambiente:"
echo "   - Messaging: $MESSAGING_TYPE"
echo "   - Environment: $ENVIRONMENT"
echo "   - Profile: dev"
echo ""

# Clean previous containers (optional)
read -p "🧹 Limpar containers existentes? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🧹 Limpando containers anteriores..."
    docker-compose --profile dev down -v --remove-orphans || true
    docker system prune -f || true
fi

# Build shared events first
echo "📦 Construindo biblioteca de eventos compartilhados..."
cd shared-events
mvn clean install -DskipTests -q
cd ..

# Start infrastructure services
echo "🏗️  Iniciando serviços de infraestrutura..."
docker-compose --profile dev up -d order-db query-db redis

if [ "$MESSAGING_TYPE" = "rabbitmq" ]; then
    echo "🐰 Iniciando RabbitMQ..."
    docker-compose --profile dev up -d rabbitmq
fi

# Wait for services to be ready
echo "⏳ Aguardando serviços ficarem prontos..."

wait_for_service() {
    local service=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    echo "   Aguardando $service (porta $port)..."
    
    while ! nc -z localhost $port; do
        if [ $attempt -eq $max_attempts ]; then
            echo "❌ Timeout esperando $service"
            exit 1
        fi
        sleep 2
        attempt=$((attempt + 1))
    done
    echo "   ✅ $service está pronto"
}

wait_for_service "PostgreSQL Order DB" 5432
wait_for_service "PostgreSQL Query DB" 5433
wait_for_service "Redis" 6379

if [ "$MESSAGING_TYPE" = "rabbitmq" ]; then
    wait_for_service "RabbitMQ" 5672
fi

# Start backend services
echo "🎯 Iniciando serviços backend..."
docker-compose --profile dev up -d order-service payment-service inventory-service order-query-service

# Wait for backend services
echo "⏳ Aguardando serviços backend..."
sleep 30

for service in 8081 8082 8083 8084; do
    wait_for_service "Backend service" $service
done

# Start frontend
echo "🖥️  Iniciando frontend..."
docker-compose --profile dev up -d frontend

# Wait for frontend
wait_for_service "Frontend" 3000

echo ""
echo "🎉 Ambiente de desenvolvimento iniciado com sucesso!"
echo "=================================================================="
echo "📍 Serviços disponíveis:"
echo "   • Frontend:          http://localhost:3000"
echo "   • Order Service:     http://localhost:8081"
echo "   • Payment Service:   http://localhost:8082"
echo "   • Inventory Service: http://localhost:8083"
echo "   • Query Service:     http://localhost:8084"
echo ""
echo "🗄️  Infraestrutura:"
echo "   • PostgreSQL (Order): localhost:5432"
echo "   • PostgreSQL (Query): localhost:5433"
echo "   • Redis:             localhost:6379"
if [ "$MESSAGING_TYPE" = "rabbitmq" ]; then
echo "   • RabbitMQ:          localhost:5672"
echo "   • RabbitMQ UI:       http://localhost:15672 (guest/guest)"
fi
echo ""
echo "📊 Comandos úteis:"
echo "   • Ver logs:          docker-compose --profile dev logs -f"
echo "   • Ver status:        docker-compose --profile dev ps"
echo "   • Parar serviços:    ./scripts/dev-down.sh"
echo ""
echo "✨ Pronto para desenvolvimento!"