#!/usr/bin/env pwsh

Write-Host "=== Iniciando Sistema Localmente (Sem Docker) ===" -ForegroundColor Cyan

# Verificar se o build foi feito
if (-not (Test-Path "services/order-service/target/order-service-1.0.0.jar")) {
    Write-Host "❌ Build não encontrado. Executando build primeiro..." -ForegroundColor Red
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Build falhou!" -ForegroundColor Red
        exit 1
    }
}

Write-Host "✅ Build encontrado. Iniciando serviços..." -ForegroundColor Green

# Função para iniciar um serviço em background
function Start-Service {
    param(
        [string]$ServiceName,
        [string]$JarPath,
        [int]$Port,
        [hashtable]$Environment = @{}
    )
    
    Write-Host "🚀 Iniciando $ServiceName na porta $Port..." -ForegroundColor Yellow
    
    # Preparar variáveis de ambiente
    $envVars = @{
        "SPRING_PROFILES_ACTIVE" = "local"
        "SERVER_PORT" = $Port.ToString()
    }
    
    # Adicionar variáveis específicas do serviço
    foreach ($key in $Environment.Keys) {
        $envVars[$key] = $Environment[$key]
    }
    
    # Converter para formato do PowerShell
    $envString = ""
    foreach ($key in $envVars.Keys) {
        $envString += "`$env:$key='$($envVars[$key])'; "
    }
    
    # Iniciar o serviço
    $command = "$envString java -jar $JarPath"
    Start-Process powershell -ArgumentList "-Command", $command -WindowStyle Minimized
    
    Write-Host "✅ $ServiceName iniciado" -ForegroundColor Green
}

# Iniciar Order Service
Start-Service -ServiceName "Order Service" -JarPath "services/order-service/target/order-service-1.0.0.jar" -Port 8081 -Environment @{
    "DATABASE_URL" = "jdbc:h2:mem:orderdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    "DATABASE_USERNAME" = "sa"
    "DATABASE_PASSWORD" = ""
    "SPRING_DATASOURCE_DRIVER_CLASS_NAME" = "org.h2.Driver"
    "SPRING_JPA_DATABASE_PLATFORM" = "org.hibernate.dialect.H2Dialect"
    "SPRING_JPA_HIBERNATE_DDL_AUTO" = "create-drop"
}

Start-Sleep -Seconds 5

# Iniciar Payment Service
Start-Service -ServiceName "Payment Service" -JarPath "services/payment-service/target/payment-service-1.0.0.jar" -Port 8082

Start-Sleep -Seconds 3

# Iniciar Inventory Service
Start-Service -ServiceName "Inventory Service" -JarPath "services/inventory-service/target/inventory-service-1.0.0.jar" -Port 8083

Start-Sleep -Seconds 3

# Iniciar Order Query Service
Start-Service -ServiceName "Order Query Service" -JarPath "services/order-query-service/target/order-query-service-1.0.0.jar" -Port 8084 -Environment @{
    "DATABASE_URL" = "jdbc:h2:mem:querydb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    "DATABASE_USERNAME" = "sa"
    "DATABASE_PASSWORD" = ""
    "SPRING_DATASOURCE_DRIVER_CLASS_NAME" = "org.h2.Driver"
    "SPRING_JPA_DATABASE_PLATFORM" = "org.hibernate.dialect.H2Dialect"
    "SPRING_JPA_HIBERNATE_DDL_AUTO" = "create-drop"
}

Write-Host "`n⏳ Aguardando serviços iniciarem..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

Write-Host "`n=== Verificando Status dos Serviços ===" -ForegroundColor Cyan

$services = @(
    @{Name="Order Service"; Url="http://localhost:8081/api/orders/health"},
    @{Name="Payment Service"; Url="http://localhost:8082/api/payments/health"},
    @{Name="Inventory Service"; Url="http://localhost:8083/api/inventory/health"},
    @{Name="Order Query Service"; Url="http://localhost:8084/api/orders/health"}
)

foreach ($service in $services) {
    try {
        $response = Invoke-RestMethod -Uri $service.Url -TimeoutSec 10
        Write-Host "✅ $($service.Name): OK" -ForegroundColor Green
    } catch {
        Write-Host "❌ $($service.Name): FALHOU - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`nSistema iniciado localmente!" -ForegroundColor Green
Write-Host "Servicos disponiveis:" -ForegroundColor Yellow
Write-Host "  • Order Service: http://localhost:8081/api/orders" -ForegroundColor White
Write-Host "  • Payment Service: http://localhost:8082/api/payments" -ForegroundColor White
Write-Host "  • Inventory Service: http://localhost:8083/api/inventory" -ForegroundColor White
Write-Host "  • Order Query Service: http://localhost:8084/api/orders" -ForegroundColor White

Write-Host "`nPara testar o sistema:" -ForegroundColor Yellow
Write-Host "  .\scripts\test-integration.ps1" -ForegroundColor White

Write-Host "`nPara parar os servicos:" -ForegroundColor Yellow
Write-Host "  .\scripts\stop-local.ps1" -ForegroundColor White