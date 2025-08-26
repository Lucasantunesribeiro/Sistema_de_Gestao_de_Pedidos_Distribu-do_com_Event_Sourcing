#!/bin/bash

# Deployment script for Order Management System
set -e

echo "=== Order Management System Deployment Script ==="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}[DEPLOY]${NC} $1"
}

# Default values
ENVIRONMENT="development"
DEPLOYMENT_TYPE="docker-compose"
SKIP_BUILD=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -t|--type)
            DEPLOYMENT_TYPE="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -e, --environment    Environment (development|production) [default: development]"
            echo "  -t, --type          Deployment type (docker-compose|unified|kubernetes) [default: docker-compose]"
            echo "  --skip-build        Skip build step"
            echo "  -h, --help          Show this help message"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

print_header "Starting deployment with environment: $ENVIRONMENT, type: $DEPLOYMENT_TYPE"

# Change to project root
cd "$(dirname "$0")/.."

# Build if not skipped
if [ "$SKIP_BUILD" = false ]; then
    print_status "Building application..."
    ./scripts/build.sh
    if [ $? -ne 0 ]; then
        print_error "Build failed"
        exit 1
    fi
fi

# Deploy based on type
case $DEPLOYMENT_TYPE in
    docker-compose)
        print_header "Deploying with Docker Compose..."
        
        # Stop existing containers
        print_status "Stopping existing containers..."
        docker-compose down --remove-orphans
        
        # Load environment variables
        if [ -f "deployment/${ENVIRONMENT}.env" ]; then
            print_status "Loading environment variables from deployment/${ENVIRONMENT}.env"
            export $(cat deployment/${ENVIRONMENT}.env | grep -v '^#' | xargs)
        fi
        
        # Start services
        print_status "Starting services..."
        docker-compose up -d --build
        
        # Wait for services to be ready
        print_status "Waiting for services to be ready..."
        sleep 30
        
        # Health check
        print_status "Performing health checks..."
        for port in 8081 8082 8083 8084; do
            if curl -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
                print_status "Service on port $port is healthy"
            else
                print_warning "Service on port $port is not responding"
            fi
        done
        ;;
        
    unified)
        print_header "Deploying unified container..."
        
        # Stop existing container
        print_status "Stopping existing unified container..."
        docker stop order-management-system || true
        docker rm order-management-system || true
        
        # Load environment variables
        ENV_FILE=""
        if [ -f "deployment/${ENVIRONMENT}.env" ]; then
            ENV_FILE="--env-file deployment/${ENVIRONMENT}.env"
        fi
        
        # Start unified container
        print_status "Starting unified container..."
        docker run -d \
            --name order-management-system \
            -p 80:80 \
            -p 8081:8081 \
            -p 8082:8082 \
            -p 8083:8083 \
            -p 8084:8084 \
            $ENV_FILE \
            order-management-system:latest
        
        # Wait for container to be ready
        print_status "Waiting for container to be ready..."
        sleep 60
        
        # Health check
        print_status "Performing health check..."
        if curl -f http://localhost/health > /dev/null 2>&1; then
            print_status "Unified deployment is healthy"
        else
            print_warning "Unified deployment health check failed"
        fi
        ;;
        
    kubernetes)
        print_header "Deploying to Kubernetes..."
        print_warning "Kubernetes deployment not implemented yet"
        exit 1
        ;;
        
    *)
        print_error "Unknown deployment type: $DEPLOYMENT_TYPE"
        exit 1
        ;;
esac

print_status "Deployment completed successfully!"
print_status "Access the application at:"
case $DEPLOYMENT_TYPE in
    docker-compose)
        print_status "  - Frontend: http://localhost:3000"
        print_status "  - Order Service: http://localhost:8081"
        print_status "  - Payment Service: http://localhost:8082"
        print_status "  - Inventory Service: http://localhost:8083"
        print_status "  - Query Service: http://localhost:8084"
        ;;
    unified)
        print_status "  - Application: http://localhost"
        print_status "  - Health Check: http://localhost/health"
        ;;
esac