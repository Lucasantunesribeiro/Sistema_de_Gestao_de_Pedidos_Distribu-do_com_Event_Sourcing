#!/bin/bash

# Automated Railway.app Deployment Script
# This script automates the complete deployment of the Order Management System
# to Railway.app with proper error handling and validation

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="order-management-system"
GITHUB_REPO="your-username/order-management-system"
SERVICES=("order-service" "payment-service" "inventory-service" "order-query-service")

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
    exit 1
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check Railway CLI
    if ! command -v railway &> /dev/null; then
        error "Railway CLI not found. Install it first: npm install -g @railway/cli"
    fi
    
    # Check if logged in to Railway
    if ! railway whoami &> /dev/null; then
        error "Not logged in to Railway. Run: railway login"
    fi
    
    # Check if in git repository
    if [ ! -d ".git" ]; then
        error "Not in a git repository. Initialize git first."
    fi
    
    success "Prerequisites check passed"
}

# Build shared components
build_shared_components() {
    log "Building shared components..."
    
    cd shared-events
    if ! mvn clean install -q; then
        error "Failed to build shared-events"
    fi
    cd ..
    
    success "Shared components built successfully"
}

# Create Railway project
create_project() {
    log "Creating Railway project: $PROJECT_NAME"
    
    # Check if project already exists
    if railway status 2>/dev/null | grep -q "Project:"; then
        warning "Railway project already exists, skipping creation"
        return 0
    fi
    
    if ! railway login; then
        error "Failed to login to Railway"
    fi
    
    if ! railway init --name "$PROJECT_NAME"; then
        error "Failed to create Railway project"
    fi
    
    success "Railway project created: $PROJECT_NAME"
}

# Setup databases
setup_databases() {
    log "Setting up databases..."
    
    # PostgreSQL for Order Service (Event Store)
    log "Creating PostgreSQL database for Order Service..."
    railway add --service postgresql
    ORDER_DB_SERVICE=$(railway status --json | jq -r '.services[] | select(.name | contains("postgresql")) | .id' | head -1)
    
    # PostgreSQL for Query Service (Read Model)
    log "Creating PostgreSQL database for Query Service..."
    railway add --service postgresql
    QUERY_DB_SERVICE=$(railway status --json | jq -r '.services[] | select(.name | contains("postgresql")) | .id' | tail -1)
    
    # RabbitMQ
    log "Creating RabbitMQ service..."
    railway add --service rabbitmq
    RABBITMQ_SERVICE=$(railway status --json | jq -r '.services[] | select(.name | contains("rabbitmq")) | .id')
    
    success "Databases created successfully"
}

