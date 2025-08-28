import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { ThemeProvider } from '@/lib/theme'
import { Toaster } from '@/components/ui/toaster'
import { Layout } from '@/components/layout/layout'
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
      <ThemeProvider defaultTheme="system" storageKey="orderflow-ui-theme">
        <Router>
          <div className="min-h-screen bg-background font-inter antialiased">
            <Routes>
              <Route path="/" element={<Layout />}>
                <Route index element={<Dashboard />} />
                <Route path="orders" element={<Orders />} />
                <Route path="payments" element={<Payments />} />
                <Route path="inventory" element={<Inventory />} />
                <Route path="reports" element={<div className="p-6">Relatórios em desenvolvimento...</div>} />
                <Route path="customers" element={<div className="p-6">Clientes em desenvolvimento...</div>} />
                <Route path="settings" element={<div className="p-6">Configurações em desenvolvimento...</div>} />
              </Route>
            </Routes>
            <Toaster />
          </div>
        </Router>
        <ReactQueryDevtools initialIsOpen={false} />
      </ThemeProvider>
    </QueryClientProvider>
  )
}

export default App