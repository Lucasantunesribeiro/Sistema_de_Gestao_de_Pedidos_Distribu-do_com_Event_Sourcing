Write-Host "TESTE DO SISTEMA COMPLETO" -ForegroundColor Green
Write-Host "=========================" -ForegroundColor Green

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

Write-Host "   Payment Service (8083):" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8083/payments" -UseBasicParsing
    Write-Host "   ✅ Funcionando - Retornou $($response.Content.Length) caracteres" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "   Inventory Service (8082):" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8082/api/inventory/all" -UseBasicParsing
    Write-Host "   ✅ Funcionando - Retornou $($response.Content.Length) caracteres" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

# Testar frontend
Write-Host "`n2. Testando frontend..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3000" -UseBasicParsing
    Write-Host "   ✅ Frontend funcionando (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

# Testar proxy do frontend
Write-Host "`n3. Testando proxy do frontend..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3000/api/orders" -UseBasicParsing
    Write-Host "   ✅ Proxy /api/orders funcionando" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Erro no proxy: $($_.Exception.Message)" -ForegroundColor Red
}

# Status final
Write-Host "`n🎉 SISTEMA TESTADO!" -ForegroundColor Green
Write-Host "Frontend: http://localhost:3000" -ForegroundColor Cyan
Write-Host "Order Service: http://localhost:8081" -ForegroundColor Cyan
Write-Host "Payment Service: http://localhost:8083" -ForegroundColor Cyan
Write-Host "Inventory Service: http://localhost:8082" -ForegroundColor Cyan
Write-Host "Order Query Service: http://localhost:8084" -ForegroundColor Cyan 