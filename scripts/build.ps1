# Build script for Order Management System (PowerShell)
param(
    [switch]$SkipTests = $false
)

Write-Host "=== Order Management System Build Script ===" -ForegroundColor Green

function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Check prerequisites
Write-Status "Checking prerequisites..."

if (!(Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "Java is not installed or not in PATH"
    exit 1
}

if (!(Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Error "Maven is not installed or not in PATH"
    exit 1
}

if (!(Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Error "Node.js is not installed or not in PATH"
    exit 1
}

if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Warning "Docker is not installed - unified deployment will not be available"
}

Write-Status "Prerequisites check completed"

# Change to project root
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $scriptPath "..")

# Build Java services
Write-Status "Building Java services..."

# Clean previous builds
Write-Status "Cleaning previous builds..."
mvn clean

if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven clean failed"
    exit 1
}

# Build all services
Write-Status "Building all Java services..."
if ($SkipTests) {
    mvn package -DskipTests
} else {
    mvn package
}

if ($LASTEXITCODE -ne 0) {
    Write-Error "Java build failed"
    exit 1
}

Write-Status "Java services built successfully"

# Build frontend
Write-Status "Building React frontend..."
Set-Location frontend

# Install dependencies
Write-Status "Installing frontend dependencies..."
npm ci

if ($LASTEXITCODE -ne 0) {
    Write-Error "Frontend dependency installation failed"
    exit 1
}

# Build frontend
Write-Status "Building frontend for production..."
npm run build

if ($LASTEXITCODE -ne 0) {
    Write-Error "Frontend build failed"
    exit 1
}

Write-Status "Frontend built successfully"

Set-Location ..

# Build unified Docker image if Docker is available
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Status "Building unified Docker image..."
    docker build -f Dockerfile.unified -t order-management-system:latest .
    
    if ($LASTEXITCODE -eq 0) {
        Write-Status "Unified Docker image built successfully"
    } else {
        Write-Warning "Docker image build failed"
    }
}

Write-Status "Build completed successfully!"
Write-Status "Built artifacts:"
Write-Status "  - Java services: services/*/target/*.jar"
Write-Status "  - Frontend: frontend/build/"
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Status "  - Docker image: order-management-system:latest"
}