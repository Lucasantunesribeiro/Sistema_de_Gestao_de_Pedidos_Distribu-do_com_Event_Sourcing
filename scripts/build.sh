#!/bin/bash

# Build script for Order Management System
set -e

echo "=== Order Management System Build Script ==="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Check prerequisites
print_status "Checking prerequisites..."

if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi

if ! command -v node &> /dev/null; then
    print_error "Node.js is not installed or not in PATH"
    exit 1
fi

if ! command -v docker &> /dev/null; then
    print_warning "Docker is not installed - unified deployment will not be available"
fi

print_status "Prerequisites check completed"

# Build Java services
print_status "Building Java services..."

cd "$(dirname "$0")/.."

# Clean previous builds
print_status "Cleaning previous builds..."
mvn clean

# Build all services
print_status "Building all Java services..."
mvn package -DskipTests

if [ $? -ne 0 ]; then
    print_error "Java build failed"
    exit 1
fi

print_status "Java services built successfully"

# Build frontend
print_status "Building React frontend..."
cd frontend

# Install dependencies
print_status "Installing frontend dependencies..."
npm ci

# Build frontend
print_status "Building frontend for production..."
npm run build

if [ $? -ne 0 ]; then
    print_error "Frontend build failed"
    exit 1
fi

print_status "Frontend built successfully"

cd ..

# Build unified Docker image if Docker is available
if command -v docker &> /dev/null; then
    print_status "Building unified Docker image..."
    docker build -f Dockerfile.unified -t order-management-system:latest .
    
    if [ $? -eq 0 ]; then
        print_status "Unified Docker image built successfully"
    else
        print_warning "Docker image build failed"
    fi
fi

print_status "Build completed successfully!"
print_status "Built artifacts:"
print_status "  - Java services: services/*/target/*.jar"
print_status "  - Frontend: frontend/build/"
if command -v docker &> /dev/null; then
    print_status "  - Docker image: order-management-system:latest"
fi