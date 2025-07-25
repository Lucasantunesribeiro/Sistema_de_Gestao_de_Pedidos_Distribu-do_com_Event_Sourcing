#!/bin/bash

# Test Local Deployment Script
# Validates Docker Compose deployment and service health

set -e

echo "🚀 Starting Local Deployment Test..."
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
TIMEOUT=300
HEALTH_CHECK_INTERVAL=5
MAX_RETRIES=60

# Service endpoints
declare -A SERVICES=(
    ["order-service"]="http://localhost:8081/api/orders/actuator/health"
    ["payment-service"]="http://localhost:8082/api/payments/actuator/health"
    ["inventory-service"]="http://localhost:8083/api/inventory/actuator/health"
    ["order-query-service"]="http://localhost:8084/api/orders/actuator/health"
)

# Infrastructure endpoints
declare -A INFRASTRUCTURE=(
    ["order-db"]="localhost:5432"
    ["query-db"]="localhost:5433"
    ["rabbitmq-amqp"]="localhost:5672"
    ["rabbitmq-management"]="http://localhost:15672"
)

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

cleanup() {
    log_info "Cleaning up previous deployment..."
    docker-compose down -v --remove-orphans 2>/dev/null || true
    docker system prune -f 2>/dev/null || true
}

start_services() {
    log_info "Starting services with Docker Compose..."
    docker-compose up --build -d
    
    if [ $? -eq 0 ]; then
        log_success "Docker Compose started successfully"
    else
        log_error "Failed to start Docker Compose"
        exit 1
    fi
}

wait_for_infrastructure() {
    log_info "Waiting for infrastructure services..."
    
    for service in "${!INFRASTRUCTURE[@]}"; do
        endpoint="${INFRASTRUCTURE[$service]}"
        log_info "Checking $service at $endpoint..."
        
        retries=0
        while [ $retries -lt $MAX_RETRIES ]; do
            if [[ $endpoint == http* ]]; then
                # HTTP endpoint
                if curl -f -s "$endpoint" > /dev/null 2>&1; then
                    log_success "$service is ready"
                    break
                fi
            else
                # TCP endpoint
                host=$(echo $endpoint | cut -d: -f1)
                port=$(echo $endpoint | cut -d: -f2)
                if nc -z "$host" "$port" 2>/dev/null; then
                    log_success "$service is ready"
                    break
                fi
            fi
            
            retries=$((retries + 1))
            if [ $retries -eq $MAX_RETRIES ]; then
                log_error "$service failed to start within timeout"
                return 1
            fi
            
            sleep $HEALTH_CHECK_INTERVAL
        done
    done
}

wait_for_services() {
    log_info "Waiting for application services..."
    
    for service in "${!SERVICES[@]}"; do
        endpoint="${SERVICES[$service]}"
        log_info "Checking $service health at $endpoint..."
        
        retries=0
        while [ $retries -lt $MAX_RETRIES ]; do
            if curl -f -s "$endpoint" | grep -q '"status":"UP"'; then
                log_success "$service is healthy"
                break
            fi
            
            retries=$((retries + 1))
            if [ $retries -eq $MAX_RETRIES ]; then
                log_error "$service failed to become healthy within timeout"
                return 1
            fi
            
            sleep $HEALTH_CHECK_INTERVAL
        done
    done
}

test_service_connectivity() {
    log_info "Testing service connectivity..."
    
    # Test database connections
    log_info "Testing database connections..."
    docker exec order-db pg_isready -U postgres -d order_db
    docker exec query-db pg_isready -U postgres -d order_query_db
    
    # Test RabbitMQ
    log_info "Testing RabbitMQ management interface..."
    curl -f -s "http://guest:guest@localhost:15672/api/overview" > /dev/null
    
    log_success "All connectivity tests passed"
}

