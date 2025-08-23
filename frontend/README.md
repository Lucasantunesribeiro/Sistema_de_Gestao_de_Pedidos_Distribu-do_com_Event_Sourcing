# Frontend - Sistema de Gestão de Pedidos

Interface web moderna para o sistema distribuído de gestão de pedidos com Event Sourcing.

## ✨ Features

- **Dashboard** em tempo real com métricas de negócio
- **Gestão de Pedidos** com Event Sourcing visualization
- **Histórico de Eventos** completo e auditável
- **Design System** baseado em shadcn/ui
- **TypeScript** para type safety
- **React Query** para data fetching otimizado

## 🚀 Tecnologias

- **React 18** + TypeScript
- **Vite** para bundling ultrarrápido
- **TailwindCSS** + shadcn/ui
- **TanStack Query** para state management
- **React Router** para navegação
- **Axios** para HTTP requests

## 📦 Instalação

```bash
# Clone e acesse o diretório
cd frontend

# Instale as dependências
npm install

# Configure as variáveis de ambiente
cp .env.example .env.local
```

## 🛠️ Desenvolvimento

```bash
# Servidor de desenvolvimento
npm run dev

# Build para produção
npm run build

# Preview da build
npm run preview

# Testes
npm run test
npm run test:ui
npm run test:coverage

# Code quality
npm run lint
npm run lint:fix
npm run type-check
npm run format
```

## 🌐 Deploy

### Deploy no Vercel (Recomendado)

1. **Conecte o repositório** no painel do Vercel
2. **Configure as settings**:
   - Framework: Vite
   - Root Directory: `frontend`
   - Build Command: `npm run build`
   - Output Directory: `dist`
3. **Variáveis de ambiente**:
   - `VITE_API_URL=https://gestao-de-pedidos.onrender.com`

📋 **Guia completo**: `VERCEL-DEPLOY.md`

### Deploy Manual via CLI

```bash
# Instalar Vercel CLI
npm install -g vercel

# Deploy
cd frontend
vercel --prod
```

### Deploy Alternativo (Netlify)

```bash
# Execute o script de deploy
./deploy.sh

# Ou manualmente
npm run build
netlify deploy --prod --dir=dist
```

## 📊 Estrutura do Projeto

```
frontend/
├── src/
│   ├── components/        # Componentes reutilizáveis
│   │   ├── ui/           # shadcn/ui components
│   │   └── layout/       # Layout components
│   ├── pages/            # Páginas da aplicação
│   │   ├── dashboard.tsx
│   │   ├── orders.tsx
│   │   ├── payments.tsx
│   │   └── inventory.tsx
│   ├── lib/              # Utilitários
│   │   ├── api.ts        # Client HTTP
│   │   └── utils.ts      # Helpers
│   ├── types/            # TypeScript types
│   └── test/             # Configurações de teste
├── public/               # Assets estáticos
├── netlify.toml         # Configuração Netlify
└── package.json
```

## 🔗 Integração com Backend

O frontend se conecta com o backend distribuído através de:

- **REST API** para operações CRUD
- **Event Sourcing** para histórico de eventos
- **Real-time updates** via polling automático
- **Error handling** com retry automático

### Endpoints Utilizados

```
GET  /api/orders           # Lista pedidos
POST /api/orders           # Cria pedido
GET  /api/orders/:id       # Detalhes do pedido
GET  /api/orders/:id/events # Eventos do pedido
PUT  /api/orders/:id/status # Atualiza status

GET  /health              # Health check
GET  /                    # System info
```

## 🎨 Design System

Baseado no **shadcn/ui** com:

- **Componentes** acessíveis e customizáveis
- **Dark/Light mode** suporte
- **Responsive design** mobile-first
- **Consistent styling** com Tailwind CSS

## 📈 Performance

- **Code splitting** automático
- **Lazy loading** de rotas
- **Optimized bundles** < 1MB
- **Caching strategy** inteligente
- **Fast refresh** em desenvolvimento

## 🧪 Testes

- **Unit tests** com Vitest
- **Component tests** com Testing Library
- **Coverage** > 80%
- **Type checking** com TypeScript

## 🔧 Configurações

### Variáveis de Ambiente

```env
# Produção
VITE_API_URL=https://gestao-de-pedidos.onrender.com

# Desenvolvimento
VITE_API_URL=http://localhost:8080

# Features opcionais
VITE_ENABLE_DEVTOOLS=true
VITE_LOG_LEVEL=info
```

### Netlify Deploy

O deploy no Netlify é configurado via `netlify.toml`:

- **Build command**: `npm ci && npm run build`
- **Publish directory**: `dist`
- **Redirects**: SPA handling
- **Headers**: Security headers
- **Environment**: Variáveis por contexto

## 📝 Scripts Disponíveis

| Script | Descrição |
|--------|-----------|
| `npm run dev` | Servidor de desenvolvimento |
| `npm run build` | Build para produção |
| `npm run preview` | Preview da build |
| `npm run test` | Executa testes |
| `npm run lint` | Verifica código |
| `npm run type-check` | Verifica tipos |
| `./deploy.sh` | Script completo de deploy |

## 🌟 Features Implementadas

### Dashboard
- ✅ Métricas em tempo real
- ✅ Gráficos de pedidos
- ✅ Status dos serviços
- ✅ Pedidos recentes

### Gestão de Pedidos
- ✅ Lista completa de pedidos
- ✅ Filtros e busca avançada
- ✅ Criação de novos pedidos
- ✅ Visualização de detalhes
- ✅ Histórico de eventos (Event Sourcing)
- ✅ Atualização de status

### Pagamentos
- ✅ Lista de pagamentos
- ✅ Status de processamento
- ✅ Integração com pedidos

### Inventário
- ✅ Controle de estoque
- ✅ Produtos disponíveis

## 🎯 Próximos Passos

- [ ] Notificações em tempo real
- [ ] Export de dados
- [ ] Relatórios avançados
- [ ] PWA support
- [ ] Offline mode