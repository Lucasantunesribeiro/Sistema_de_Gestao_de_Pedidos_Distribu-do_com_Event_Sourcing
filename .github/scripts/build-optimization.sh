#!/bin/bash

# Build Optimization Script for CI/CD
# Optimizes Maven and Frontend builds for production deployment

set -e

echo "🚀 Starting optimized build process..."

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

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi

# Check if Node is available
if ! command -v npm &> /dev/null; then
    print_error "Node.js/npm is not installed or not in PATH"
    exit 1
fi

print_status "Maven version: $(mvn -version | head -1)"
print_status "Node version: $(node --version)"
print_status "NPM version: $(npm --version)"

# 1. Optimize Maven Build
print_status "Building active shared libraries..."
mvn -pl libs/common-events,libs/common-security,libs/common-messaging,libs/common-observability -am clean install -DskipTests -B -q
print_success "Active shared libraries built successfully"

print_status "Building unified runtime with optimizations..."
mvn -f unified-order-system/pom.xml clean package -DskipTests -B -T 1C -q \
    -Dmaven.compile.fork=true \
    -Dmaven.compiler.maxmem=1024m

if [ $? -eq 0 ]; then
    print_success "Unified runtime built successfully"
else
    print_error "Failed to build unified runtime"
    exit 1
fi

# 2. Optimize Frontend Build
print_status "Building optimized frontend..."
cd frontend

# Clear npm cache for clean build
npm cache clean --force

# Install dependencies with optimizations
npm ci --prefer-offline --no-audit

# Run build with optimization flags
NODE_ENV=production npm run build

if [ $? -eq 0 ]; then
    print_success "Frontend built successfully"
else
    print_error "Failed to build frontend"
    exit 1
fi

# Analyze bundle size
BUNDLE_SIZE=$(du -sh dist | cut -f1)
print_status "Frontend bundle size: $BUNDLE_SIZE"

# Check if bundle is too large (warning if > 2MB)
BUNDLE_SIZE_KB=$(du -sk dist | cut -f1)
if [ $BUNDLE_SIZE_KB -gt 2048 ]; then
    print_warning "Bundle size ($BUNDLE_SIZE) exceeds 2MB - consider optimization"
else
    print_success "Bundle size ($BUNDLE_SIZE) within acceptable limits"
fi

# Return to root
cd ..

# 3. Validate JAR files
print_status "Validating JAR files..."
JAR_FILES=(
    "unified-order-system/target/unified-order-system-1.0.0.jar"
)

for jar in "${JAR_FILES[@]}"; do
    if [ -f "$jar" ]; then
        SIZE=$(du -h "$jar" | cut -f1)
        print_success "✓ $jar ($SIZE)"
    else
        print_error "✗ Missing: $jar"
        exit 1
    fi
done

# 4. Validate Frontend dist
if [ -d "frontend/dist" ] && [ -f "frontend/dist/index.html" ]; then
    print_success "✓ Frontend dist directory created successfully"
else
    print_error "✗ Frontend dist directory missing or incomplete"
    exit 1
fi

# 5. Build Summary
echo ""
echo "═══════════════════════════════════════"
echo "🎯 BUILD OPTIMIZATION COMPLETE"
echo "═══════════════════════════════════════"
echo ""

# Services summary
echo "Java Services:"
for jar in "${JAR_FILES[@]}"; do
    if [ -f "$jar" ]; then
        SIZE=$(du -h "$jar" | cut -f1)
        SERVICE_NAME=$(basename "$jar" .jar)
        echo "  ✅ $SERVICE_NAME: $SIZE"
    fi
done

# Frontend summary
echo ""
echo "Frontend:"
echo "  ✅ Bundle size: $BUNDLE_SIZE"
echo "  ✅ Output directory: frontend/dist/"

# Performance metrics
echo ""
echo "Performance Metrics:"
echo "  📊 Total build time: ${SECONDS}s"
echo "  📦 Unified runtime ready for containerization"
echo "  🚀 Ready for Render deployment"

print_success "All builds completed successfully! 🎉"
