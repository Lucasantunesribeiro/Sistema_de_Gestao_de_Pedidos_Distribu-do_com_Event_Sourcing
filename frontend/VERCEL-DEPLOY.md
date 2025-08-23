# 🚀 Deploy Frontend no Vercel

## Método 1: Deploy Automático (Recomendado)

### 1. Conectar Repositório
1. Acesse [vercel.com](https://vercel.com)
2. Clique em "New Project"
3. Conecte seu repositório GitHub
4. Selecione o repositório do projeto

### 2. Configurar Deploy
```
Framework Preset: Vite
Root Directory: frontend
Build Command: npm run build
Output Directory: dist
Install Command: npm ci
```

### 3. Variáveis de Ambiente
No painel do Vercel, configure:
```
VITE_API_URL = https://gestao-de-pedidos.onrender.com
VITE_ENV = production
```

### 4. Deploy
- Push na branch `main` = deploy automático
- Pull requests = deploy preview
- O `vercel.json` já está configurado

## Método 2: Deploy Manual via CLI

### 1. Instalar Vercel CLI
```bash
npm install -g vercel
```

### 2. Login
```bash
vercel login
```

### 3. Configurar Projeto
```bash
cd frontend
vercel
```

### 4. Deploy para Produção
```bash
vercel --prod
```

## 📋 Arquivos de Configuração

### vercel.json
- ✅ SPA routing configurado
- ✅ Headers de segurança
- ✅ Cache optimization
- ✅ Environment variables

### package.json
- ✅ Scripts de build otimizados
- ✅ Dependências corretas
- ✅ Build command para Vercel

## 🔧 Configurações Avançadas

### Variáveis por Ambiente
```bash
# Produção
vercel env add VITE_API_URL production

# Preview
vercel env add VITE_API_URL preview

# Development
vercel env add VITE_API_URL development
```

### Custom Domains
1. Acesse o painel do Vercel
2. Vá em Settings > Domains
3. Adicione seu domínio personalizado

## ✅ Checklist Deploy

- ✅ `vercel.json` configurado
- ✅ Variáveis de ambiente definidas
- ✅ Build testada localmente
- ✅ Repositório conectado
- ✅ Deploy automático ativo

## 🌐 URLs Geradas

Após o deploy, você receberá:
- **URL de produção**: https://sistema-gestao-pedidos-frontend.vercel.app
- **URLs de preview**: Para cada PR/branch
- **Backend API**: https://gestao-de-pedidos.onrender.com

## 🎯 Vantagens do Vercel

- ⚡ Deploy instantâneo
- 🌍 CDN global
- 🔄 Deploy previews automáticos  
- 📊 Analytics integrado
- 🚀 Edge Functions support
- 💰 Plano gratuito generoso

## 🔧 Troubleshooting

### Build Errors
```bash
# Local test
npm run build

# Check logs
vercel logs <deployment-url>
```

### Environment Variables
```bash
# List variables
vercel env ls

# Pull variables locally  
vercel env pull .env.local
```

## 🎉 Deploy Completo!

O sistema agora está rodando:
- **Frontend**: Vercel (otimizado)
- **Backend**: Render (4 microsserviços)
- **Integração**: API REST completa