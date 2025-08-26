#!/bin/bash

# Sistema de Gestão de Pedidos - Development Shutdown Script
# Para o ambiente completo de desenvolvimento

set -e

echo "🛑 Sistema de Gestão de Pedidos - Parando Ambiente de Desenvolvimento"
echo "=================================================================="

# Check if services are running
if ! docker-compose --profile dev ps | grep -q "Up"; then
    echo "ℹ️  Nenhum serviço está executando."
    exit 0
fi

echo "📊 Status atual dos serviços:"
docker-compose --profile dev ps

echo ""
read -p "🔄 Parar todos os serviços? (y/n): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "⏹️  Parando serviços..."
    
    # Stop services gracefully in reverse dependency order
    echo "   Parando frontend..."
    docker-compose --profile dev stop frontend || true
    
    echo "   Parando serviços backend..."
    docker-compose --profile dev stop order-query-service || true
    docker-compose --profile dev stop inventory-service || true
    docker-compose --profile dev stop payment-service || true
    docker-compose --profile dev stop order-service || true
    
    echo "   Parando infraestrutura..."
    docker-compose --profile dev stop redis || true
    docker-compose --profile dev stop rabbitmq || true
    docker-compose --profile dev stop query-db || true
    docker-compose --profile dev stop order-db || true
    
    echo "🧹 Removendo containers..."
    docker-compose --profile dev rm -f
    
    echo ""
    read -p "🗑️  Remover volumes de dados? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "🗑️  Removendo volumes..."
        docker-compose --profile dev down -v || true
        echo "   ✅ Volumes removidos"
    fi
    
    echo ""
    read -p "🧹 Limpar imagens não utilizadas? (y/n): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "🧹 Limpando imagens..."
        docker image prune -f
        echo "   ✅ Imagens limpas"
    fi
    
    echo ""
    echo "✅ Ambiente de desenvolvimento parado com sucesso!"
    echo "=================================================================="
    echo "💡 Para reiniciar:"
    echo "   ./scripts/dev-up.sh"
    echo ""
    echo "📊 Status final:"
    docker-compose --profile dev ps || true
    
else
    echo "❌ Operação cancelada."
fi