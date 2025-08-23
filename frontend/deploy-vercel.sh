#!/bin/bash

# Deploy Frontend para Vercel - Sistema de Gestão de Pedidos
set -e

echo "🚀 Preparando deploy para Vercel..."

# Verificar se estamos no diretório correto
if [ ! -f "package.json" ]; then
    echo "❌ Erro: Execute este script no diretório frontend/"
    exit 1
fi

# Verificar se a CLI do Vercel está instalada
if ! command -v vercel &> /dev/null; then
    echo "📦 Instalando Vercel CLI..."
    npm install -g vercel
fi

# Build local para testar
echo "🏗️ Testando build local..."
npm run build

if [ -d "dist" ]; then
    echo "✅ Build local bem-sucedida!"
    echo "📊 Tamanho da build:"
    du -sh dist/
else
    echo "❌ Erro na build local"
    exit 1
fi

echo ""
echo "🌐 Pronto para deploy no Vercel!"
echo ""
echo "🔧 Para fazer deploy:"
echo "1. Execute: vercel login"
echo "2. Execute: vercel --prod"
echo ""
echo "🎯 Ou configure deploy automático:"
echo "1. Conecte o repositório no painel do Vercel"
echo "2. Configure as variáveis de ambiente:"
echo "   - VITE_API_URL=https://gestao-de-pedidos.onrender.com"
echo "   - VITE_ENV=production"
echo ""
echo "📝 O arquivo vercel.json já está configurado!"