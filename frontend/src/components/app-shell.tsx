import { useState } from "react"
import { Outlet } from "react-router-dom"
import { Sidebar } from "@/components/layout/sidebar"
import { Header } from "@/components/layout/header"

interface AppShellProps {
  children?: React.ReactNode
}

export function AppShell({ children }: AppShellProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const toggleSidebar = () => setSidebarOpen((open) => !open)

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-muted/20 to-muted">
      <Sidebar isOpen={sidebarOpen} onToggle={toggleSidebar} />
      <div className="lg:pl-64">
        <Header onMenuClick={toggleSidebar} />
        <main className="mt-16 px-6 py-4 min-h-screen container mx-auto">
          {children ?? <Outlet />}
        </main>
      </div>
    </div>
  )
}