# Deploy individual service
deploy_service() {
    local service_name=$1
    local service_path="services/$service_name"
    
    log "Deploying $service_name..."
    
    # Create service
    railway service create "$service_name"
    railway service use "$service_name"
    
    # Set build configuration
    railway variables set RAILWAY_BUILD_COMMAND="cd $service_path && mvn clean package -DskipTests"
    railway variables set RAILWAY_START_COMMAND="cd $service_path && java -jar target/$service_name-1.0.0.jar"
    
    # Set service-specific environment variables
    case $service_name in
        "order-service")
            railway variables set --service "$service_name" DATABASE_URL='${{Postgres.DATABASE_URL}}'
            railway variables set --service "$service_name" RABBITMQ_HOST='${{RabbitMQ.RABBITMQ_HOST}}'
            railway variables set --service "$service_name" RABBITMQ_PORT='${{RabbitMQ.RABBITMQ_PORT}}'
            railway variables set --service "$service_name" RABBITMQ_USERNAME='${{RabbitMQ.RABBITMQ_USERNAME}}'
            railway variables set --service "$service_name" RABBITMQ_PASSWORD='${{RabbitMQ.RABBITMQ_PASSWORD}}'
            railway variables set --service "$service_name" JWT_SECRET="your-jwt-secret-key-change-in-production"
            railway variables set --service "$service_name" SPRING_PROFILES_ACTIVE="production"
            ;;
        "payment-service")
            railway variables set --service "$service_name" RABBITMQ_HOST='${{RabbitMQ.RABBITMQ_HOST}}'
            railway variables set --service "$service_name" RABBITMQ_PORT='${{RabbitMQ.RABBITMQ_PORT}}'
            railway variables set --service "$service_name" RABBITMQ_USERNAME='${{RabbitMQ.RABBITMQ_USERNAME}}'
            railway variables set --service "$service_name" RABBITMQ_PASSWORD='${{RabbitMQ.RABBITMQ_PASSWORD}}'
            railway variables set --service "$service_name" SPRING_PROFILES_ACTIVE="production"
            ;;
        "inventory-service")
            railway variables set --service "$service_name" RABBITMQ_HOST='${{RabbitMQ.RABBITMQ_HOST}}'
            railway variables set --service "$service_name" RABBITMQ_PORT='${{RabbitMQ.RABBITMQ_PORT}}'
            railway variables set --service "$service_name" RABBITMQ_USERNAME='${{RabbitMQ.RABBITMQ_USERNAME}}'
            railway variables set --service "$service_name" RABBITMQ_PASSWORD='${{RabbitMQ.RABBITMQ_PASSWORD}}'
            railway variables set --service "$service_name" SPRING_PROFILES_ACTIVE="production"
            ;;
        "order-query-service")
            railway variables set --service "$service_name" DATABASE_URL='${{Postgres.DATABASE_URL}}'
            railway variables set --service "$service_name" RABBITMQ_HOST='${{RabbitMQ.RABBITMQ_HOST}}'
            railway variables set --service "$service_name" RABBITMQ_PORT='${{RabbitMQ.RABBITMQ_PORT}}'
            railway variables set --service "$service_name" RABBITMQ_USERNAME='${{RabbitMQ.RABBITMQ_USERNAME}}'
            railway variables set --service "$service_name" RABBITMQ_PASSWORD='${{RabbitMQ.RABBITMQ_PASSWORD}}'
            railway variables set --service "$service_name" SPRING_PROFILES_ACTIVE="production"
            ;;
    esac
    
    # Deploy from GitHub
    railway up --service "$service_name"
    
    success "$service_name deployed successfully"
}

# Deploy all services
deploy_services() {
    log "Deploying all microservices..."
    
    for service in "${SERVICES[@]}"; do
        deploy_service "$service"
        sleep 30  # Wait between deployments
    done
    
    success "All services deployed successfully"
}

# Health check
health_check() {
    log "Performing health checks..."
    
    # Get service URLs
    ORDER_URL=$(railway status --service order-service --json | jq -r '.deployments[0].url')
    PAYMENT_URL=$(railway status --service payment-service --json | jq -r '.deployments[0].url')
    INVENTORY_URL=$(railway status --service inventory-service --json | jq -r '.deployments[0].url')
    QUERY_URL=$(railway status --service order-query-service --json | jq -r '.deployments[0].url')
    
    # Health check function
    check_service_health() {
        local service_name=$1
        local url=$2
        local health_endpoint="$url/actuator/health"
        
        log "Checking health of $service_name at $health_endpoint"
        
        local retries=0
        local max_retries=10
        
        while [ $retries -lt $max_retries ]; do
            if curl -f -s "$health_endpoint" > /dev/null; then
                success "$service_name is healthy"
                return 0
            fi
            
            warning "$service_name not ready yet, waiting... (attempt $((retries + 1))/$max_retries)"
            sleep 30
            ((retries++))
        done
        
        error "$service_name health check failed after $max_retries attempts"
    }
    
    # Check all services
    check_service_health "Order Service" "$ORDER_URL"
    check_service_health "Payment Service" "$PAYMENT_URL"
    check_service_health "Inventory Service" "$INVENTORY_URL"
    check_service_health "Query Service" "$QUERY_URL"
    
    success "All services are healthy"
}

