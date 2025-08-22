#!/bin/bash

# Validation Script - Railway Deployment Readiness
# Verifica se todos os arquivos estão prontos para deployment

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[✅]${NC} $1"; }
log_error() { echo -e "${RED}[❌]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[⚠️]${NC} $1"; }

# Validation counters
passed=0
failed=0

validate_file() {
    local file=$1
    local description=$2
    
    if [ -f "$file" ]; then
        log_success "$description: $file"
        ((passed++))
    else
        log_error "$description: $file (MISSING)"
        ((failed++))
    fi
}

validate_directory() {
    local dir=$1
    local description=$2
    
    if [ -d "$dir" ]; then
        log_success "$description: $dir"
        ((passed++))
    else
        log_error "$description: $dir (MISSING)"
        ((failed++))
    fi
}

echo -e "${BLUE}"
echo "🔍 VALIDAÇÃO DE DEPLOYMENT - Railway.app"
echo "========================================"
echo -e "${NC}"

echo "📋 1. Verificando estrutura do projeto..."

# Project structure
validate_file "pom.xml" "POM principal"
validate_directory "services" "Diretório de serviços"
validate_directory "frontend" "Diretório frontend"
validate_directory "shared-events" "Biblioteca compartilhada"

echo ""
echo "📋 2. Verificando configurações Railway..."

# Railway configurations
validate_file "railway.json" "Configuração Railway principal"
validate_file "deploy-production.sh" "Script de deployment"
validate_file "RAILWAY_DEPLOYMENT_COMPLETE.md" "Documentação completa"

echo ""
echo "📋 3. Verificando microsserviços..."

# Services
services=("order-service" "payment-service" "inventory-service" "order-query-service")

for service in "${services[@]}"; do
    echo ""
    log_info "Validando $service..."
    
    validate_file "services/$service/railway.json" "Railway config"
    validate_file "services/$service/Dockerfile" "Dockerfile"
    validate_file "services/$service/pom.xml" "Maven POM"
    validate_file "services/$service/src/main/resources/application-railway.yml" "Perfil Railway"
    validate_file "services/$service/src/main/resources/application.yml" "Configuração principal"
done

echo ""
echo "📋 4. Verificando frontend..."

validate_file "frontend/package.json" "Package.json"
validate_file "frontend/Dockerfile" "Dockerfile frontend"
validate_file "frontend/railway.json" "Railway config frontend"
validate_file "frontend/.env.production" "Environment production"

echo ""
echo "📋 5. Verificando shared-events..."

validate_file "shared-events/pom.xml" "Shared events POM"
validate_directory "shared-events/src" "Shared events source"

echo ""
echo "📋 6. Verificando dependências..."

# Check Railway CLI
if command -v railway &> /dev/null; then
    log_success "Railway CLI instalado: $(railway --version)"
    ((passed++))
else
    log_error "Railway CLI não encontrado"
    ((failed++))
fi

# Check Maven
if command -v mvn &> /dev/null; then
    log_success "Maven encontrado: $(mvn --version | head -1)"
    ((passed++))
else
    log_error "Maven não encontrado"
    ((failed++))
fi

# Check Node.js
if command -v node &> /dev/null; then
    log_success "Node.js encontrado: $(node --version)"
    ((passed++))
else
    log_error "Node.js não encontrado"
    ((failed++))
fi

echo ""
echo "📋 7. Verificando build readiness..."

# Check if shared-events can be built
if [ -f "shared-events/pom.xml" ]; then
    cd shared-events
    if mvn validate &> /dev/null; then
        log_success "Shared-events: POM válido"
        ((passed++))
    else
        log_error "Shared-events: POM inválido"
        ((failed++))
    fi
    cd ..
fi

# Check services POM validity
for service in "${services[@]}"; do
    if [ -f "services/$service/pom.xml" ]; then
        cd "services/$service"
        if mvn validate &> /dev/null; then
            log_success "$service: POM válido"
            ((passed++))
        else
            log_error "$service: POM inválido"
            ((failed++))
        fi
        cd - > /dev/null
    fi
done

# Check frontend package.json
if [ -f "frontend/package.json" ]; then
    cd frontend
    if npm ls &> /dev/null; then
        log_success "Frontend: package.json válido"
        ((passed++))
    else
        log_warning "Frontend: pode precisar de npm install"
        ((passed++))
    fi
    cd ..
fi

echo ""
echo "📋 8. Resumo da validação..."
echo ""

total=$((passed + failed))
percentage=$((passed * 100 / total))

if [ $failed -eq 0 ]; then
    echo -e "${GREEN}🎉 VALIDAÇÃO COMPLETA!${NC}"
    echo -e "${GREEN}✅ $passed/$total verificações passaram ($percentage%)${NC}"
    echo ""
    echo -e "${BLUE}🚀 PRONTO PARA DEPLOYMENT!${NC}"
    echo ""
    echo "Próximos passos:"
    echo "1. railway login"
    echo "2. ./deploy-production.sh"
    echo ""
elif [ $failed -le 2 ]; then
    echo -e "${YELLOW}⚠️ VALIDAÇÃO QUASE COMPLETA${NC}"
    echo -e "${YELLOW}✅ $passed/$total verificações passaram ($percentage%)${NC}"
    echo -e "${YELLOW}❌ $failed verificações falharam${NC}"
    echo ""
    echo -e "${YELLOW}🔧 CORRIJA OS PROBLEMAS MENORES E PROSSIGA${NC}"
else
    echo -e "${RED}❌ VALIDAÇÃO FALHOU${NC}"
    echo -e "${RED}✅ $passed/$total verificações passaram ($percentage%)${NC}"
    echo -e "${RED}❌ $failed verificações falharam${NC}"
    echo ""
    echo -e "${RED}🛑 CORRIJA OS PROBLEMAS ANTES DO DEPLOYMENT${NC}"
fi

echo ""
echo "📊 Status dos componentes:"
echo "   🎯 Microsserviços: $(ls services/ | wc -l) configurados"
echo "   🌐 Frontend: React + TypeScript"
echo "   🗄️ Infraestrutura: PostgreSQL + Redis + RabbitMQ"
echo "   📦 Build: Maven + Docker"
echo ""

exit $failed