run_integration_tests() {
    log_info "Running integration tests..."
    
    # Test order creation flow
    log_info "Testing order creation flow..."
    
    ORDER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/orders \
        -H "Content-Type: application/json" \
        -d '{
            "customerId": "test-customer-001",
            "items": [
                {
                    "productId": "test-product-001",
                    "productName": "Test Product",
                    "quantity": 2,
                    "price": 29.99
                }
            ]
        }')
    
    if echo "$ORDER_RESPONSE" | grep -q "orderId"; then
        ORDER_ID=$(echo "$ORDER_RESPONSE" | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
        log_success "Order created successfully: $ORDER_ID"
        
        # Wait for event processing
        sleep 5
        
        # Test query service
        log_info "Testing query service..."
        QUERY_RESPONSE=$(curl -s "http://localhost:8084/api/orders/$ORDER_ID")
        
        if echo "$QUERY_RESPONSE" | grep -q "$ORDER_ID"; then
            log_success "Query service returned order data"
        else
            log_warning "Query service did not return expected order data"
        fi
    else
        log_error "Failed to create order"
        return 1
    fi
}

generate_report() {
    log_info "Generating deployment test report..."
    
    echo ""
    echo "📊 DEPLOYMENT TEST REPORT"
    echo "========================="
    echo "Timestamp: $(date)"
    echo ""
    
    echo "🏗️  Infrastructure Status:"
    for service in "${!INFRASTRUCTURE[@]}"; do
        endpoint="${INFRASTRUCTURE[$service]}"
        if [[ $endpoint == http* ]]; then
            if curl -f -s "$endpoint" > /dev/null 2>&1; then
                echo "  ✅ $service: HEALTHY"
            else
                echo "  ❌ $service: UNHEALTHY"
            fi
        else
            host=$(echo $endpoint | cut -d: -f1)
            port=$(echo $endpoint | cut -d: -f2)
            if nc -z "$host" "$port" 2>/dev/null; then
                echo "  ✅ $service: HEALTHY"
            else
                echo "  ❌ $service: UNHEALTHY"
            fi
        fi
    done
    
    echo ""
    echo "🚀 Application Services Status:"
    for service in "${!SERVICES[@]}"; do
        endpoint="${SERVICES[$service]}"
        if curl -f -s "$endpoint" | grep -q '"status":"UP"'; then
            echo "  ✅ $service: HEALTHY"
        else
            echo "  ❌ $service: UNHEALTHY"
        fi
    done
    
    echo ""
    echo "🐳 Docker Containers:"
    docker-compose ps
    
    echo ""
    echo "📈 Resource Usage:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"
}

main() {
    log_info "Local Deployment Test Started"
    
    # Cleanup previous deployment
    cleanup
    
    # Start services
    start_services
    
    # Wait for infrastructure
    if ! wait_for_infrastructure; then
        log_error "Infrastructure services failed to start"
        exit 1
    fi
    
    # Wait for application services
    if ! wait_for_services; then
        log_error "Application services failed to start"
        exit 1
    fi
    
    # Test connectivity
    if ! test_service_connectivity; then
        log_error "Connectivity tests failed"
        exit 1
    fi
    
    # Run integration tests
    if ! run_integration_tests; then
        log_error "Integration tests failed"
        exit 1
    fi
    
    # Generate report
    generate_report
    
    log_success "🎉 Local deployment test completed successfully!"
    echo ""
    echo "Services are running and ready for development."
    echo "Access the services at:"
    echo "  - Order Service: http://localhost:8081/api/orders/actuator/health"
    echo "  - Payment Service: http://localhost:8082/api/payments/actuator/health"
    echo "  - Inventory Service: http://localhost:8083/api/inventory/actuator/health"
    echo "  - Query Service: http://localhost:8084/api/orders/actuator/health"
    echo "  - RabbitMQ Management: http://localhost:15672 (guest/guest)"
    echo ""
    echo "To stop services: docker-compose down"
}

# Handle script interruption
trap 'log_warning "Test interrupted by user"; exit 130' INT

# Run main function
main "$@"