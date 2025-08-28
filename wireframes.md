# 📐 Wireframes de Baixa Fidelidade

## **Fluxo de Navegação Principal**
```
Login → Dashboard → [Orders, Payments, Inventory, Settings] → Detail Views
```

---

## **1. Tela de Login** 📱💻

### **Mobile (375px)**
```
┌─────────────────────────────────────┐
│  [Logo] Sistema de Gestão           │
│                                     │
│  ┌─────────────────────────────────┐ │
│  │ 📧 Email ou usuário             │ │
│  └─────────────────────────────────┘ │
│                                     │
│  ┌─────────────────────────────────┐ │
│  │ 🔒 Senha                        │ │
│  └─────────────────────────────────┘ │
│                                     │
│  ☐ Lembrar de mim                   │
│                                     │
│  ┌─────────────────────────────────┐ │
│  │        ENTRAR                   │ │
│  └─────────────────────────────────┘ │
│                                     │
│  Esqueceu a senha?                  │
│                                     │
│  ─────── ou ───────                 │
│                                     │
│  Criar nova conta                   │
│                                     │
└─────────────────────────────────────┘
```

### **Desktop (1440px)**
```
┌─────────────────────┬─────────────────────┐
│                     │                     │
│    [Ilustração      │   [Logo Empresa]    │
│     ou Pattern]     │                     │
│                     │   Sistema de Gestão │
│                     │   de Pedidos        │
│                     │                     │
│                     │ ┌─────────────────┐ │
│                     │ │ Email/Username  │ │
│                     │ └─────────────────┘ │
│                     │                     │
│                     │ ┌─────────────────┐ │
│                     │ │ Senha           │ │
│                     │ └─────────────────┘ │
│                     │                     │
│                     │ ☐ Manter conectado  │
│                     │                     │
│                     │ [    ENTRAR    ]    │
│                     │                     │
│                     │ Esqueceu a senha?   │
│                     │                     │
└─────────────────────┴─────────────────────┘
```

---

## **2. Dashboard Principal** 📊

