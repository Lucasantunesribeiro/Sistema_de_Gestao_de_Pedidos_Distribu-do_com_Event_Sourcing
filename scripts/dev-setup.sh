#!/bin/bash
# Setup completo para desenvolvimento

echo "🚀 Iniciando ambiente de desenvolvimento..."

# Verifica infraestrutura
if ! docker ps | grep -q postgres; then
    echo "🔧 Iniciando infraestrutura..."
    docker-compose up -d order-db query-db rabbitmq redis
    echo "⏳ Aguardando infraestrutura (15s)..."
    sleep 15
fi

# Build shared-events
echo "📦 Building shared-events..."
cd shared-events && mvn clean install -q && cd ..

echo "✅ Ambiente pronto!"
echo "💡 Use 'pedidos backend' para iniciar microsserviços"
echo "💡 Use 'pedidos frontend' para iniciar React"
