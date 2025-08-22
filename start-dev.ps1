Write-Host "Starting Order Management System..." -ForegroundColor Green
Start-Process -NoNewWindow -FilePath "node" -ArgumentList "mock-server.js" -WorkingDirectory "frontend"
Start-Sleep -Seconds 3
Start-Process -NoNewWindow -FilePath "npm" -ArgumentList "run", "dev" -WorkingDirectory "frontend"
Write-Host "System started! Frontend: http://localhost:3000" -ForegroundColor Green
Write-Host "Press Ctrl+C to stop all processes" -ForegroundColor Yellow
try {
    while ($true) { Start-Sleep -Seconds 1 }
} catch {
    Write-Host "Stopping processes..." -ForegroundColor Yellow
    Get-Process -Name "node" -ErrorAction SilentlyContinue | Stop-Process -Force
}
