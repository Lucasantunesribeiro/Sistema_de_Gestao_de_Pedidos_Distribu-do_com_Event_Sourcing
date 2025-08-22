# Setup script for Order Management System (PowerShell)
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
        exit 1
    }
    
    if (!(Get-Command docker-compose -ErrorAction SilentlyContinue)) {
        Write-Error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    }
    
    Write-Success "Docker and Docker Compose are installed"
}

function Test-Java {
    Write-Status "Checking Java installation..."
    
    if (!(Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Warning "Java is not installed. You'll need Java 17 for local development."
        return
    }
    
    $javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString().Split('"')[1] }
    $majorVersion = $javaVersion.Split('.')[0]
    
    if ([int]$majorVersion -ge 17) {
        Write-Success "Java $javaVersion is installed"
    } else {
        Write-Warning "Java version is $javaVersion. Java 17 or higher is recommended."
    }
}

function Test-Maven {
    Write-Status "Checking Maven installation..."
    
    if (!(Get-Command mvn -ErrorAction SilentlyContinue)) {
        Write-Warning "Maven is not installed. You'll need Maven for local development."
    } else {
        Write-Success "Maven is installed"
    }
}

function Test-Node {
    Write-Status "Checking Node.js installation..."
    
    if (!(Get-Command node -ErrorAction SilentlyContinue)) {
        Write-Warning "Node.js is not installed. You'll need Node.js 18+ for frontend development."
        return
    }
    
    $nodeVersion = node -v
    $majorVersion = $nodeVersion.Substring(1).Split('.')[0]
    
    if ([int]$majorVersion -ge 18) {
        Write-Success "Node.js $nodeVersion is installed"
    } else {
        Write-Warning "Node.js version is $nodeVersion. Node.js 18 or higher is recommended."
    }
}

function Build-SharedEvents {
    Write-Status "Building shared events library..."
    
    Push-Location shared-events
    try {
        $result = mvn clean install -q
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Shared events library built successfully"
        } else {
            Write-Error "Failed to build shared events library"
            exit 1
        }
    } finally {
        Pop-Location
    }
}

function Setup-Frontend {
    Write-Status "Setting up frontend dependencies..."
    
    Push-Location frontend
    try {
        npm install
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Frontend dependencies installed successfully"
        } else {
            Write-Error "Failed to install frontend dependencies"
            exit 1
        }
    } finally {
        Pop-Location
    }
}

function New-Directories {
    Write-Status "Creating necessary directories..."
    
    $directories = @("logs", "data\postgres", "data\rabbitmq", "data\redis")
    
    foreach ($dir in $directories) {
        if (!(Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
    }
    
    Write-Success "Directories created"
}

function Start-Infrastructure {
    Write-Status "Starting infrastructure services (PostgreSQL, RabbitMQ, Redis)..."
    
    docker-compose up -d order-db query-db rabbitmq redis
    
    Write-Status "Waiting for services to be ready..."
    Start-Sleep -Seconds 30
    
    $services = docker-compose ps
    if ($services -match "healthy") {
        Write-Success "Infrastructure services are running"
    } else {
        Write-Warning "Some services might not be fully ready yet. Check with: docker-compose ps"
    }
}

function Start-AllServices {
    Write-Status "Starting all services..."
    
    docker-compose up --build -d
    
    Write-Status "Waiting for all services to be ready..."
    Start-Sleep -Seconds 60
    
    Write-Success "All services should be running now!"
    Write-Status "Access the application at:"
    Write-Host "  - Frontend: http://localhost:3000" -ForegroundColor Cyan
    Write-Host "  - API Gateway: http://localhost:8080" -ForegroundColor Cyan
    Write-Host "  - RabbitMQ Management: http://localhost:15672 (guest/guest)" -ForegroundColor Cyan
}

function Test-Health {
    Write-Status "Performing health check..."
    
    $services = @(
        @{Name="order-service"; Port=8081},
        @{Name="payment-service"; Port=8082},
        @{Name="inventory-service"; Port=8083},
        @{Name="order-query-service"; Port=8084}
    )
    
    foreach ($service in $services) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$($service.Port)/actuator/health" -TimeoutSec 5 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                Write-Success "$($service.Name) is healthy"
            }
        } catch {
            Write-Warning "$($service.Name) might not be ready yet"
        }
    }
}

function Show-Menu {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "   Order Management System Setup" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "What would you like to do?"
    Write-Host "1) Full setup (build + start all services)"
    Write-Host "2) Build shared library only"
    Write-Host "3) Setup frontend only"
    Write-Host "4) Start infrastructure only"
    Write-Host "5) Start all services"
    Write-Host "6) Health check"
    Write-Host ""
    
    $choice = Read-Host "Enter your choice (1-6)"
    
    switch ($choice) {
        "1" {
            New-Directories
            Build-SharedEvents
            Setup-Frontend
            Start-AllServices
            Test-Health
        }
        "2" {
            Build-SharedEvents
        }
        "3" {
            Setup-Frontend
        }
        "4" {
            Start-Infrastructure
        }
        "5" {
            Start-AllServices
        }
        "6" {
            Test-Health
        }
        default {
            Write-Error "Invalid choice. Please run the script again."
            exit 1
        }
    }
}

# Main execution
Write-Status "Checking prerequisites..."
Test-Docker
Test-Java
Test-Maven
Test-Node
Write-Host ""

if ($Action -eq "menu") {
    Show-Menu
} else {
    switch ($Action) {
        "build" { Build-SharedEvents }
        "frontend" { Setup-Frontend }
        "infrastructure" { Start-Infrastructure }
        "all" { Start-AllServices }
        "health" { Test-Health }
        default { Show-Menu }
    }
}

Write-Host ""
Write-Success "Setup completed!"
Write-Host ""
Write-Host "Useful commands:"
Write-Host "  - View logs: docker-compose logs -f"
Write-Host "  - Stop services: docker-compose down"
Write-Host "  - Restart services: docker-compose restart"
Write-Host "  - View service status: docker-compose ps"