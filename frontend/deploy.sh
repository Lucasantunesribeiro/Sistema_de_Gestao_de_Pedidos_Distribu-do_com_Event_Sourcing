#!/bin/bash

# Script de Deploy Frontend - Sistema de Gestao de Pedidos
# Automatiza o deploy para Netlify

set -e

echo "🚀 Iniciando deploy do frontend..."

# Verificar se estamos no diretorio correto
if [ ! -f "package.json" ]; then
    echo "❌ Erro: Execute este script no diretorio frontend/"
    exit 1
fi

# Instalar dependencias
echo "📦 Instalando dependencias..."
npm ci --silent

# Type checking
echo "🔍 Verificando tipos TypeScript..."
npm run type-check

# Lint
echo "✨ Verificando codigo com ESLint..."
npm run lint

# Build
echo "🏗️ Fazendo build para producao..."
npm run build

echo "✅ Build concluida! Arquivos em ./dist"

# Mostrar tamanho dos arquivos
if [ -d "dist" ]; then
    echo "📊 Estatisticas da build:"
    du -sh dist/
fi

echo ""
echo "🌐 Pronto para deploy no Netlify!"
echo "💡 Configure as variaveis de ambiente no painel do Netlify"
echo "   - VITE_API_URL=https://gestao-de-pedidos.onrender.com"