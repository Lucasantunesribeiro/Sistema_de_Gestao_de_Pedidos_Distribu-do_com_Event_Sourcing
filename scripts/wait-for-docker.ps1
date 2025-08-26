#!/usr/bin/env pwsh

Write-Host "=== Aguardando Docker Desktop inicializar ===" -ForegroundColor Cyan

$maxAttempts = 30
$attempt = 0
$dockerReady = $false

while ($attempt -lt $maxAttempts -and -not $dockerReady) {
    $attempt++
    Write-Host "Tentativa $attempt/$maxAttempts - Verificando Docker..." -ForegroundColor Yellow
    
    try {
        docker info 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $dockerReady = $true
            Write-Host "✅ Docker está pronto!" -ForegroundColor Green
        } else {
            Write-Host "⏳ Docker ainda não está pronto..." -ForegroundColor Yellow
            Start-Sleep -Seconds 10
        }
    } catch {
        Write-Host "⏳ Docker ainda não está pronto..." -ForegroundColor Yellow
        Start-Sleep -Seconds 10
    }
}

if (-not $dockerReady) {
    Write-Host "❌ Docker não iniciou dentro do tempo esperado" -ForegroundColor Red
    Write-Host "Por favor, inicie o Docker Desktop manualmente e tente novamente" -ForegroundColor White
    exit 1
}

Write-Host "🐳 Docker está funcionando!" -ForegroundColor Green
docker --version
docker info --format "{{.ServerVersion}}"