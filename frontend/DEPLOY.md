# 🚀 Guia de Deploy Frontend

## Deploy no Netlify (Recomendado)

### 1. Conectar Repositório
1. Acesse [netlify.com](https://netlify.com)
2. Clique em "New site from Git"
3. Conecte seu repositório GitHub

### 2. Configurar Build
```
Build command: npm ci && npm run build
Publish directory: frontend/dist
Base directory: frontend
```

### 3. Variáveis de Ambiente
No painel do Netlify, configure:
```
VITE_API_URL = https://gestao-de-pedidos.onrender.com
VITE_ENV = production
```

### 4. Deploy Automático
- Push na branch `main` = deploy automático
- Pull requests = deploy preview
- O `netlify.toml` já está configurado

## Deploy Manual (Alternativo)

### 1. Instalar dependências
```bash
cd frontend
npm install
```

### 2. Build
```bash
npm run build
```

### 3. Deploy com Netlify CLI
```bash
# Instalar CLI
npm install -g netlify-cli

# Login
netlify login

# Deploy
netlify deploy --prod --dir=dist
```

## ✅ Frontend Finalizado

### Features Implementadas:
- ✅ Dashboard com métricas em tempo real
- ✅ Gestão completa de pedidos
- ✅ Event Sourcing visualization
- ✅ Integração com backend Render
- ✅ UI responsiva com shadcn/ui
- ✅ TypeScript + React 18
- ✅ Configuração de deploy Netlify

### Arquivos de Deploy:
- ✅ `netlify.toml` - Configuração Netlify
- ✅ `.env.example` - Variáveis de ambiente
- ✅ `README.md` - Documentação completa
- ✅ Build otimizada para produção

## 🌐 URLs
- **Backend API**: https://gestao-de-pedidos.onrender.com
- **Frontend**: Será definida após deploy no Netlify

## 🎯 Próximo Passo
Faça o deploy no Netlify seguindo os passos acima para ter o sistema completo funcionando!