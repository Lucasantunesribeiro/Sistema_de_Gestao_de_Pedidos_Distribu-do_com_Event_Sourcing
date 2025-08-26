# Deployment script for Order Management System (PowerShell)
param(
    [string]$Environment = "development",
    [string]$Type = "docker-compose",
    [switch]$SkipBuild = $false
)

Write-Host "=== Order Management System Deployment Script ===" -ForegroundColor Blue

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

function Write-Header {
    param([string]$Message)
    Write-Host "[DEPLOY] $Message" -ForegroundColor Blue
}

Write-Header "Starting deployment with environment: $Environment, type: $Type"

# Change to project root
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $scriptPath "..")

# Build if not skipped
if (-not $SkipBuild) {
    Write-Status "Building application..."
    & ".\scripts\build.ps1" -SkipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build failed"
        exit 1
    }
}

# Deploy based on type
switch ($Type) {
    "docker-compose" {
        Write-Header "Deploying with Docker Compose..."
        
        # Stop existing containers
        Write-Status "Stopping existing containers..."
        docker-compose down --remove-orphans
        
        # Load environment variables
        $envFile = "deployment\$Environment.env"
        if (Test-Path $envFile) {
            Write-Status "Loading environment variables from $envFile"
            Get-Content $envFile | Where-Object { $_ -notmatch '^#' -and $_ -ne '' } | ForEach-Object {
                $name, $value = $_ -split '=', 2
                [Environment]::SetEnvironmentVariable($name, $value, 'Process')
            }
        }
        
        # Start services
        Write-Status "Starting services..."
        docker-compose up -d --build
        
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Docker Compose deployment failed"
            exit 1
        }
        
        # Wait for services to be ready
        Write-Status "Waiting for services to be ready..."
        Start-Sleep -Seconds 30
        
        # Health check
        Write-Status "Performing health checks..."
        $ports = @(8081, 8082, 8083, 8084)
        foreach ($port in $ports) {
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:$port/actuator/health" -TimeoutSec 5 -ErrorAction Stop
                Write-Status "Service on port $port is healthy"
            } catch {
                Write-Warning "Service on port $port is not responding"
            }
        }
    }
    
    "unified" {
        Write-Header "Deploying unified container..."
        
        # Stop existing container
        Write-Status "Stopping existing unified container..."
        docker stop order-management-system 2>$null
        docker rm order-management-system 2>$null
        
        # Load environment variables
        $envFileArg = ""
        $envFile = "deployment\$Environment.env"
        if (Test-Path $envFile) {
            $envFileArg = "--env-file $envFile"
        }
        
        # Start unified container
        Write-Status "Starting unified container..."
        $dockerCmd = "docker run -d --name order-management-system -p 80:80 -p 8081:8081 -p 8082:8082 -p 8083:8083 -p 8084:8084 $envFileArg order-management-system:latest"
        Invoke-Expression $dockerCmd
        
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Unified container deployment failed"
            exit 1
        }
        
        # Wait for container to be ready
        Write-Status "Waiting for container to be ready..."
        Start-Sleep -Seconds 60
        
        # Health check
        Write-Status "Performing health check..."
        try {
            $response = Invoke-WebRequest -Uri "http://localhost/health" -TimeoutSec 10 -ErrorAction Stop
            Write-Status "Unified deployment is healthy"
        } catch {
            Write-Warning "Unified deployment health check failed"
        }
    }
    
    "kubernetes" {
        Write-Header "Deploying to Kubernetes..."
        Write-Warning "Kubernetes deployment not implemented yet"
        exit 1
    }
    
    default {
        Write-Error "Unknown deployment type: $Type"
        exit 1
    }
}

Write-Status "Deployment completed successfully!"
Write-Status "Access the application at:"
switch ($Type) {
    "docker-compose" {
        Write-Status "  - Frontend: http://localhost:3000"
        Write-Status "  - Order Service: http://localhost:8081"
        Write-Status "  - Payment Service: http://localhost:8082"
        Write-Status "  - Inventory Service: http://localhost:8083"
        Write-Status "  - Query Service: http://localhost:8084"
    }
    "unified" {
        Write-Status "  - Application: http://localhost"
        Write-Status "  - Health Check: http://localhost/health"
    }
}