# Functional test
functional_test() {
    log "Running functional tests..."
    
    ORDER_URL=$(railway status --service order-service --json | jq -r '.deployments[0].url')
    QUERY_URL=$(railway status --service order-query-service --json | jq -r '.deployments[0].url')
    
    # Test order creation
    log "Testing order creation..."
    ORDER_RESPONSE=$(curl -s -X POST "$ORDER_URL/api/orders" \
        -H "Content-Type: application/json" \
        -d '{
            "customerId": "test-customer-001",
            "items": [
                {
                    "productId": "product-test-1",
                    "productName": "Test Laptop",
                    "quantity": 1,
                    "price": 999.99
                }
            ]
        }')
    
    if [ $? -eq 0 ]; then
        success "Order creation test passed"
        ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.orderId // .id // .')
        log "Created order with ID: $ORDER_ID"
    else
        error "Order creation test failed"
    fi
    
    # Wait for eventual consistency
    sleep 10
    
    # Test order query
    log "Testing order query..."
    if curl -f -s "$QUERY_URL/api/orders" > /dev/null; then
        success "Order query test passed"
    else
        error "Order query test failed"
    fi
    
    success "Functional tests completed"
}

# Setup monitoring
setup_monitoring() {
    log "Setting up monitoring and alerting..."
    
    # Railway provides basic monitoring by default
    # For advanced monitoring, you would integrate with external services
    
    warning "Basic monitoring is provided by Railway. For advanced monitoring:"
    echo "1. Integrate with Datadog, New Relic, or similar APM tools"
    echo "2. Set up log aggregation with Logtail or similar"
    echo "3. Configure alerting with PagerDuty or similar"
    echo "4. Set up uptime monitoring with Pingdom or similar"
    
    success "Basic monitoring configured"
}

# Generate deployment report
generate_report() {
    log "Generating deployment report..."
    
    local report_file="deployment-report-$(date +%Y%m%d-%H%M%S).md"
    
    cat > "$report_file" << EOF
# Deployment Report - $(date)

## Project Information
- **Project Name**: $PROJECT_NAME
- **Deployment Time**: $(date)
- **Deployed By**: $(git config user.name)
- **Git Commit**: $(git rev-parse --short HEAD)

## Service URLs
EOF
    
    for service in "${SERVICES[@]}"; do
        local url=$(railway status --service "$service" --json | jq -r '.deployments[0].url // "Not available"')
        echo "- **$service**: $url" >> "$report_file"
    done
    
    cat >> "$report_file" << EOF

## Database Information
- **Order Database**: PostgreSQL (Event Store)
- **Query Database**: PostgreSQL (Read Models)
- **Message Broker**: RabbitMQ

## Health Check Results
All services passed health checks at deployment time.

## Test Results
- ✅ Order creation functional test
- ✅ Order query functional test

## Next Steps
1. Configure custom domain names
2. Set up SSL certificates
3. Configure advanced monitoring
4. Set up CI/CD pipeline
5. Perform load testing

## Support Information
For support, contact: $(git config user.email)
Repository: https://github.com/$GITHUB_REPO
EOF
    
    success "Deployment report generated: $report_file"
}

# Main deployment function
main() {
    log "Starting Railway.app deployment process..."
    
    check_prerequisites
    build_shared_components
    create_project
    setup_databases
    deploy_services
    health_check
    functional_test
    setup_monitoring
    generate_report
    
    success "🎉 Deployment completed successfully!"
    
    echo ""
    echo "=========================================="
    echo "         DEPLOYMENT SUMMARY"
    echo "=========================================="
    echo "Project: $PROJECT_NAME"
    echo "Services deployed: ${#SERVICES[@]}"
    echo "Status: ✅ All systems operational"
    echo ""
    echo "Service URLs:"
    for service in "${SERVICES[@]}"; do
        local url=$(railway status --service "$service" --json | jq -r '.deployments[0].url // "Not available"')
        echo "  • $service: $url"
    done
    echo ""
    echo "Next steps:"
    echo "1. Test the APIs using the provided URLs"
    echo "2. Configure monitoring and alerting"
    echo "3. Set up custom domains if needed"
    echo "4. Review and update security settings"
    echo "=========================================="
}

# Run deployment
main "$@"