#!/bin/bash
# Health check para todos os serviços

services=(
    "Order Service:http://localhost:8081/api/orders/health"
    "Payment Service:http://localhost:8082/api/payments/health" 
    "Inventory Service:http://localhost:8083/api/inventory/health"
    "Query Service:http://localhost:8084/api/orders/health"
    "Frontend:http://localhost:3000"
)

echo "🏥 Health Check dos Microsserviços"
echo "=================================="

for service in "${services[@]}"; do
    name="${service%%:*}"
    url="${service##*:}"
    
    if curl -sf "$url" > /dev/null 2>&1; then
        echo "✅ $name - OK"
    else
        echo "❌ $name - FAILED"
    fi
done

echo ""
echo "🗄️  Database Status:"
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "(postgres|redis|rabbitmq)"
