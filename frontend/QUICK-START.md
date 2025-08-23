# ⚡ Quick Start - Deploy no Vercel

## 🚀 Deploy em 3 Passos

### 1. Clone e Prepare
```bash
git clone <seu-repositorio>
cd frontend
npm install
```

### 2. Teste Local
```bash
npm run build
npm run preview
```

### 3. Deploy no Vercel
```bash
# Via Web (Recomendado)
1. Acesse vercel.com
2. "New Project" → Conecte GitHub
3. Configure:
   - Root: frontend
   - Framework: Vite  
   - Build: npm run build
   - Output: dist
4. Environment Variables:
   - VITE_API_URL=https://gestao-de-pedidos.onrender.com

# Via CLI
npm install -g vercel
vercel login
vercel --prod
```

## ✅ Sistema Completo

- ✅ **Backend**: 4 microsserviços no Render
- ✅ **Frontend**: React + Vite pronto para Vercel
- ✅ **Event Sourcing**: Implementado
- ✅ **UI/UX**: shadcn/ui + TailwindCSS

## 🌐 URLs Finais
- **API**: https://gestao-de-pedidos.onrender.com
- **Frontend**: `<seu-projeto>.vercel.app`

## 🎯 Funcionalidades
- Dashboard com métricas em tempo real
- Gestão completa de pedidos
- Visualização de eventos (Event Sourcing)
- Interface responsiva e moderna