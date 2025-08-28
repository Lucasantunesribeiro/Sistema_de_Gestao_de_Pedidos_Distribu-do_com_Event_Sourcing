# 🎨 Mockups de Alta Fidelidade - Sistema de Gestão de Pedidos v2.0

## **Paleta de Cores Premium**

### **Primary Brand**
- **Primary-500**: `#0891b2` (Azul petróleo confiável)
- **Primary-600**: `#0e7490` (Hover states)
- **Primary-50**: `#f0f9ff` (Backgrounds sutis)

### **Secondary Accent**
- **Secondary-500**: `#f97316` (Laranja energético para CTAs)
- **Secondary-600**: `#ea580c` (Hover states)
- **Secondary-50**: `#fff7ed` (Backgrounds sutis)

### **Neutrals Equilibrados**
- **Neutral-0**: `#ffffff` (Backgrounds principais)
- **Neutral-50**: `#f8fafc` (Backgrounds alternativos)
- **Neutral-900**: `#0f172a` (Texto principal)
- **Neutral-600**: `#475569` (Texto secundário)

---

## **1. 🔐 Tela de Login - Premium Design**

### **Mobile (375x812px)**

```css
/* Layout Specifications */
.login-mobile {
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  padding: 2rem 1.5rem;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-card {
  background: #ffffff;
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.logo {
  font-size: 1.5rem;
  font-weight: 700;
  color: #0891b2;
  text-align: center;
  margin-bottom: 2rem;
}

.input-group {
  margin-bottom: 1.5rem;
}

.input-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #334155;
  margin-bottom: 0.5rem;
}

.input-field {
  width: 100%;
  height: 3rem;
  padding: 0.75rem 1rem;
  border: 2px solid #e2e8f0;
  border-radius: 0.75rem;
  font-size: 1rem;
  transition: all 200ms cubic-bezier(0.2, 0.9, 0.3, 1);
}

.input-field:focus {
  border-color: #0891b2;
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.1);
  outline: none;
}

.button-primary {
  width: 100%;
  height: 3rem;
  background: linear-gradient(135deg, #0891b2 0%, #0e7490 100%);
  color: #ffffff;
  border: none;
  border-radius: 0.75rem;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms cubic-bezier(0.2, 0.9, 0.3, 1);
  margin-top: 1rem;
}

.button-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(8, 145, 178, 0.3);
}
```

**Visual Description:**
- Fundo com gradiente sutil branco→cinza claro
- Card central com sombra premium e bordas arredondadas
- Campos de input com estados focus elegantes
- Botão principal com gradiente e micro-animação hover
- Logo em azul petróleo com tipografia Inter Bold

### **Desktop (1440x1024px)**

```css
.login-desktop {
  display: grid;
  grid-template-columns: 1fr 480px;
  min-height: 100vh;
}

.login-illustration {
  background: linear-gradient(135deg, #0891b2 0%, #164e63 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.login-pattern {
  position: absolute;
  width: 100%;
  height: 100%;
  background-image: 
    radial-gradient(circle at 25% 25%, rgba(255,255,255,0.1) 2px, transparent 2px),
    radial-gradient(circle at 75% 75%, rgba(255,255,255,0.05) 1px, transparent 1px);
  background-size: 60px 60px, 40px 40px;
}

.login-form-area {
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem;
}

.login-form {
  width: 100%;
  max-width: 360px;
}
```

---

## **2. 📊 Dashboard - Data Visualization Premium**

### **Mobile (375x812px)**

```css
.dashboard-mobile {
  background: #f8fafc;
  padding: 1rem;
}

.dashboard-header {
  background: linear-gradient(135deg, #0891b2 0%, #164e63 100%);
  padding: 1.5rem 1rem;
  border-radius: 1rem;
  margin-bottom: 1.5rem;
  color: #ffffff;
}

.dashboard-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.dashboard-subtitle {
  font-size: 0.875rem;
  opacity: 0.8;
}

.metrics-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.metric-card {
  background: #ffffff;
  padding: 1.5rem 1rem;
  border-radius: 1rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  text-align: center;
}

.metric-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0891b2;
  margin-bottom: 0.25rem;
}

.metric-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.metric-trend {
  font-size: 0.75rem;
  color: #059669;
  margin-top: 0.25rem;
}
```

