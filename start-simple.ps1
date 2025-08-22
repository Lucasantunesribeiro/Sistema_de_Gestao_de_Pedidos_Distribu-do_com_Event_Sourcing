Write-Host "Iniciando Sistema de Gestao de Pedidos..." -ForegroundColor Green

# Iniciar mock API
Write-Host "Iniciando Mock API..." -ForegroundColor Yellow
Start-Process -NoNewWindow -FilePath "node" -ArgumentList "mock-server.js" -WorkingDirectory "frontend"

# Aguardar um pouco
Start-Sleep -Seconds 2

# Iniciar frontend
Write-Host "Iniciando Frontend..." -ForegroundColor Yellow
Set-Location frontend
Start-Process -NoNewWindow -FilePath "cmd" -ArgumentList "/c", "npm", "run", "dev"
Set-Location ..

Write-Host ""
Write-Host "Sistema iniciado!" -ForegroundColor Green
Write-Host "Frontend: http://localhost:3000" -ForegroundColor Cyan
Write-Host "API Mock: http://localhost:8080" -ForegroundColor Cyan
Write-Host ""
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow

# Manter o script rodando
try {
    while ($true) { Start-Sleep -Seconds 1 }
} catch {
    Write-Host "Parando processos..." -ForegroundColor Yellow
    Get-Process -Name "node" -ErrorAction SilentlyContinue | Stop-Process -Force
}