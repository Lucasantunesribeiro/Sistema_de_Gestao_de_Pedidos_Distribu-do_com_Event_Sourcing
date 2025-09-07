import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { ThemeProvider } from '@/components/theme-provider'
import { ToastProvider } from '@/components/toast-provider'
import { AppShell } from '@/components/app-shell'
import { Dashboard } from '@/pages/dashboard'
import { Orders } from '@/pages/orders'
import { Payments } from '@/pages/payments'
import { Inventory } from '@/pages/inventory'

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes (formerly cacheTime)
      retry: 3,
      refetchOnWindowFocus: false,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider attribute="class" defaultTheme="system" storageKey="orderflow-ui-theme">
        <Router>
          <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/10 font-inter antialiased">
            <Routes>
              <Route path="/" element={<AppShell />}>
                <Route index element={<Dashboard />} />
                <Route path="orders" element={<Orders />} />
                <Route path="payments" element={<Payments />} />
                <Route path="inventory" element={<Inventory />} />
                <Route path="reports" element={<div className="p-6">Relatórios em desenvolvimento...</div>} />
                <Route path="customers" element={<div className="p-6">Clientes em desenvolvimento...</div>} />
                <Route path="settings" element={<div className="p-6">Configurações em desenvolvimento...</div>} />
              </Route>
            </Routes>
            <ToastProvider />
          </div>
        </Router>
        <ReactQueryDevtools initialIsOpen={false} />
      </ThemeProvider>
    </QueryClientProvider>
  )
}

export default App