### **Desktop (1440x1024px)**

```css
.dashboard-desktop {
  display: grid;
  grid-template-columns: 280px 1fr;
  min-height: 100vh;
  background: #f8fafc;
}

.sidebar {
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  padding: 2rem 1rem;
}

.main-content {
  padding: 2rem;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.metric-card-desktop {
  background: #ffffff;
  padding: 2rem;
  border-radius: 1rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  position: relative;
  overflow: hidden;
}

.metric-card-desktop::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #0891b2 0%, #f97316 100%);
}

.chart-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
  margin-top: 2rem;
}

.chart-card {
  background: #ffffff;
  padding: 2rem;
  border-radius: 1rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}
```

---

## **3. 📝 Lista de Pedidos - Data Table Premium**

### **Mobile (375x812px)**

```css
.orders-mobile {
  background: #f8fafc;
  padding: 1rem;
}

.search-section {
  margin-bottom: 1.5rem;
}

.search-input {
  width: 100%;
  height: 3rem;
  padding: 0.75rem 1rem 0.75rem 3rem;
  background: #ffffff;
  border: 2px solid #e2e8f0;
  border-radius: 0.75rem;
  font-size: 1rem;
  position: relative;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2364748b'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z'%3E%3C/path%3E%3C/svg%3E");
  background-size: 1.25rem 1.25rem;
  background-position: 0.75rem center;
  background-repeat: no-repeat;
}

.filter-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  overflow-x: auto;
  padding-bottom: 0.5rem;
}

.filter-tab {
  padding: 0.5rem 1rem;
  background: #ffffff;
  border: 2px solid #e2e8f0;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #64748b;
  white-space: nowrap;
  cursor: pointer;
  transition: all 200ms cubic-bezier(0.2, 0.9, 0.3, 1);
}

.filter-tab.active {
  background: #0891b2;
  border-color: #0891b2;
  color: #ffffff;
}

.order-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 1rem;
  padding: 1.5rem;
  margin-bottom: 1rem;
  position: relative;
  transition: all 200ms cubic-bezier(0.2, 0.9, 0.3, 1);
}

.order-card:hover {
  border-color: #0891b2;
  box-shadow: 0 4px 6px -1px rgba(8, 145, 178, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.order-id {
  font-size: 1rem;
  font-weight: 700;
  color: #0891b2;
}

.order-status {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.status-pending {
  background: #fef3c7;
  color: #b45309;
}

.status-completed {
  background: #d1fae5;
  color: #047857;
}

.status-failed {
  background: #fee2e2;
  color: #b91c1c;
}
```

### **Desktop (1440x1024px)**

```css
.orders-desktop {
  padding: 2rem;
  background: #f8fafc;
}

.orders-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  background: #ffffff;
  padding: 1.5rem 2rem;
  border-radius: 1rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}

.orders-table {
  background: #ffffff;
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}

.table-header {
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  padding: 1rem 2rem;
  border-bottom: 2px solid #e2e8f0;
}

.table-row {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid #f1f5f9;
  transition: all 200ms cubic-bezier(0.2, 0.9, 0.3, 1);
}

.table-row:hover {
  background: #f8fafc;
}

.table-cell {
  display: flex;
  align-items: center;
  font-size: 0.875rem;
}

.table-cell-header {
  font-weight: 600;
  color: #334155;
  text-transform: uppercase;
  letter-spacing: 0.025em;
  font-size: 0.75rem;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.action-button {
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #64748b;
  cursor: pointer;
  transition: all 200ms cubic-bezier(0.2, 0.9, 0.3, 1);
}

.action-button:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  transform: scale(1.05);
}
```

---

## **4. 🔍 Detalhe do Pedido - Information Architecture**

### **Mobile (375x812px)**

```css
.order-detail-mobile {
  background: #f8fafc;
  min-height: 100vh;
}

.order-detail-header {
  background: linear-gradient(135deg, #0891b2 0%, #164e63 100%);
  padding: 1rem;
  color: #ffffff;
  position: sticky;
  top: 0;
  z-index: 20;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 1rem;
  cursor: pointer;
}

.order-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.order-detail-content {
  padding: 1.5rem 1rem;
}

.info-section {
  background: #ffffff;
  border-radius: 1rem;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.timeline-step {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.timeline-dot {
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 9999px;
  flex-shrink: 0;
}

.timeline-dot.completed {
  background: #10b981;
  box-shadow: 0 0 0 3px #d1fae5;
}

.timeline-dot.current {
  background: #f97316;
  box-shadow: 0 0 0 3px #fed7aa;
}

.timeline-dot.pending {
  background: #cbd5e1;
  box-shadow: 0 0 0 3px #f1f5f9;
}

.timeline-content {
  flex: 1;
}

.timeline-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: #334155;
}

.timeline-time {
  font-size: 0.75rem;
  color: #64748b;
}
```

