#!/usr/bin/env bash
set -euo pipefail

# Script de inicialização para Sistema de Gestão de Pedidos Distribuído
# Inicia todos os microsserviços usando supervisord
# Produzido por Claude Code para deploy no Render

echo "🚀 Starting Distributed Order Management System..."

# Debug: Verificar arquivos e diretórios
echo "📁 Current directory: $(pwd)"
echo "📄 Available files:"
ls -la /app/

# Create log directories  
mkdir -p /var/log/supervisor
mkdir -p /var/log

# Set environment variables for all services
export SPRING_PROFILES_ACTIVE=render
export DATABASE_URL=jdbc:h2:mem:orderdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
export DATABASE_USERNAME=sa
export DATABASE_PASSWORD=
export DATABASE_DRIVER=org.h2.Driver
export HIBERNATE_DIALECT=org.hibernate.dialect.H2Dialect
export JWT_SECRET_KEY=renderSecretKeyForProduction2024
export JAVA_OPTS="-Xmx150m -XX:+UseContainerSupport"

# Função para aguardar serviços estarem prontos
wait_for_service() {
    local port=$1
    local service_name=$2
    echo "⏳ Aguardando $service_name na porta $port..."
    
    for i in $(seq 1 60); do
        if nc -z localhost "$port" 2>/dev/null; then
            echo "✅ $service_name está pronto na porta $port"
            return 0
        fi
        sleep 2
    done
    
    echo "❌ $service_name falhou ao iniciar na porta $port"
    return 1
}

# Verificar se arquivos JAR existem
echo "🔍 Verificando arquivos JAR..."
for jar in order-service.jar payment-service.jar inventory-service.jar query-service.jar; do
    if [[ -f "/app/$jar" ]]; then
        echo "✅ $jar encontrado"
    else
        echo "❌ $jar NÃO encontrado"
        exit 1
    fi
done

# Start all services with supervisor
echo "🔄 Iniciando todos os serviços com supervisord..."
echo "📋 Configuração: /etc/supervisor/conf.d/supervisord.conf"

# Debug: Mostrar variáveis de ambiente importantes
echo "🔍 Environment Debug:"
echo "PORT=${PORT:-'not set'}"
echo "DATABASE_URL=${DATABASE_URL:-'not set'}"
echo "JAVA_OPTS=${JAVA_OPTS:-'not set'}"

# Test port availability
if [[ -n "${PORT:-}" ]]; then
    echo "🔍 Testando disponibilidade da porta $PORT..."
    if nc -z localhost "$PORT" 2>/dev/null; then
        echo "⚠️ AVISO: Porta $PORT já está em uso!"
    else
        echo "✅ Porta $PORT disponível"
    fi
else
    echo "⚠️ AVISO: Variável PORT não está definida, usando fallback 8080"
    export PORT=8080
fi

# Usar exec para que supervisord seja PID 1
echo "🚀 Iniciando supervisord com loglevel=debug..."
exec /usr/bin/supervisord -n -c /etc/supervisor/conf.d/supervisord.conf