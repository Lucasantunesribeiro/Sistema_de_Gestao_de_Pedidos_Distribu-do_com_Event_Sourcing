Write-Host "=== Docker Status Check ===" -ForegroundColor Cyan

# Check if Docker is installed
$dockerVersion = docker --version 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Docker is installed: $dockerVersion" -ForegroundColor Green
} else {
    Write-Host "❌ Docker is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Check if Docker daemon is running
docker info 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Docker daemon is running" -ForegroundColor Green
    
    # Show running containers
    Write-Host "`n📋 Current containers:" -ForegroundColor Yellow
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    
    Write-Host "`n✅ Docker is ready for deployment!" -ForegroundColor Green
} else {
    Write-Host "❌ Docker daemon is not running" -ForegroundColor Red
    Write-Host "`n🔧 To start Docker:" -ForegroundColor Yellow
    Write-Host "1. Open Docker Desktop from Start Menu" -ForegroundColor White
    Write-Host "2. Wait for Docker to fully start (usually 30-60 seconds)" -ForegroundColor White
    Write-Host "3. Look for the Docker whale icon in system tray" -ForegroundColor White
    Write-Host "4. Run this script again to verify" -ForegroundColor White
    
    Write-Host "`n⏳ Attempting to start Docker Desktop..." -ForegroundColor Yellow
    Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe" -ErrorAction SilentlyContinue
    if ($?) {
        Write-Host "✅ Docker Desktop startup initiated" -ForegroundColor Green
        Write-Host "⏳ Please wait 30-60 seconds for Docker to fully start" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Could not start Docker Desktop automatically" -ForegroundColor Red
        Write-Host "Please start Docker Desktop manually from the Start Menu" -ForegroundColor White
    }
    
    exit 1
}