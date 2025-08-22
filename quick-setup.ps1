# Sistema de Gestao de Pedidos Distribuido - Quick Setup (PowerShell)
Write-Host "SISTEMA DE GESTAO DE PEDIDOS - QUICK SETUP" -ForegroundColor Green

# Check Docker
try {
    docker --version | Out-Null
    Write-Host "Docker encontrado" -ForegroundColor Green
} catch {
    Write-Host "Docker nao encontrado. Instale Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check Node.js
try {
    node --version | Out-Null
    Write-Host "Node.js encontrado" -ForegroundColor Green
} catch {
    Write-Host "Node.js nao encontrado. Instale Node.js 18+." -ForegroundColor Red
    exit 1
}

Write-Host "Pre-requisitos atendidos" -ForegroundColor Green

# Start infrastructure
Write-Host "Iniciando infraestrutura..." -ForegroundColor Yellow
docker-compose up -d order-db query-db rabbitmq redis

# Setup frontend
Write-Host "Configurando frontend..." -ForegroundColor Yellow
Set-Location frontend
npm install
npm run build
Set-Location ..

# Create mock API
npm install express cors --prefix frontend --save-dev

$mockServerContent = @'
import express from 'express';
import cors from 'cors';

const app = express();

app.use(cors());
app.use(express.json());

const mockOrders = [
    { orderId: '1', customerId: 'cust-001', status: 'CONFIRMED', totalAmount: 299.99 },
    { orderId: '2', customerId: 'cust-002', status: 'PENDING', totalAmount: 150.50 }
];

const mockMetrics = {
    totalOrders: 156,
    totalRevenue: 45678.90,
    pendingOrders: 23
};

app.get('/api/orders', (req, res) => res.json(mockOrders));
app.post('/api/orders', (req, res) => {
    const newOrder = { orderId: Date.now().toString(), ...req.body, status: 'PENDING' };
    mockOrders.push(newOrder);
    res.json(newOrder);
});
app.get('/api/payments', (req, res) => res.json([]));
app.get('/api/inventory', (req, res) => res.json([]));
app.get('/api/dashboard/metrics', (req, res) => res.json(mockMetrics));

app.listen(8080, () => console.log('Mock API running on :8080'));
'@

$mockServerContent | Out-File -FilePath "frontend/mock-server.js" -Encoding UTF8

# Create start script
$startScriptContent = @'
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
'@

$startScriptContent | Out-File -FilePath "start-dev.ps1" -Encoding UTF8

Write-Host ""
Write-Host "SETUP COMPLETO!" -ForegroundColor Green
Write-Host ""
Write-Host "Para iniciar: .\start-dev.ps1" -ForegroundColor Cyan
Write-Host "Frontend: http://localhost:3000" -ForegroundColor Cyan
Write-Host "API Mock: http://localhost:8080" -ForegroundColor Cyan 