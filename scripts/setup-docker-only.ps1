# Setup script for Order Management System (Docker Only - No Maven Required)
param(
    [string]$Action = "menu"
)

# Colors for output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Blue"

function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor $Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor $Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $Red
}

function Test-Docker {
    Write-Status "Checking Docker installation..."
    
    if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "Docker is not installed. Please install Docker Desktop first."
        Write-Host "Download from: https://www.docker.com/products/docker-desktop/" -ForegroundColor Cyan
        exit 1
    }
    
    if (!(Get-Command docker-compose -ErrorAction SilentlyContinue)) {
        Write-Error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    }
    
    # Test if Docker is running
    try {
        docker ps | Out-Null
        Write-Success "Docker and Docker Compose are installed and running"
    } catch {
        Write-Error "Docker is installed but not running. Please start Docker Desktop."
        exit 1
    }
}

function Test-Node {
    Write-Status "Checking Node.js installation..."
    
    if (!(Get-Command node -ErrorAction SilentlyContinue)) {
        Write-Warning "Node.js is not installed. You'll need Node.js 18+ for frontend development."
        Write-Host "Download from: https://nodejs.org/" -ForegroundColor Cyan
        return $false
    }
    
    $nodeVersion = node -v
    $majorVersion = $nodeVersion.Substring(1).Split('.')[0]
    
    if ([int]$majorVersion -ge 18) {
        Write-Success "Node.js $nodeVersion is installed"
        return $true
    } else {
        Write-Warning "Node.js version is $nodeVersion. Node.js 18 or higher is recommended."
        return $false
    }
}

function Build-SharedEventsWithDocker {
    Write-Status "Building shared events library using Docker..."
    
    # Create a temporary Dockerfile for building shared-events
    $dockerfileContent = @"
FROM maven:3.9-openjdk-17-slim AS builder
WORKDIR /app
COPY shared-events/pom.xml ./
RUN mvn dependency:go-offline -B
COPY shared-events/src ./src
RUN mvn clean install -DskipTests

FROM alpine:latest
WORKDIR /output
COPY --from=builder /root/.m2/repository/com/ordersystem/shared-events ./
CMD ["sh", "-c", "echo 'Shared events built successfully'"]
"@
    
    $dockerfileContent | Out-File -FilePath "Dockerfile.shared-events" -Encoding UTF8
    
    try {
        Write-Status "Building shared-events with Docker (this may take a few minutes)..."
        docker build -f Dockerfile.shared-events -t shared-events-builder .
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Shared events library built successfully with Docker"
            
            # Extract the built artifacts to local .m2 repository
            Write-Status "Extracting built artifacts..."
            docker run --rm -v "${PWD}:/host" shared-events-builder sh -c "cp -r /output/* /host/shared-events-output/ 2>/dev/null || true"
            
            return $true
        } else {
            Write-Error "Failed to build shared events library with Docker"
            return $false
        }
    } catch {
        Write-Error "Error building shared events: $_"
        return $false
    } finally {
        # Clean up temporary Dockerfile
        if (Test-Path "Dockerfile.shared-events") {
            Remove-Item "Dockerfile.shared-events" -Force
        }
    }
}

function Setup-Frontend {
    Write-Status "Setting up frontend dependencies..."
    
    if (!(Test-Node)) {
        Write-Warning "Skipping frontend setup - Node.js not available"
        return $false
    }
    
    Push-Location frontend
    try {
        Write-Status "Installing frontend dependencies..."
        npm install
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Frontend dependencies installed successfully"
            return $true
        } else {
            Write-Error "Failed to install frontend dependencies"
            return $false
        }
    } catch {
        Write-Error "Error setting up frontend: $_"
        return $false
    } finally {
        Pop-Location
    }
}

