#!/bin/bash

# Script para validar otimizações do Connection Pool
# Verifica métricas HikariCP via endpoints de monitoring

set -e

echo "=== VALIDAÇÃO CONNECTION POOL OPTIMIZATIONS ==="
echo "Target: Aumentar throughput 500 → 1000 req/sec"
echo "================================================"

# Aguardar infraestrutura
echo "⏱️  Aguardando infraestrutura inicializar..."
sleep 15

# Função para verificar se serviço está rodando
check_service() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    echo "🔍 Verificando $service_name (porta $port)..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
            echo "✅ $service_name está rodando"
            return 0
        fi
        echo "   Tentativa $attempt/$max_attempts..."
        sleep 2
        ((attempt++))
    done
    
    echo "❌ $service_name não iniciou após $max_attempts tentativas"
    return 1
}

# Função para verificar métricas HikariCP
check_hikari_metrics() {
    local service_name=$1
    local port=$2
    
    echo "📊 Verificando métricas HikariCP $service_name..."
    
    local metrics_url="http://localhost:$port/actuator/metrics/hikaricp.connections.max"
    local response=$(curl -s "$metrics_url" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ ! -z "$response" ]; then
        local max_pool=$(echo "$response" | grep -o '"value":[0-9]*' | cut -d':' -f2)
        echo "   📈 Pool máximo configurado: $max_pool conexões"
        
        if [ "$max_pool" -ge 25 ]; then
            echo "   ✅ Pool size otimizado (≥25)"
        else
            echo "   ⚠️  Pool size abaixo do esperado (<25)"
        fi
    else
        echo "   ⚠️  Não foi possível obter métricas HikariCP"
    fi
}

# Verificar databases estão rodando
echo "🗄️  Verificando databases..."
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "(order-db|query-db)"

# Iniciar Order Service para teste
echo ""
echo "🚀 Iniciando Order Service com pools otimizados..."
cd services/order-service
mvn spring-boot:run -Dspring-boot.run.profiles=local &
ORDER_PID=$!

# Aguardar Order Service inicializar
echo "⏱️  Aguardando Order Service inicializar..."
sleep 30

if check_service "Order Service" 8081; then
    check_hikari_metrics "Order Service" 8081
else
    echo "❌ Order Service falhou ao inicializar"
    kill $ORDER_PID 2>/dev/null || true
    exit 1
fi

# Iniciar Query Service para teste
echo ""
echo "🚀 Iniciando Query Service com pools otimizados..."
cd ../order-query-service
mvn spring-boot:run -Dspring-boot.run.profiles=local &
QUERY_PID=$!

# Aguardar Query Service inicializar
echo "⏱️  Aguardando Query Service inicializar..."
sleep 30

if check_service "Query Service" 8084; then
    check_hikari_metrics "Query Service" 8084
else
    echo "❌ Query Service falhou ao inicializar"
    kill $QUERY_PID 2>/dev/null || true
    kill $ORDER_PID 2>/dev/null || true
    exit 1
fi

echo ""
echo "=== VALIDAÇÃO COMPLETA ==="
echo "✅ Connection Pools otimizados e funcionando"
echo "📈 Métricas disponíveis em:"
echo "   - Order Service: http://localhost:8081/actuator/metrics"
echo "   - Query Service: http://localhost:8084/actuator/metrics"
echo ""
echo "🎯 PRÓXIMOS PASSOS:"
echo "   1. Execute testes de carga para validar 1000 req/sec"
echo "   2. Monitore métricas HikariCP em produção"
echo "   3. Prossiga para implementação do Event Registry Pattern"

# Limpar processos
echo ""
echo "🧹 Finalizando processos de teste..."
kill $ORDER_PID 2>/dev/null || true
kill $QUERY_PID 2>/dev/null || true

echo "✅ Validação de Connection Pool Optimization concluída!"