### **Mobile (375px)**
```
┌─────────────────────────────────────┐
│ [≡] Dashboard        [🔔3] [👤]    │
├─────────────────────────────────────┤
│                                     │
│ ┌─────────┐ ┌─────────┐             │
│ │   156   │ │ R$42.3K │             │
│ │ Pedidos │ │ Receita │             │
│ └─────────┘ └─────────┘             │
│ ┌─────────┐ ┌─────────┐             │
│ │  94.2%  │ │   12    │             │
│ │Taxa Con.│ │Pendente │             │
│ └─────────┘ └─────────┘             │
│                                     │
│ ▸ Pedidos Recentes                  │
│ ┌─────────────────────────────────┐ │
│ │ #1234 - João Silva         R$89 │ │
│ │ ● PROCESSANDO                   │ │
│ ├─────────────────────────────────┤ │
│ │ #1235 - Maria Costa       R$156 │ │
│ │ ● CONCLUÍDO                     │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ▸ Status do Sistema                 │
│ ┌─────────────────────────────────┐ │
│ │ ● Order Service      [ONLINE]   │ │
│ │ ● Payment Service    [ONLINE]   │ │
│ │ ● Inventory Service  [ONLINE]   │ │
│ └─────────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

### **Desktop (1440px)**
```
┌─────┬───────────────────────────────────────────────────────┐
│[≡]  │ Dashboard                     [🔍 Buscar] [🔔3] [👤] │
├─────┼───────────────────────────────────────────────────────┤
│📊   │                                                       │
│Dash │ ┌──────────┐┌──────────┐┌──────────┐┌──────────┐      │
│     │ │   1,247  ││  R$89.2K ││  96.8%   ││    23    │      │
│📦   │ │  Pedidos ││  Receita ││Taxa Conv.││ Pendente │      │
│Order│ └──────────┘└──────────┘└──────────┘└──────────┘      │
│     │                                                       │
│💳   │ ┌─────────────────────┐ ┌─────────────────────┐       │
│Pay  │ │ ▸ Pedidos Recentes  │ │ ▸ Status Sistema    │       │
│     │ │                     │ │                     │       │
│📋   │ │ #1234 João    R$89  │ │ ● Order    [ONLINE] │       │
│Inv  │ │ ● PROCESSANDO       │ │ ● Payment  [ONLINE] │       │
│     │ │ #1235 Maria  R$156  │ │ ● Inventory[ONLINE] │       │
│⚙️   │ │ ● CONCLUÍDO         │ │ ● Query    [ONLINE] │       │
│Set  │ │ #1236 Carlos R$234  │ │                     │       │
│     │ │ ● PENDENTE          │ │ [Ver Detalhes]      │       │
│     │ └─────────────────────┘ └─────────────────────┘       │
│     │                                                       │
│     │ ┌─────────────────────────────────────────────────┐   │
│     │ │        [Gráfico de Pedidos - Últimos 7 dias]   │   │
│     │ │   ▄▄                                            │   │
│     │ │ ▄████▄      ▄▄▄                                 │   │
│     │ │ ██████████████████                              │   │
│     │ └─────────────────────────────────────────────────┘   │
└─────┴───────────────────────────────────────────────────────┘
```

---

## **3. Lista de Pedidos** 📝

### **Mobile (375px)**
```
┌─────────────────────────────────────┐
│ [≡] Pedidos          [🔔3] [👤]    │
├─────────────────────────────────────┤
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 🔍 Buscar pedidos...            │ │
│ └─────────────────────────────────┘ │
│                                     │
│ [Todos] [Pendente] [Concluído]      │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ #1234              [●] PENDING  │ │
│ │ João Silva - R$89,50            │ │
│ │ 2 itens • há 2h                 │ │
│ │                       [Ver >]   │ │
│ ├─────────────────────────────────┤ │
│ │ #1235              [●] COMPLETE │ │
│ │ Maria Costa - R$156,00          │ │
│ │ 1 item • há 4h                  │ │
│ │                       [Ver >]   │ │
│ ├─────────────────────────────────┤ │
│ │ #1236              [●] FAILED   │ │
│ │ Carlos Mendes - R$234,90        │ │
│ │ 3 itens • há 6h                 │ │
│ │                       [Ver >]   │ │
│ └─────────────────────────────────┘ │
│                                     │
│ [Carregar mais...]                  │
│                                     │
└─────────────────────────────────────┘
```

### **Desktop (1440px)**
```
┌─────┬───────────────────────────────────────────────────────┐
│[≡]  │ Pedidos                       [🔍 Buscar] [🔔3] [👤] │
├─────┼───────────────────────────────────────────────────────┤
│📊   │                                                       │
│Dash │ ┌─────────────────────────────────────────────────┐   │
│     │ │ 🔍 Buscar por ID, cliente, produto...          │   │
│📦   │ └─────────────────────────────────────────────────┘   │
│Order│                                                       │
│     │ [🗸 Selecionar Todos] [Filtros ▼] [Exportar ↓]       │
│💳   │                                                       │
│Pay  │ ┌──┬──────┬─────────────┬──────────┬───────┬────────┐ │
│     │ │☐ │ ID   │ Cliente     │ Total    │Status │ Ações  │ │
│📋   │ ├──┼──────┼─────────────┼──────────┼───────┼────────┤ │
│Inv  │ │☐ │1234  │João Silva   │ R$89,50  │●PEND  │👁 ✏️ 🗑│ │
│     │ │☐ │1235  │Maria Costa  │ R$156,00 │●DONE  │👁 ✏️ 🗑│ │
│⚙️   │ │☐ │1236  │Carlos Mendes│ R$234,90 │●FAIL  │👁 ✏️ 🗑│ │
│Set  │ │☐ │1237  │Ana Pereira  │ R$67,30  │●PROC  │👁 ✏️ 🗑│ │
│     │ └──┴──────┴─────────────┴──────────┴───────┴────────┘ │
│     │                                                       │
│     │ ← Página 1 de 47                    [Próximo →]      │
│     │                                                       │
└─────┴───────────────────────────────────────────────────────┘
```

---

## **4. Detalhe do Pedido** 🔍

### **Mobile (375px)**
```
┌─────────────────────────────────────┐
│ [<] Pedido #1234     [🔔3] [👤]    │
├─────────────────────────────────────┤
│                                     │
│ ● PROCESSANDO           [Atualizar] │
│ João Silva                          │
│ joao.silva@email.com                │
│ +55 11 99999-9999                   │
│                                     │
│ ▸ Itens do Pedido                   │
│ ┌─────────────────────────────────┐ │
│ │ Produto A    2x    R$25,00      │ │
│ │ Produto B    1x    R$39,50      │ │
│ │ ─────────────────────────────── │ │
│ │ Subtotal:          R$89,50      │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ▸ Timeline                          │
│ ┌─────────────────────────────────┐ │
│ │ ● Criado        14:30           │ │
│ │ ● Confirmado    14:35           │ │
│ │ ○ Processando   15:00           │ │
│ │ ○ Enviado       -               │ │
│ │ ○ Entregue      -               │ │
│ └─────────────────────────────────┘ │
│                                     │
│ [Cancelar Pedido] [Processar]       │
│                                     │
└─────────────────────────────────────┘
```

### **Desktop (1440px)**
```
┌─────┬───────────────────────────────────────────────────────┐
│[≡]  │ [← Pedidos] Pedido #1234      [🔍 Buscar] [🔔3] [👤] │
├─────┼───────────────────────────────────────────────────────┤
│📊   │                                                       │
│Dash │ ┌──────────────────────┐ ┌─────────────────────────┐  │
│     │ │ ℹ️ Informações       │ │ 📦 Itens do Pedido      │  │
│📦   │ │                      │ │                         │  │
│Order│ │ Status: ●PROCESSANDO │ │ ┌─────────────────────┐ │  │
│     │ │                      │ │ │Produto A  2x R$25  │ │  │
│💳   │ │ Cliente:             │ │ │Produto B  1x R$39,5│ │  │
│Pay  │ │ João Silva           │ │ │─────────────────────│ │  │
│     │ │ joao.silva@email.com │ │ │Subtotal:    R$89,50│ │  │
│📋   │ │ +55 11 99999-9999    │ │ └─────────────────────┘ │  │
│Inv  │ │                      │ └─────────────────────────┘  │
│     │ │ Criado: 27/08 14:30  │                              │
│⚙️   │ │ Valor: R$89,50       │ ┌─────────────────────────┐  │
│Set  │ └──────────────────────┘ │ 🕒 Timeline             │  │
│     │                          │                         │  │
│     │ ┌──────────────────────┐ │ ● 14:30 Pedido criado   │  │
│     │ │ 🗂️ Ações             │ │ ● 14:35 Pagamento conf. │  │
│     │ │                      │ │ ● 15:00 Em processament │  │
│     │ │ [Processar Pedido]   │ │ ○       Enviado         │  │
│     │ │ [Cancelar Pedido]    │ │ ○       Entregue        │  │
│     │ │ [Enviar Notificação] │ └─────────────────────────┘  │
│     │ │ [Exportar PDF]       │                              │
│     │ └──────────────────────┘                              │
└─────┴───────────────────────────────────────────────────────┘
```

---

## **5. Tela de Clientes** 👥

### **Mobile (375px)**
```
┌─────────────────────────────────────┐
│ [≡] Clientes         [🔔3] [👤]    │
├─────────────────────────────────────┤
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 🔍 Buscar clientes...           │ │
│ └─────────────────────────────────┘ │
│                                     │
│ [+] Novo Cliente                    │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 👤 João Silva                   │ │
│ │ joao.silva@email.com            │ │
│ │ 5 pedidos • R$450 total         │ │
│ │                       [Ver >]   │ │
│ ├─────────────────────────────────┤ │
│ │ 👤 Maria Costa                  │ │
│ │ maria.costa@email.com           │ │
│ │ 3 pedidos • R$320 total         │ │
│ │                       [Ver >]   │ │
│ └─────────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

