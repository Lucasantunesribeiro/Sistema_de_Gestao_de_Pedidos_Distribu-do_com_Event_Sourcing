#!/bin/bash

# Setup script for Order Management System
set -e

echo "🚀 Setting up Order Management System..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
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

# Check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    print_success "Docker and Docker Compose are installed"
}

# Check if Java is installed
check_java() {
    if ! command -v java &> /dev/null; then
        print_warning "Java is not installed. You'll need Java 17 for local development."
    else
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            print_success "Java $JAVA_VERSION is installed"
        else
            print_warning "Java version is $JAVA_VERSION. Java 17 or higher is recommended."
        fi
    fi
}

# Check if Maven is installed
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_warning "Maven is not installed. You'll need Maven for local development."
    else
        print_success "Maven is installed"
    fi
}

# Check if Node.js is installed
check_node() {
    if ! command -v node &> /dev/null; then
        print_warning "Node.js is not installed. You'll need Node.js 18+ for frontend development."
    else
        NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
        if [ "$NODE_VERSION" -ge 18 ]; then
            print_success "Node.js $NODE_VERSION is installed"
        else
            print_warning "Node.js version is $NODE_VERSION. Node.js 18 or higher is recommended."
        fi
    fi
}

# Build shared events library
build_shared_events() {
    print_status "Building shared events library..."
    cd shared-events
    if mvn clean install -q; then
        print_success "Shared events library built successfully"
    else
        print_error "Failed to build shared events library"
        exit 1
    fi
    cd ..
}

# Setup frontend dependencies
setup_frontend() {
    print_status "Setting up frontend dependencies..."
    cd frontend
    if npm install; then
        print_success "Frontend dependencies installed successfully"
    else
        print_error "Failed to install frontend dependencies"
        exit 1
    fi
    cd ..
}

# Create necessary directories
create_directories() {
    print_status "Creating necessary directories..."
    mkdir -p logs
    mkdir -p data/postgres
    mkdir -p data/rabbitmq
    mkdir -p data/redis
    print_success "Directories created"
}

# Start infrastructure services
start_infrastructure() {
    print_status "Starting infrastructure services (PostgreSQL, RabbitMQ, Redis)..."
    docker-compose up -d order-db query-db rabbitmq redis
    
    print_status "Waiting for services to be ready..."
    sleep 30
    
    # Check if services are healthy
    if docker-compose ps | grep -q "healthy"; then
        print_success "Infrastructure services are running"
    else
        print_warning "Some services might not be fully ready yet. Check with: docker-compose ps"
    fi
}

# Start all services
start_all_services() {
    print_status "Starting all services..."
    docker-compose up --build -d
    
    print_status "Waiting for all services to be ready..."
    sleep 60
    
    print_success "All services should be running now!"
    print_status "Access the application at:"
    echo "  - Frontend: http://localhost:3000"
    echo "  - API Gateway: http://localhost:8080"
    echo "  - RabbitMQ Management: http://localhost:15672 (guest/guest)"
}

# Health check
health_check() {
    print_status "Performing health check..."
    
    services=("order-service:8081" "payment-service:8082" "inventory-service:8083" "order-query-service:8084")
    
    for service in "${services[@]}"; do
        name=$(echo $service | cut -d':' -f1)
        port=$(echo $service | cut -d':' -f2)
        
        if curl -f -s "http://localhost:$port/api/*/health" > /dev/null 2>&1; then
            print_success "$name is healthy"
        else
            print_warning "$name might not be ready yet"
        fi
    done
}

# Main execution
main() {
    echo "============================================"
    echo "   Order Management System Setup"
    echo "============================================"
    echo
    
    # Check prerequisites
    print_status "Checking prerequisites..."
    check_docker
    check_java
    check_maven
    check_node
    echo
    
    # Ask user what they want to do
    echo "What would you like to do?"
    echo "1) Full setup (build + start all services)"
    echo "2) Build shared library only"
    echo "3) Setup frontend only"
    echo "4) Start infrastructure only"
    echo "5) Start all services"
    echo "6) Health check"
    echo
    read -p "Enter your choice (1-6): " choice
    
    case $choice in
        1)
            create_directories
            build_shared_events
            setup_frontend
            start_all_services
            health_check
            ;;
        2)
            build_shared_events
            ;;
        3)
            setup_frontend
            ;;
        4)
            start_infrastructure
            ;;
        5)
            start_all_services
            ;;
        6)
            health_check
            ;;
        *)
            print_error "Invalid choice. Please run the script again."
            exit 1
            ;;
    esac
    
    echo
    print_success "Setup completed!"
    echo
    echo "Useful commands:"
    echo "  - View logs: docker-compose logs -f"
    echo "  - Stop services: docker-compose down"
    echo "  - Restart services: docker-compose restart"
    echo "  - View service status: docker-compose ps"
}

# Run main function
main "$@"