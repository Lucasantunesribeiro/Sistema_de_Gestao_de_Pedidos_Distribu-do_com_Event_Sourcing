Write-Host "Iniciando Servicos Backend..." -ForegroundColor Green

# Build shared-events primeiro
Write-Host "Build shared-events..." -ForegroundColor Yellow
Set-Location shared-events
mvn clean install -q
Set-Location ..

# Iniciar order-service
Write-Host "Iniciando order-service..." -ForegroundColor Yellow
Start-Process -NoNewWindow -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory "services/order-service"

# Aguardar um pouco
Start-Sleep -Seconds 10

# Iniciar payment-service
Write-Host "Iniciando payment-service..." -ForegroundColor Yellow
Start-Process -NoNewWindow -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory "services/payment-service"

# Aguardar um pouco
Start-Sleep -Seconds 10

# Iniciar inventory-service
Write-Host "Iniciando inventory-service..." -ForegroundColor Yellow
Start-Process -NoNewWindow -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory "services/inventory-service"

# Aguardar um pouco
Start-Sleep -Seconds 10

# Iniciar order-query-service
Write-Host "Iniciando order-query-service..." -ForegroundColor Yellow
Start-Process -NoNewWindow -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory "services/order-query-service"

Write-Host ""
Write-Host "Servicos Backend iniciados!" -ForegroundColor Green
Write-Host "Order Service: http://localhost:8081" -ForegroundColor Cyan
Write-Host "Payment Service: http://localhost:8082" -ForegroundColor Cyan
Write-Host "Inventory Service: http://localhost:8083" -ForegroundColor Cyan
Write-Host "Order Query Service: http://localhost:8084" -ForegroundColor Cyan
Write-Host ""
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow

# Manter o script rodando
try {
    while ($true) { Start-Sleep -Seconds 1 }
} catch {
    Write-Host "Script interrompido" -ForegroundColor Red
} 