### **Desktop (1440px)**
```
┌─────┬───────────────────────────────────────────────────────┐
│[≡]  │ Clientes                      [🔍 Buscar] [🔔3] [👤] │
├─────┼───────────────────────────────────────────────────────┤
│📊   │                                                       │
│Dash │ ┌─────────────────────────────────────────────────┐   │
│     │ │ 🔍 Buscar por nome, email, telefone...         │   │
│📦   │ └─────────────────────────────────────────────────┘   │
│Order│                                          [+ Cliente]   │
│     │                                                       │
│💳   │ ┌──┬─────────────┬─────────────────┬─────────┬────────┐│
│Pay  │ │  │ Nome        │ Email           │ Pedidos │ Ações  ││
│     │ ├──┼─────────────┼─────────────────┼─────────┼────────┤│
│📋   │ │👤│João Silva   │joao@email.com   │ 5 (R$450)│👁 ✏️ 🗑││
│Inv  │ │👤│Maria Costa  │maria@email.com  │ 3 (R$320)│👁 ✏️ 🗑││
│     │ │👤│Carlos Mendes│carlos@email.com │ 8 (R$780)│👁 ✏️ 🗑││
│⚙️   │ └──┴─────────────┴─────────────────┴─────────┴────────┘│
│Set  │                                                       │
└─────┴───────────────────────────────────────────────────────┘
```

