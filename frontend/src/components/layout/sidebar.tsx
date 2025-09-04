import { Link, useLocation } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { cn } from '@/lib/utils'
import { 
  LayoutDashboard, 
  ShoppingCart, 
  CreditCard, 
  Package,
  Settings,
  X,
  ChevronLeft,
  ChevronRight,
  BarChart3,
  Users,
  Bell,
  Search,
  LogOut,
  User
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { 
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

interface SidebarProps {
  isOpen: boolean
  onToggle: () => void
}

const navigation = [
  {
    name: 'Dashboard',
    href: '/',
    icon: LayoutDashboard,
    badge: null,
    description: 'Visão geral do sistema'
  },
  {
    name: 'Pedidos',
    href: '/orders',
    icon: ShoppingCart,
    badge: '12',
    description: 'Gerenciar pedidos'
  },
  {
    name: 'Pagamentos',
    href: '/payments',
    icon: CreditCard,
    badge: '3',
    description: 'Controle de pagamentos'
  },
  {
    name: 'Estoque',
    href: '/inventory',
    icon: Package,
    badge: null,
    description: 'Controle de estoque'
  },
  {
    name: 'Relatórios',
    href: '/reports',
    icon: BarChart3,
    badge: null,
    description: 'Relatórios e análises'
  },
  {
    name: 'Clientes',
    href: '/customers',
    icon: Users,
    badge: null,
    description: 'Gestão de clientes'
  },
]

const bottomNavigation = [
  {
    name: 'Configurações',
    href: '/settings',
    icon: Settings,
    description: 'Configurações do sistema'
  },
]

export function Sidebar({ isOpen, onToggle }: SidebarProps) {
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  const toggleCollapsed = () => {
    setCollapsed(!collapsed)
  }

  if (!mounted) {
    return null
  }

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm lg:hidden"
          onClick={onToggle}
        />
      )}

      {/* Sidebar */}
      <div className={cn(
        "fixed inset-y-0 left-0 z-50 flex flex-col bg-sidebar text-sidebar-foreground border-r border-sidebar-border shadow-xl transition-all duration-300 ease-in-out lg:translate-x-0 lg:static lg:inset-0",
        isOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
        collapsed ? "w-16" : "w-64"
      )}>
        {/* Header */}
        <div className="flex items-center justify-between h-16 px-4 border-b border-sidebar-border bg-sidebar">
          <div className={cn(
            "flex items-center transition-opacity duration-200",
            collapsed ? "opacity-0" : "opacity-100"
          )}>
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 rounded-lg bg-sidebar-primary flex items-center justify-center">
                <LayoutDashboard className="h-4 w-4 text-sidebar-primary-foreground" />
              </div>
              <div>
                <h1 className="text-lg font-bold">OrderFlow</h1>
                <p className="text-xs text-sidebar-foreground/70">Enterprise</p>
              </div>
            </div>
          </div>
          
          {/* Mobile close button */}
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggle}
            className="lg:hidden text-sidebar-foreground hover:bg-sidebar-accent"
          >
            <X className="h-5 w-5" />
          </Button>

          {/* Desktop collapse button */}
          <Button
            variant="ghost"
            size="icon"
            onClick={toggleCollapsed}
            className={cn(
              "hidden lg:flex text-sidebar-foreground hover:bg-sidebar-accent transition-transform duration-200",
              collapsed && "rotate-180"
            )}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
        </div>

        {/* User Profile */}
        <div className="p-4 border-b border-sidebar-border">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                className={cn(
                  "w-full justify-start p-2 h-auto hover:bg-sidebar-accent transition-all duration-200",
                  collapsed && "px-2"
                )}
              >
                <Avatar className="h-8 w-8">
                  <AvatarImage src="https://placehold.co/32x32" />
                  <AvatarFallback className="bg-sidebar-primary text-sidebar-primary-foreground text-sm">
                    AD
                  </AvatarFallback>
                </Avatar>
                {!collapsed && (
                  <div className="ml-3 text-left flex-1">
                    <p className="text-sm font-medium">Admin User</p>
                    <p className="text-xs text-sidebar-foreground/70">admin@orderflow.com</p>
                  </div>
                )}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>Minha Conta</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem>
                <User className="mr-2 h-4 w-4" />
                Perfil
              </DropdownMenuItem>
              <DropdownMenuItem>
                <Bell className="mr-2 h-4 w-4" />
                Notificações
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem className="text-destructive">
                <LogOut className="mr-2 h-4 w-4" />
                Sair
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-2 space-y-1 overflow-y-auto">
          <div className="space-y-1">
            {navigation.map((item) => {
              const isActive = location.pathname === item.href
              const Icon = item.icon
              
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={cn(
                    "group flex items-center px-3 py-2.5 text-sm font-medium rounded-lg transition-all duration-200 relative",
                    isActive
                      ? "bg-sidebar-primary text-sidebar-primary-foreground shadow-lg shadow-sidebar-primary/20"
                      : "text-sidebar-foreground/80 hover:text-sidebar-foreground hover:bg-sidebar-accent"
                  )}
                  onClick={() => {
                    // Close sidebar on mobile after navigation
                    if (window.innerWidth < 1024) {
                      onToggle()
                    }
                  }}
                  title={collapsed ? item.name : undefined}
                >
                  {/* Active indicator */}
                  {isActive && (
                    <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-sidebar-primary-foreground rounded-r-full" />
                  )}
                  
                  <Icon className={cn(
                    "flex-shrink-0 h-5 w-5 transition-transform duration-200",
                    isActive ? "scale-110" : "group-hover:scale-105",
                    collapsed ? "mx-auto" : "mr-3"
                  )} />
                  
                  {!collapsed && (
                    <>
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <span>{item.name}</span>
                          {item.badge && (
                            <Badge 
                              variant={isActive ? "secondary" : "outline"} 
                              className="ml-2 h-5 text-xs"
                            >
                              {item.badge}
                            </Badge>
                          )}
                        </div>
                        {!isActive && (
                          <p className="text-xs text-sidebar-foreground/60 mt-0.5 group-hover:text-sidebar-foreground/80 transition-colors">
                            {item.description}
                          </p>
                        )}
                      </div>
                    </>
                  )}
                </Link>
              )
            })}
          </div>
        </nav>

        {/* Bottom Navigation */}
        <div className="p-2 border-t border-sidebar-border">
          {bottomNavigation.map((item) => {
            const isActive = location.pathname === item.href
            const Icon = item.icon
            
            return (
              <Link
                key={item.name}
                to={item.href}
                className={cn(
                  "group flex items-center px-3 py-2.5 text-sm font-medium rounded-lg transition-all duration-200",
                  isActive
                    ? "bg-sidebar-primary text-sidebar-primary-foreground"
                    : "text-sidebar-foreground/80 hover:text-sidebar-foreground hover:bg-sidebar-accent"
                )}
                onClick={() => {
                  if (window.innerWidth < 1024) {
                    onToggle()
                  }
                }}
                title={collapsed ? item.name : undefined}
              >
                <Icon className={cn(
                  "flex-shrink-0 h-5 w-5 transition-transform duration-200 group-hover:scale-105",
                  collapsed ? "mx-auto" : "mr-3"
                )} />
                
                {!collapsed && (
                  <div className="flex-1">
                    <span>{item.name}</span>
                    <p className="text-xs text-sidebar-foreground/60 mt-0.5 group-hover:text-sidebar-foreground/80 transition-colors">
                      {item.description}
                    </p>
                  </div>
                )}
              </Link>
            )
          })}
        </div>
      </div>
    </>
  )
}