function New-Directories {
    Write-Status "Creating necessary directories..."
    
    $directories = @("logs", "data\postgres", "data\rabbitmq", "data\redis", "shared-events-output")
    
    foreach ($dir in $directories) {
        if (!(Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
    }
    
    Write-Success "Directories created"
}

function Start-Infrastructure {
    Write-Status "Starting infrastructure services (PostgreSQL, RabbitMQ, Redis)..."
    
    try {
        docker-compose up -d order-db query-db rabbitmq redis
        
        Write-Status "Waiting for services to be ready..."
        Start-Sleep -Seconds 30
        
        $services = docker-compose ps
        if ($services -match "healthy" -or $services -match "Up") {
            Write-Success "Infrastructure services are running"
            return $true
        } else {
            Write-Warning "Some services might not be fully ready yet. Check with: docker-compose ps"
            return $false
        }
    } catch {
        Write-Error "Failed to start infrastructure services: $_"
        return $false
    }
}

function Start-AllServices {
    Write-Status "Starting all services with Docker Compose..."
    
    try {
        # Build and start all services
        docker-compose up --build -d
        
        Write-Status "Waiting for all services to be ready (this may take 2-3 minutes)..."
        
        # Wait and show progress
        for ($i = 1; $i -le 12; $i++) {
            Write-Host "." -NoNewline -ForegroundColor Yellow
            Start-Sleep -Seconds 15
        }
        Write-Host ""
        
        Write-Success "All services should be running now!"
        Write-Status "Access the application at:"
        Write-Host "  - Frontend: http://localhost:3000" -ForegroundColor Cyan
        Write-Host "  - API Gateway: http://localhost:8080" -ForegroundColor Cyan
        Write-Host "  - RabbitMQ Management: http://localhost:15672 (guest/guest)" -ForegroundColor Cyan
        
        return $true
    } catch {
        Write-Error "Failed to start all services: $_"
        return $false
    }
}

function Test-Health {
    Write-Status "Performing health check..."
    
    $services = @(
        @{Name="Frontend"; Url="http://localhost:3000"},
        @{Name="API Gateway"; Url="http://localhost:8080/health"},
        @{Name="RabbitMQ Management"; Url="http://localhost:15672"}
    )
    
    foreach ($service in $services) {
        try {
            $response = Invoke-WebRequest -Uri $service.Url -TimeoutSec 10 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                Write-Success "$($service.Name) is accessible"
            }
        } catch {
            Write-Warning "$($service.Name) might not be ready yet or is not accessible"
        }
    }
    
    Write-Status "Checking Docker containers status..."
    docker-compose ps
}

function Stop-AllServices {
    Write-Status "Stopping all services..."
    
    try {
        docker-compose down
        Write-Success "All services stopped"
    } catch {
        Write-Error "Failed to stop services: $_"
    }
}

function Show-Logs {
    Write-Status "Showing logs from all services..."
    docker-compose logs -f
}

function Show-Menu {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "   Order Management System Setup (Docker)" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "What would you like to do?"
    Write-Host "1) Full setup (build + start all services) - RECOMMENDED"
    Write-Host "2) Setup frontend only"
    Write-Host "3) Start infrastructure only"
    Write-Host "4) Start all services (skip build)"
    Write-Host "5) Health check"
    Write-Host "6) Stop all services"
    Write-Host "7) Show logs"
    Write-Host ""
    
    $choice = Read-Host "Enter your choice (1-7)"
    
    switch ($choice) {
        "1" {
            New-Directories
            Setup-Frontend
            Start-AllServices
            Start-Sleep -Seconds 10
            Test-Health
        }
        "2" {
            Setup-Frontend
        }
        "3" {
            Start-Infrastructure
        }
        "4" {
            docker-compose up -d
            Start-Sleep -Seconds 30
            Test-Health
        }
        "5" {
            Test-Health
        }
        "6" {
            Stop-AllServices
        }
        "7" {
            Show-Logs
        }
        default {
            Write-Error "Invalid choice. Please run the script again."
            exit 1
        }
    }
}

# Main execution
Write-Host "🚀 Order Management System Setup (Docker-Only Version)" -ForegroundColor Green
Write-Host ""

Write-Status "Checking prerequisites..."
Test-Docker
Write-Host ""

if ($Action -eq "menu") {
    Show-Menu
} else {
    switch ($Action) {
        "frontend" { Setup-Frontend }
        "infrastructure" { Start-Infrastructure }
        "all" { Start-AllServices }
        "health" { Test-Health }
        "stop" { Stop-AllServices }
        "logs" { Show-Logs }
        default { Show-Menu }
    }
}

Write-Host ""
Write-Success "Setup completed!"
Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Yellow
Write-Host "  - View logs: docker-compose logs -f"
Write-Host "  - Stop services: docker-compose down"
Write-Host "  - Restart services: docker-compose restart"
Write-Host "  - View service status: docker-compose ps"
Write-Host "  - Remove all data: docker-compose down -v"