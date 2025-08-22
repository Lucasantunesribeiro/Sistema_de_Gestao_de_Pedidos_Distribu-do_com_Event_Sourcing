Write-Host "SISTEMA DE GESTAO DE PEDIDOS - INICIANDO SISTEMA COMPLETO" -ForegroundColor Green

# Verificar se Docker está rodando
Write-Host "Verificando Docker..." -ForegroundColor Yellow
try {
    docker ps | Out-Null
    Write-Host "Docker OK" -ForegroundColor Green
} catch {
    Write-Host "Docker nao esta rodando. Inicie o Docker Desktop." -ForegroundColor Red
    exit 1
}

# Iniciar infraestrutura
Write-Host "Iniciando infraestrutura..." -ForegroundColor Yellow
docker-compose -f docker-compose-fixed.yml up -d

# Aguardar infraestrutura ficar pronta
Write-Host "Aguardando infraestrutura..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Verificar status dos serviços
Write-Host "Verificando status dos serviços..." -ForegroundColor Yellow
docker-compose -f docker-compose-fixed.yml ps

# Iniciar frontend
Write-Host "Iniciando frontend..." -ForegroundColor Yellow
Set-Location frontend
Start-Process -NoNewWindow -FilePath "npm" -ArgumentList "run", "dev"
Set-Location ..

Write-Host ""
Write-Host "SISTEMA INICIADO COM SUCESSO!" -ForegroundColor Green
Write-Host ""
Write-Host "URLs dos serviços:" -ForegroundColor Cyan
Write-Host "Frontend: http://localhost:3000" -ForegroundColor White
Write-Host "Order Service: http://localhost:8081" -ForegroundColor White
Write-Host "Payment Service: http://localhost:8083" -ForegroundColor White
Write-Host "Inventory Service: http://localhost:8082" -ForegroundColor White
Write-Host "Order Query Service: http://localhost:8084" -ForegroundColor White
Write-Host "RabbitMQ Management: http://localhost:15672" -ForegroundColor White
Write-Host ""
Write-Host "Credenciais RabbitMQ: guest/guest" -ForegroundColor Yellow
Write-Host ""
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow

# Manter o script rodando
try {
    while ($true) { Start-Sleep -Seconds 10 }
} catch {
    Write-Host "Parando sistema..." -ForegroundColor Yellow
    docker-compose -f docker-compose-fixed.yml down
    Get-Process -Name "node" -ErrorAction SilentlyContinue | Stop-Process -Force
}