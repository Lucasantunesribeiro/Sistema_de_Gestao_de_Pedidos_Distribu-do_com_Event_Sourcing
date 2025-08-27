#!/bin/bash

# ============================================================================
# SCRIPT DE TESTE - DOCKERFILE MULTI-SERVICE
# Testa build e deploy com diferentes SERVICE_TYPEs
# ============================================================================

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função de log com cores
log() {
    local color=$1
    local message=$2
    echo -e "${color}[$(date +'%H:%M:%S')] ${message}${NC}"
}

log $BLUE "=========================================="
log $BLUE "   DOCKERFILE MULTI-SERVICE - TESTE"
log $BLUE "=========================================="

# Build da imagem base
log $YELLOW "1. Building Docker image..."
docker build -t distributed-order-system:latest .

if [ $? -eq 0 ]; then
    log $GREEN "✓ Docker build successful!"
else
    log $RED "✗ Docker build failed!"
    exit 1
fi

# Função para testar um serviço específico
test_service() {
    local service_type=$1
    local port=$2
    local container_name="test-${service_type}-service"
    
    log $BLUE "=========================================="
    log $BLUE "Testing SERVICE_TYPE=${service_type}"
    log $BLUE "=========================================="
    
    # Remove container existente se houver
    docker rm -f $container_name 2>/dev/null || true
    
    # Start container
    log $YELLOW "Starting ${service_type} service on port ${port}..."
    docker run -d \
        --name $container_name \
        -e SERVICE_TYPE=$service_type \
        -e PORT=$port \
        -p $port:$port \
        distributed-order-system:latest
    
    # Wait for startup
    log $YELLOW "Waiting for service to start..."
    sleep 10
    
    # Check container status
    if docker ps | grep -q $container_name; then
        log $GREEN "✓ Container is running"
        
        # Check logs
        log $YELLOW "Container logs (last 20 lines):"
        docker logs --tail 20 $container_name
        
        # Test health endpoint if web service
        if [ "$service_type" = "web" ]; then
            log $YELLOW "Testing health endpoint..."
            if curl -s -f http://localhost:$port/health > /dev/null; then
                log $GREEN "✓ Health endpoint responding"
            else
                log $RED "✗ Health endpoint not responding"
            fi
        fi
    else
        log $RED "✗ Container failed to start"
        log $RED "Container logs:"
        docker logs $container_name
    fi
    
    # Cleanup
    log $YELLOW "Cleaning up container..."
    docker rm -f $container_name
    
    echo ""
}

# Test each service type
test_service "web" "8080"
test_service "order" "8081"  
test_service "payment" "8082"
test_service "inventory" "8083"

# Test invalid service type
log $BLUE "=========================================="
log $BLUE "Testing invalid SERVICE_TYPE"
log $BLUE "=========================================="

docker run --rm \
    -e SERVICE_TYPE=invalid \
    distributed-order-system:latest || log $GREEN "✓ Invalid service type correctly rejected"

log $BLUE "=========================================="
log $GREEN "   ALL TESTS COMPLETED!"
log $BLUE "=========================================="

# Show image size
log $YELLOW "Final image size:"
docker images distributed-order-system:latest --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"

# Cleanup instructions
log $BLUE "To clean up the test image:"
log $YELLOW "docker rmi distributed-order-system:latest"