### **Desktop (1440x1024px)**

```css
.order-detail-desktop {
  display: grid;
  grid-template-columns: 280px 1fr;
  min-height: 100vh;
  background: #f8fafc;
}

.order-detail-main {
  padding: 2rem;
}

.order-detail-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
}

.order-info-card {
  background: #ffffff;
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  height: fit-content;
}

.order-timeline-card {
  background: #ffffff;
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  height: fit-content;
}

.order-items-table {
  margin-top: 1.5rem;
}

.items-table-header {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 1rem;
  padding: 1rem 0;
  border-bottom: 2px solid #f1f5f9;
  font-weight: 600;
  color: #334155;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.items-table-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 1rem;
  padding: 1rem 0;
  border-bottom: 1px solid #f1f5f9;
  align-items: center;
}

.action-buttons-desktop {
  display: flex;
  gap: 1rem;
  margin-top: 2rem;
}

.button-secondary {
  padding: 0.75rem 1.5rem;
  background: #ffffff;
  border: 2px solid #e2e8f0;
  color: #64748b;
  border-radius: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 200ms cubic-bezier(0.2, 0.9, 0.3, 1);
}

.button-secondary:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}
```

---

## **5. 👥 Tela de Clientes - Cards Layout**

### **Mobile & Desktop Specifications**

```css
.clients-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
  padding: 2rem;
}

.client-card {
  background: #ffffff;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  transition: all 200ms cubic-bezier(0.2, 0.9, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.client-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #0891b2 0%, #f97316 100%);
}

.client-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  border-color: #0891b2;
}

.client-avatar {
  width: 3rem;
  height: 3rem;
  border-radius: 9999px;
  background: linear-gradient(135deg, #0891b2 0%, #164e63 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-weight: 700;
  font-size: 1.25rem;
  margin-bottom: 1rem;
}

.client-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 0.25rem;
}

.client-email {
  font-size: 0.875rem;
  color: #64748b;
  margin-bottom: 1rem;
}

.client-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.client-stat {
  text-align: center;
}

.client-stat-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: #0891b2;
}

.client-stat-label {
  font-size: 0.75rem;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}
```

---

## **Tema Escuro (Dark Mode)**

```css
/* Dark Mode Specifications */
.dark {
  --bg-primary: #0f172a;
  --bg-secondary: #1e293b;
  --bg-tertiary: #334155;
  --text-primary: #f8fafc;
  --text-secondary: #cbd5e1;
  --text-muted: #64748b;
  --border-primary: #334155;
  --border-secondary: #475569;
}

.dark .card {
  background: var(--bg-secondary);
  border-color: var(--border-primary);
  color: var(--text-primary);
}

.dark .input-field {
  background: var(--bg-tertiary);
  border-color: var(--border-primary);
  color: var(--text-primary);
}

.dark .input-field:focus {
  border-color: #0891b2;
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.2);
}

.dark .sidebar {
  background: var(--bg-secondary);
  border-color: var(--border-primary);
}

.dark .table-row:hover {
  background: var(--bg-tertiary);
}
```

---

## **Micro-interações e Animações**

```css
/* Loading States */
.skeleton {
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

@keyframes loading {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* Hover Effects */
.interactive-element {
  transition: all 200ms cubic-bezier(0.2, 0.9, 0.3, 1);
}

.interactive-element:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

/* Focus States for Accessibility */
.focusable:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.3);
  border-radius: 0.375rem;
}

/* Toast Notifications */
.toast {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 0.75rem;
  padding: 1rem 1.5rem;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  animation: slideIn 300ms cubic-bezier(0.2, 0.9, 0.3, 1);
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
```

---

**🎨 Especificações completas para implementação de interfaces premium com foco em usabilidade e experiência visual de alta qualidade.**