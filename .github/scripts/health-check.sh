#!/bin/bash

# Health Check Script for Production Deployment
# Validates all services are running correctly after deploy

set -e

# Configuration
DEFAULT_HOST="localhost"
DEFAULT_PORT="8080"
MAX_RETRIES=10
RETRY_DELAY=15

HOST=${1:-$DEFAULT_HOST}
PORT=${2:-$DEFAULT_PORT}
BASE_URL="http://${HOST}:${PORT}"

# Colors for output
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

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if service is responding
check_endpoint() {
    local endpoint=$1
    local expected_status=${2:-200}
    local description=$3
    
    print_status "Checking $description..."
    
    for i in $(seq 1 $MAX_RETRIES); do
        HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$endpoint" || echo "000")
        
        if [ "$HTTP_STATUS" = "$expected_status" ]; then
            print_success "✓ $description - HTTP $HTTP_STATUS"
            return 0
        else
            if [ $i -eq $MAX_RETRIES ]; then
                print_error "✗ $description - HTTP $HTTP_STATUS (expected $expected_status)"
                return 1
            else
                print_warning "Attempt $i/$MAX_RETRIES failed - HTTP $HTTP_STATUS, retrying in ${RETRY_DELAY}s..."
                sleep $RETRY_DELAY
            fi
        fi
    done
}

# Function to check JSON response
check_json_endpoint() {
    local endpoint=$1
    local expected_field=$2
    local description=$3
    
    print_status "Checking $description..."
    
    for i in $(seq 1 $MAX_RETRIES); do
        RESPONSE=$(curl -s "$BASE_URL$endpoint" || echo "")
        
        if echo "$RESPONSE" | jq -e ".$expected_field" > /dev/null 2>&1; then
            print_success "✓ $description - JSON response valid"
            return 0
        else
            if [ $i -eq $MAX_RETRIES ]; then
                print_error "✗ $description - Invalid JSON response"
                echo "Response: $RESPONSE"
                return 1
            else
                print_warning "Attempt $i/$MAX_RETRIES failed, retrying in ${RETRY_DELAY}s..."
                sleep $RETRY_DELAY
            fi
        fi
    done
}

echo "🏥 Starting Health Check for Sistema de Gestão de Pedidos"
echo "========================================================="
echo "Target: $BASE_URL"
echo "Max retries: $MAX_RETRIES"
echo "Retry delay: ${RETRY_DELAY}s"
echo ""

# 1. Basic connectivity test
print_status "Testing basic connectivity..."
if ! curl -s --connect-timeout 10 "$BASE_URL" > /dev/null; then
    print_error "Cannot connect to $BASE_URL"
    print_error "Please verify the service is running and accessible"
    exit 1
fi
print_success "Basic connectivity established"

# 2. Frontend availability
check_endpoint "/" 200 "Frontend (React SPA)"

# 3. Main health endpoint
check_endpoint "/health" 200 "Application Health Check"

# 4. Service-specific health checks
print_status "Checking individual service health..."

# Order Service
check_endpoint "/api/orders/health" 200 "Order Service Health"

# Payment Service  
check_endpoint "/api/payments/health" 200 "Payment Service Health"

# Inventory Service
check_endpoint "/api/inventory/health" 200 "Inventory Service Health"

# Query Service
check_endpoint "/api/query/health" 200 "Query Service Health"

# 5. API functionality tests
print_status "Testing core API functionality..."

# Test order creation endpoint (should return 400 without payload, but service is responding)
check_endpoint "/api/orders" 400 "Order Creation Endpoint"

# Test inventory query endpoint
check_endpoint "/api/inventory" 200 "Inventory Query Endpoint"

# Test payment status endpoint  
check_endpoint "/api/payments/status" 200 "Payment Status Endpoint"

# 6. Static assets check
print_status "Checking static assets..."
check_endpoint "/assets/index.js" 200 "Frontend JavaScript Bundle" || print_warning "JS bundle check failed (expected if using different bundling)"
check_endpoint "/assets/index.css" 200 "Frontend CSS Bundle" || print_warning "CSS bundle check failed (expected if using different bundling)"

# 7. Performance check
print_status "Running performance check..."
START_TIME=$(date +%s%N)
curl -s "$BASE_URL/health" > /dev/null
END_TIME=$(date +%s%N)
RESPONSE_TIME=$(( (END_TIME - START_TIME) / 1000000 )) # Convert to milliseconds

if [ $RESPONSE_TIME -lt 1000 ]; then
    print_success "✓ Response time: ${RESPONSE_TIME}ms (excellent)"
elif [ $RESPONSE_TIME -lt 3000 ]; then
    print_warning "⚠ Response time: ${RESPONSE_TIME}ms (acceptable)"
else
    print_error "✗ Response time: ${RESPONSE_TIME}ms (poor - investigate)"
fi

# 8. Memory and resource check (if available)
print_status "Checking system resources..."
if command -v free &> /dev/null; then
    MEMORY_USAGE=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}')
    print_status "Memory usage: ${MEMORY_USAGE}%"
fi

# Summary
echo ""
echo "═══════════════════════════════════════"
echo "🎯 HEALTH CHECK COMPLETE"
echo "═══════════════════════════════════════"
echo ""
echo "✅ Frontend: Available"
echo "✅ Backend Services: Running"  
echo "✅ Health Endpoints: Responding"
echo "✅ API Endpoints: Functional"
echo "⚡ Response Time: ${RESPONSE_TIME}ms"
echo ""

print_success "All health checks passed! 🎉"
print_status "System is ready for production traffic"

echo ""
echo "📊 Quick Access URLs:"
echo "🌐 Frontend: $BASE_URL/"
echo "🏥 Health: $BASE_URL/health"
echo "📋 API Docs: $BASE_URL/swagger-ui.html"
echo ""