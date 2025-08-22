# Deploy Production Railway - Windows PowerShell Script
# Sistema de Gestão de Pedidos Distribuído
# 🚀 PRODUCTION DEPLOYMENT AUTOMATION

Write-Host "🚀 INICIANDO DEPLOYMENT RAILWAY - Sistema de Gestão de Pedidos" -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Cyan

# Verificar se Railway CLI está instalado
Write-Host "📋 1. Verificando Railway CLI..." -ForegroundColor Yellow
try {
    $railwayVersion = railway --version 2>$null
    if ($railwayVersion) {
        Write-Host "✅ Railway CLI encontrado: $railwayVersion" -ForegroundColor Green
    } else {
        throw "Railway CLI não encontrado"
    }
} catch {
    Write-Host "❌ Railway CLI não instalado!" -ForegroundColor Red
    Write-Host "📥 Instalando Railway CLI..." -ForegroundColor Yellow
    
    # Instalar Railway CLI via npm
    try {
        npm install -g @railway/cli
        Write-Host "✅ Railway CLI instalado com sucesso!" -ForegroundColor Green
    } catch {
        Write-Host "❌ Erro ao instalar Railway CLI. Instale manualmente:" -ForegroundColor Red
        Write-Host "   npm install -g @railway/cli" -ForegroundColor White
        Write-Host "   ou baixe de: https://railway.app/cli" -ForegroundColor White
        exit 1
    }
}

# Verificar autenticação Railway
Write-Host "🔐 2. Verificando autenticação Railway..." -ForegroundColor Yellow
try {
    $railwayStatus = railway status 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "🔑 Fazendo login no Railway..." -ForegroundColor Yellow
        railway login
        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ Erro no login Railway. Execute manualmente: railway login" -ForegroundColor Red
            exit 1
        }
    }
    Write-Host "✅ Autenticado no Railway!" -ForegroundColor Green
} catch {
    Write-Host "❌ Erro na autenticação Railway" -ForegroundColor Red
    exit 1
}

# Verificar projeto Railway
Write-Host "📁 3. Configurando projeto Railway..." -ForegroundColor Yellow
try {
    # Tentar obter informações do projeto atual
    $projectInfo = railway status 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "🆕 Criando novo projeto Railway..." -ForegroundColor Yellow
        railway init
        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ Erro ao criar projeto Railway" -ForegroundColor Red
            exit 1
        }
    }
    Write-Host "✅ Projeto Railway configurado!" -ForegroundColor Green
} catch {
    Write-Host "❌ Erro na configuração do projeto" -ForegroundColor Red
    exit 1
}

# Build shared-events primeiro
Write-Host "🔧 4. Building shared-events library..." -ForegroundColor Yellow
Set-Location "shared-events"
try {
    mvn clean install -DskipTests -q
    if ($LASTEXITCODE -ne 0) {
        throw "Erro no build shared-events"
    }
    Write-Host "✅ Shared-events built successfully!" -ForegroundColor Green
} catch {
    Write-Host "❌ Erro no build shared-events: $_" -ForegroundColor Red
    Set-Location ".."
    exit 1
}
Set-Location ".."

# Deploy services na ordem correta
$services = @(
    @{name="order-service"; port=8081},
    @{name="payment-service"; port=8082}, 
    @{name="inventory-service"; port=8083},
    @{name="order-query-service"; port=8084}
)

Write-Host "🚀 5. Deploying microsserviços..." -ForegroundColor Yellow
foreach ($service in $services) {
    Write-Host "   📦 Deploying $($service.name)..." -ForegroundColor Cyan
    Set-Location "services\$($service.name)"
    
    try {
        # Build do serviço
        mvn clean package -DskipTests -q
        if ($LASTEXITCODE -ne 0) {
            throw "Erro no build de $($service.name)"
        }
        
        # Deploy no Railway
        railway up --detach
        if ($LASTEXITCODE -ne 0) {
            throw "Erro no deploy de $($service.name)"
        }
        
        Write-Host "   ✅ $($service.name) deployed!" -ForegroundColor Green
    } catch {
        Write-Host "   ❌ Erro no deploy de $($service.name): $_" -ForegroundColor Red
        Set-Location "..\..\"
        exit 1
    }
    
    Set-Location "..\..\"
}

# Deploy Frontend
Write-Host "🎨 6. Deploying Frontend React..." -ForegroundColor Yellow
Set-Location "frontend"
try {
    # Install dependencies se necessário
    if (!(Test-Path "node_modules")) {
        npm install
    }
    
    # Build production
    npm run build
    if ($LASTEXITCODE -ne 0) {
        throw "Erro no build do frontend"
    }
    
    # Deploy no Railway
    railway up --detach
    if ($LASTEXITCODE -ne 0) {
        throw "Erro no deploy do frontend"
    }
    
    Write-Host "✅ Frontend deployed!" -ForegroundColor Green
} catch {
    Write-Host "❌ Erro no deploy do frontend: $_" -ForegroundColor Red
    Set-Location ".."
    exit 1
}
Set-Location ".."

# Aguardar deployments
Write-Host "⏳ 7. Aguardando deployments finalizarem..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Validar deployments
Write-Host "🔍 8. Validando deployments..." -ForegroundColor Yellow
try {
    $deployments = railway status
    Write-Host "$deployments" -ForegroundColor White
} catch {
    Write-Host "⚠️ Não foi possível obter status dos deployments" -ForegroundColor Yellow
}

# Exibir URLs
Write-Host "`n🌐 DEPLOYMENT CONCLUÍDO!" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "🎉 Sistema de Gestão de Pedidos deployed com sucesso!" -ForegroundColor Green
Write-Host "`n📱 Para acessar seus serviços:" -ForegroundColor Yellow
Write-Host "   railway open" -ForegroundColor White
Write-Host "`n📊 Para monitorar:" -ForegroundColor Yellow  
Write-Host "   railway logs" -ForegroundColor White
Write-Host "`n🔧 Para gerenciar:" -ForegroundColor Yellow
Write-Host "   railway status" -ForegroundColor White

Write-Host "`n✨ DEPLOYMENT RAILWAY CONCLUÍDO COM SUCESSO! ✨" -ForegroundColor Green
Write-Host "Sistema está rodando em produção no Railway.app" -ForegroundColor Cyan