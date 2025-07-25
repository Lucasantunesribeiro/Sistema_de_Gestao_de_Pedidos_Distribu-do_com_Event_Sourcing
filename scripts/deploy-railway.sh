#!/bin/bash

# Railway Deployment Script
# Automates deployment of microservices to Railway.app

set -e

echo "🚂 Railway Deployment Script"
echo "============================"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
RAILWAY_PROJECT_NAME="distributed-order-system"
SERVICES=("order-service" "payment-service" "inventory-service" "order-query-service")

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

check_railway_cli() {
    if ! command -v railway &> /dev/null; then
        log_error "Railway CLI is not installed"
        log_info "Install it from: https://docs.railway.app/develop/cli"
        exit 1
    fi
    
    log_success "Railway CLI found"
}

check_authentication() {
    if ! railway whoami &> /dev/null; then
        log_error "Not authenticated with Railway"
        log_info "Run: railway login"
        exit 1
    fi
    
    log_success "Authenticated with Railway"
}

create_or_select_project() {
    log_info "Setting up Railway project..."
    
    # Try to link to existing project or create new one
    if ! railway status &> /dev/null; then
        log_info "Creating new Railway project: $RAILWAY_PROJECT_NAME"
        railway init "$RAILWAY_PROJECT_NAME"
    else
        log_info "Using existing Railway project"
    fi
}

setup_databases() {
    log_info "Setting up databases..."
    
    # Create PostgreSQL databases
    log_info "Creating PostgreSQL databases..."
    
    # Order database
    railway add --database postgresql
    railway variables set DATABASE_NAME=order_db
    
    # Query database  
    railway add --database postgresql
    railway variables set QUERY_DATABASE_NAME=order_query_db
    
    log_success "Databases configured"
}

setup_rabbitmq() {
    log_info "Setting up RabbitMQ..."
    
    # Add RabbitMQ service
    railway add --template rabbitmq
    
    log_success "RabbitMQ configured"
}

deploy_service() {
    local service_name=$1
    local service_path="services/$service_name"
    
    log_info "Deploying $service_name..."
    
    # Create service
    railway service create "$service_name"
    
    # Set service-specific variables
    case $service_name in
        "order-service")
            railway variables set \
                SPRING_PROFILES_ACTIVE=railway \
                SERVER_PORT=8081 \
                JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
            ;;
        "payment-service")
            railway variables set \
                SPRING_PROFILES_ACTIVE=railway \
                SERVER_PORT=8082 \
                JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
            ;;
        "inventory-service")
            railway variables set \
                SPRING_PROFILES_ACTIVE=railway \
                SERVER_PORT=8083 \
                JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
            ;;
        "order-query-service")
            railway variables set \
                SPRING_PROFILES_ACTIVE=railway \
                SERVER_PORT=8084 \
                JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
            ;;
    esac
    
    # Deploy the service
    cd "$service_path"
    railway up --detach
    cd - > /dev/null
    
    log_success "$service_name deployed"
}

wait_for_deployments() {
    log_info "Waiting for deployments to complete..."
    
    for service in "${SERVICES[@]}"; do
        log_info "Checking $service deployment status..."
        
        # Wait for deployment to complete
        timeout=300
        elapsed=0
        
        while [ $elapsed -lt $timeout ]; do
            if railway status --service "$service" | grep -q "ACTIVE"; then
                log_success "$service is active"
                break
            fi
            
            sleep 10
            elapsed=$((elapsed + 10))
        done
        
        if [ $elapsed -ge $timeout ]; then
            log_error "$service deployment timed out"
            return 1
        fi
    done
}

verify_deployments() {
    log_info "Verifying deployments..."
    
    for service in "${SERVICES[@]}"; do
        log_info "Getting $service URL..."
        
        service_url=$(railway url --service "$service" 2>/dev/null || echo "URL not available")
        
        if [ "$service_url" != "URL not available" ]; then
            log_info "$service URL: $service_url"
            
            # Test health endpoint
            health_url="$service_url/actuator/health"
            if curl -f -s --max-time 10 "$health_url" | grep -q '"status":"UP"'; then
                log_success "$service health check passed"
            else
                log_warning "$service health check failed or not ready yet"
            fi
        else
            log_warning "$service URL not available yet"
        fi
    done
}

show_deployment_info() {
    log_info "Deployment Information"
    echo "======================"
    
    echo ""
    echo "🚀 Services:"
    for service in "${SERVICES[@]}"; do
        service_url=$(railway url --service "$service" 2>/dev/null || echo "Pending...")
        echo "  - $service: $service_url"
    done
    
    echo ""
    echo "📊 Project Status:"
    railway status
    
    echo ""
    echo "🔧 Useful Commands:"
    echo "  - View logs: railway logs --service <service-name>"
    echo "  - Check status: railway status"
    echo "  - Open dashboard: railway open"
    echo "  - Connect to database: railway connect <database-service>"
}

main() {
    log_info "Starting Railway deployment..."
    
    # Pre-flight checks
    check_railway_cli
    check_authentication
    
    # Setup project
    create_or_select_project
    
    # Setup infrastructure
    setup_databases
    setup_rabbitmq
    
    # Deploy services
    for service in "${SERVICES[@]}"; do
        deploy_service "$service"
    done
    
    # Wait for deployments
    if ! wait_for_deployments; then
        log_error "Some deployments failed"
        exit 1
    fi
    
    # Verify deployments
    verify_deployments
    
    # Show deployment info
    show_deployment_info
    
    log_success "🎉 Railway deployment completed!"
    log_info "Your distributed order system is now running on Railway!"
}

# Handle script interruption
trap 'log_warning "Deployment interrupted by user"; exit 130' INT

# Run main function
main "$@"