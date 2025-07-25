#!/bin/bash

# Quick Health Check Script
# Performs rapid health validation of all services

set -e

echo "🏥 Quick Health Check"
echo "===================="

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Service endpoints
SERVICES=(
    "order-service:http://localhost:8081/api/orders/actuator/health"
    "payment-service:http://localhost:8082/api/payments/actuator/health"
    "inventory-service:http://localhost:8083/api/inventory/actuator/health"
    "order-query-service:http://localhost:8084/api/orders/actuator/health"
)

# Infrastructure
INFRASTRUCTURE=(
    "order-db:localhost:5432"
    "query-db:localhost:5433"
    "rabbitmq:localhost:5672"
    "rabbitmq-mgmt:http://localhost:15672"
)

check_http_service() {
    local name=$1
    local url=$2
    
    if curl -f -s --max-time 5 "$url" | grep -q '"status":"UP"'; then
        echo -e "  ✅ ${GREEN}$name${NC}: HEALTHY"
        return 0
    else
        echo -e "  ❌ ${RED}$name${NC}: UNHEALTHY"
        return 1
    fi
}

check_tcp_service() {
    local name=$1
    local host=$2
    local port=$3
    
    if nc -z -w5 "$host" "$port" 2>/dev/null; then
        echo -e "  ✅ ${GREEN}$name${NC}: REACHABLE"
        return 0
    else
        echo -e "  ❌ ${RED}$name${NC}: UNREACHABLE"
        return 1
    fi
}

check_http_endpoint() {
    local name=$1
    local url=$2
    
    if curl -f -s --max-time 5 "$url" > /dev/null 2>&1; then
        echo -e "  ✅ ${GREEN}$name${NC}: REACHABLE"
        return 0
    else
        echo -e "  ❌ ${RED}$name${NC}: UNREACHABLE"
        return 1
    fi
}

echo "🏗️  Infrastructure Services:"
healthy_infra=0
total_infra=0

for service_info in "${INFRASTRUCTURE[@]}"; do
    name=$(echo "$service_info" | cut -d: -f1)
    endpoint=$(echo "$service_info" | cut -d: -f2-)
    total_infra=$((total_infra + 1))
    
    if [[ $endpoint == http* ]]; then
        if check_http_endpoint "$name" "$endpoint"; then
            healthy_infra=$((healthy_infra + 1))
        fi
    else
        host=$(echo "$endpoint" | cut -d: -f1)
        port=$(echo "$endpoint" | cut -d: -f2)
        if check_tcp_service "$name" "$host" "$port"; then
            healthy_infra=$((healthy_infra + 1))
        fi
    fi
done

echo ""
echo "🚀 Application Services:"
healthy_services=0
total_services=0

for service_info in "${SERVICES[@]}"; do
    name=$(echo "$service_info" | cut -d: -f1)
    url=$(echo "$service_info" | cut -d: -f2-)
    total_services=$((total_services + 1))
    
    if check_http_service "$name" "$url"; then
        healthy_services=$((healthy_services + 1))
    fi
done

echo ""
echo "📊 Summary:"
echo "  Infrastructure: $healthy_infra/$total_infra healthy"
echo "  Services: $healthy_services/$total_services healthy"

if [ $healthy_infra -eq $total_infra ] && [ $healthy_services -eq $total_services ]; then
    echo -e "  ${GREEN}✅ All systems operational${NC}"
    exit 0
else
    echo -e "  ${RED}❌ Some systems are unhealthy${NC}"
    exit 1
fi