Write-Host "=== Parando Servicos Locais ===" -ForegroundColor Cyan

# Parar processos Java que estao rodando os servicos
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue

if ($javaProcesses) {
    Write-Host "Parando $($javaProcesses.Count) processo(s) Java..." -ForegroundColor Yellow
    
    foreach ($process in $javaProcesses) {
        try {
            $process.Kill()
            Write-Host "Processo Java (PID: $($process.Id)) parado" -ForegroundColor Green
        } catch {
            Write-Host "Erro ao parar processo Java (PID: $($process.Id)): $($_.Exception.Message)" -ForegroundColor Red
        }
    }
} else {
    Write-Host "Nenhum processo Java encontrado" -ForegroundColor Blue
}

# Verificar se ainda ha processos rodando nas portas
$ports = @(8081, 8082, 8083, 8084)

foreach ($port in $ports) {
    try {
        $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
        if ($connection) {
            Write-Host "Porta $port ainda esta em uso" -ForegroundColor Yellow
        } else {
            Write-Host "Porta $port esta livre" -ForegroundColor Green
        }
    } catch {
        Write-Host "Porta $port esta livre" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Servicos locais parados!" -ForegroundColor Green