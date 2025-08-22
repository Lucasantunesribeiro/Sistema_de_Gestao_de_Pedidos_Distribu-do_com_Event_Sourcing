# Setup Railway CLI - Windows
# Sistema de Gestão de Pedidos Distribuído

Write-Host "🔧 SETUP RAILWAY CLI - Windows" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan

# Verificar Node.js
Write-Host "📋 Verificando Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>$null
    if ($nodeVersion) {
        Write-Host "✅ Node.js encontrado: $nodeVersion" -ForegroundColor Green
    } else {
        throw "Node.js não encontrado"
    }
} catch {
    Write-Host "❌ Node.js não instalado!" -ForegroundColor Red
    Write-Host "📥 Baixe e instale Node.js de: https://nodejs.org" -ForegroundColor White
    exit 1
}

# Verificar npm
Write-Host "📋 Verificando npm..." -ForegroundColor Yellow
try {
    $npmVersion = npm --version 2>$null
    if ($npmVersion) {
        Write-Host "✅ npm encontrado: $npmVersion" -ForegroundColor Green
    } else {
        throw "npm não encontrado"
    }
} catch {
    Write-Host "❌ npm não encontrado!" -ForegroundColor Red
    exit 1
}

# Instalar Railway CLI
Write-Host "🚀 Instalando Railway CLI..." -ForegroundColor Yellow
try {
    npm install -g @railway/cli
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Railway CLI instalado com sucesso!" -ForegroundColor Green
    } else {
        throw "Erro na instalação"
    }
} catch {
    Write-Host "❌ Erro ao instalar Railway CLI" -ForegroundColor Red
    Write-Host "💡 Tente executar como Administrador ou instale manualmente:" -ForegroundColor Yellow
    Write-Host "   npm install -g @railway/cli" -ForegroundColor White
    exit 1
}

# Verificar instalação
Write-Host "🔍 Verificando instalação..." -ForegroundColor Yellow
try {
    $railwayVersion = railway --version 2>$null
    if ($railwayVersion) {
        Write-Host "✅ Railway CLI instalado: $railwayVersion" -ForegroundColor Green
    } else {
        throw "Railway CLI não funciona"
    }
} catch {
    Write-Host "❌ Railway CLI não está funcionando corretamente" -ForegroundColor Red
    exit 1
}

Write-Host "`n🎉 SETUP CONCLUÍDO!" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "✅ Railway CLI instalado e funcionando!" -ForegroundColor Green
Write-Host "`n🚀 Próximos passos:" -ForegroundColor Yellow
Write-Host "   1. railway login" -ForegroundColor White
Write-Host "   2. .\deploy-windows.ps1" -ForegroundColor White
Write-Host "`n💡 Comandos úteis:" -ForegroundColor Yellow
Write-Host "   railway login     # Fazer login" -ForegroundColor White
Write-Host "   railway init      # Criar projeto" -ForegroundColor White
Write-Host "   railway up        # Deploy" -ForegroundColor White
Write-Host "   railway status    # Ver status" -ForegroundColor White
Write-Host "   railway open      # Abrir dashboard" -ForegroundColor White

Write-Host "`n✨ PRONTO PARA DEPLOY! ✨" -ForegroundColor Green