---

## **6. Tela de Produtos** 📦

### **Mobile (375px)**
```
┌─────────────────────────────────────┐
│ [≡] Produtos         [🔔3] [👤]    │
├─────────────────────────────────────┤
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 🔍 Buscar produtos...           │ │
│ └─────────────────────────────────┘ │
│                                     │
│ [+] Novo Produto                    │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 📦 Produto A                    │ │
│ │ R$25,00 • Estoque: 150          │ │
│ │ SKU: PRD001 • Categoria: X      │ │
│ │                       [Ver >]   │ │
│ ├─────────────────────────────────┤ │
│ │ 📦 Produto B                    │ │
│ │ R$39,50 • Estoque: 89           │ │
│ │ SKU: PRD002 • Categoria: Y      │ │
│ │                       [Ver >]   │ │
│ └─────────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

---

## **7. Configurações** ⚙️

### **Mobile (375px)**
```
┌─────────────────────────────────────┐
│ [≡] Configurações    [🔔3] [👤]    │
├─────────────────────────────────────┤
│                                     │
│ ▸ Perfil                            │
│ ┌─────────────────────────────────┐ │
│ │ 👤 Alterar dados pessoais   >   │ │
│ │ 🔒 Alterar senha            >   │ │
│ │ 📧 Notificações             >   │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ▸ Sistema                           │
│ ┌─────────────────────────────────┐ │
│ │ 🌙 Tema escuro         [○●]     │ │
│ │ 🌐 Idioma: Português        >   │ │
│ │ 🔔 Notificações push   [●○]     │ │
│ │ 💾 Backup automático   [●○]     │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ▸ Integrações                       │
│ ┌─────────────────────────────────┐ │
│ │ 🔌 APIs e Webhooks          >   │ │
│ │ 📊 Google Analytics         >   │ │
│ │ 💳 Gateway de Pagamento     >   │ │
│ └─────────────────────────────────┘ │
│                                     │
│ [Sair da conta]                     │
│                                     │
└─────────────────────────────────────┘
```

---

## **Padrões de Interação**

### **Estados de Loading**
- **Skeleton screens** para carregamentos iniciais
- **Shimmer effects** em cards e listas
- **Spinners inline** para ações específicas
- **Progress bars** para processos longos

### **Estados Vazios**
- **Ilustrações simples** + texto explicativo
- **Call-to-action** principal sempre visível
- **Dicas contextuais** para primeiros passos

### **Feedback de Ações**
- **Toast notifications** para sucesso/erro
- **Inline validation** em formulários
- **Loading states** em botões durante ação
- **Color coding** para status (verde/vermelho/amarelo)

### **Navegação**
- **Breadcrumbs** em desktop para contexto
- **Back button** sempre presente em mobile
- **Tab navigation** para seções relacionadas
- **Sidebar collapse** para maximizar espaço

---

**✅ Wireframes completos para implementação das 8 telas principais + padrões de interação.**