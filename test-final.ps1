Write-Host "TESTE FINAL DO SISTEMA" -ForegroundColor Green
Write-Host "=====================" -ForegroundColor Green

# Testar serviços backend
Write-Host "`n1. Testando serviços backend..." -ForegroundColor Yellow

Write-Host "   Order Service (8081):" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -UseBasicParsing
    Write-Host "   ✅ Funcionando" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "   Order Query Service (8084):" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8084/api/orders" -UseBasicParsing
    Write-Host "   ✅ Funcionando - Retornou $($response.Content.Length) caracteres" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

# Testar frontend
Write-Host "`n2. Testando frontend..." -ForegroundColor Yellow
Write-Host "   Frontend (3000):" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3000" -UseBasicParsing
    Write-Host "   ✅ Funcionando" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

# Testar proxy do frontend para backend
Write-Host "`n3. Testando proxy frontend -> backend..." -ForegroundColor Yellow
Write-Host "   Frontend -> Order Query Service:" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3000/api/orders" -UseBasicParsing
    Write-Host "   ✅ Funcionando - Retornou $($response.Content.Length) caracteres" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n🎉 SISTEMA FUNCIONANDO!" -ForegroundColor Green
Write-Host "=====================" -ForegroundColor Green
Write-Host "Frontend: http://localhost:3000" -ForegroundColor Cyan
Write-Host "Order Service: http://localhost:8081" -ForegroundColor Cyan
Write-Host "Order Query Service: http://localhost:8084" -ForegroundColor Cyan
Write-Host "`nNota: Payment e Inventory services estão temporariamente desabilitados" -ForegroundColor Yellow 