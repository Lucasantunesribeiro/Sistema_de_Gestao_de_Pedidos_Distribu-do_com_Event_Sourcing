import * as React from 'react'
import { useLocation, Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { ThemeToggle } from '@/components/ui/theme-toggle'
import { 
  Menu, 
  Bell, 
  Search, 
  Command, 
  ChevronRight, 
  Home,
  Users,
  TrendingUp,
  FileText,
  HelpCircle,
  Settings,
  User,
  LogOut
} from 'lucide-react'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { cn } from '@/lib/utils'

interface HeaderProps {
  onMenuClick: () => void
  onSearch?: (query: string) => void
  notifications?: number
  user?: {
    name?: string
    email?: string
    avatar?: string
  }
}

interface BreadcrumbItem {
  label: string
  href?: string
  icon?: React.ElementType
}

const getBreadcrumbs = (pathname: string): BreadcrumbItem[] => {
  const pathMap: Record<string, BreadcrumbItem[]> = {
    '/': [{ label: 'Dashboard', icon: Home }],
    '/orders': [
      { label: 'Dashboard', href: '/', icon: Home },
      { label: 'Pedidos' }
    ],
    '/payments': [
      { label: 'Dashboard', href: '/', icon: Home },
      { label: 'Pagamentos' }
    ],
    '/inventory': [
      { label: 'Dashboard', href: '/', icon: Home },
      { label: 'Estoque' }
    ],
    '/reports': [
      { label: 'Dashboard', href: '/', icon: Home },
      { label: 'Relatórios' }
    ],
    '/customers': [
      { label: 'Dashboard', href: '/', icon: Home },
      { label: 'Clientes' }
    ],
    '/settings': [
      { label: 'Dashboard', href: '/', icon: Home },
      { label: 'Configurações' }
    ],
  }
  
  return pathMap[pathname] || [{ label: 'Dashboard', icon: Home }]
}

export function Header({ 
  onMenuClick, 
  onSearch, 
  notifications = 3,
  user = { 
    name: "Admin User", 
    email: "admin@orderflow.com",
    avatar: "/api/placeholder/32/32" 
  }
}: HeaderProps) {
  const location = useLocation()
  const [searchQuery, setSearchQuery] = React.useState('')
  const [isSearchFocused, setIsSearchFocused] = React.useState(false)
  const [showMobileSearch, setShowMobileSearch] = React.useState(false)
  
  const breadcrumbs = getBreadcrumbs(location.pathname)
  
  const handleSearch = React.useCallback((query: string) => {
    if (onSearch) {
      onSearch(query.trim())
    }
  }, [onSearch])

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    handleSearch(searchQuery)
  }

  const handleSearchKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      setSearchQuery('')
      setIsSearchFocused(false)
      if (showMobileSearch) {
        setShowMobileSearch(false)
      }
    }
  }

  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        setIsSearchFocused(true)
        setShowMobileSearch(true)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [])

  return (
    <>
      {/* Main Header */}
      <header 
        className="fixed top-0 right-0 left-0 lg:left-64 z-40 h-16 bg-background/95 backdrop-blur-md border-b border-border/60 shadow-sm"
        role="banner"
      >
        <div className="flex items-center justify-between h-full px-4 lg:px-6">
          {/* Left Section - Mobile Menu + Breadcrumbs */}
          <div className="flex items-center flex-1 min-w-0 gap-4">
            <Button
              variant="ghost"
              size="icon"
              onClick={onMenuClick}
              className="lg:hidden shrink-0 hover:bg-accent"
              aria-label="Abrir menu lateral"
            >
              <Menu className="h-5 w-5" />
            </Button>
            
            {/* Breadcrumbs */}
            <nav 
              className="hidden sm:flex items-center text-sm text-muted-foreground min-w-0" 
              aria-label="Breadcrumb"
            >
              <ol className="flex items-center space-x-2 min-w-0">
                {breadcrumbs.map((item, index) => {
                  const isLast = index === breadcrumbs.length - 1
                  const Icon = item.icon
                  
                  return (
                    <li key={index} className="flex items-center min-w-0">
                      {index > 0 && (
                        <ChevronRight className="h-4 w-4 mx-2 text-muted-foreground/50" />
                      )}
                      <div className="flex items-center min-w-0">
                        {Icon && <Icon className="h-4 w-4 mr-2 shrink-0" />}
                        {item.href && !isLast ? (
                          <Link
                            to={item.href}
                            className="hover:text-foreground transition-colors truncate"
                          >
                            {item.label}
                          </Link>
                        ) : (
                          <span 
                            className={cn(
                              "truncate",
                              isLast && "text-foreground font-medium"
                            )}
                          >
                            {item.label}
                          </span>
                        )}
                      </div>
                    </li>
                  )
                })}
              </ol>
            </nav>

            {/* Mobile title */}
            <h1 className="sm:hidden text-lg font-semibold text-foreground truncate">
              {breadcrumbs[breadcrumbs.length - 1]?.label || 'OrderFlow'}
            </h1>
          </div>

          {/* Center Section - Desktop Search */}
          <div className="hidden md:flex mx-6 max-w-md flex-1">
            <form onSubmit={handleSearchSubmit} className="relative w-full">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  type="search"
                  placeholder="Buscar pedidos, produtos... (⌘K)"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onFocus={() => setIsSearchFocused(true)}
                  onBlur={() => setIsSearchFocused(false)}
                  onKeyDown={handleSearchKeyDown}
                  className={cn(
                    "w-full pl-10 pr-12 transition-all duration-200",
                    isSearchFocused && "ring-2 ring-primary/20 border-primary/50"
                  )}
                  aria-label="Campo de busca"
                />
                <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1 pointer-events-none">
                  <kbd className="inline-flex items-center rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">
                    <Command className="h-3 w-3 mr-0.5" />
                    K
                  </kbd>
                </div>
              </div>
            </form>
          </div>

          {/* Right Section - Actions */}
          <div className="flex items-center gap-2">
            {/* Mobile Search */}
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden"
              onClick={() => setShowMobileSearch(true)}
              aria-label="Abrir busca"
            >
              <Search className="h-4 w-4" />
            </Button>

            {/* Theme Toggle */}
            <ThemeToggle />

            {/* Quick Actions */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="hover:bg-accent"
                >
                  <TrendingUp className="h-4 w-4" />
                  <span className="sr-only">Ações rápidas</span>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuLabel>Ações Rápidas</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem>
                  <FileText className="mr-2 h-4 w-4" />
                  Novo Pedido
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <Users className="mr-2 h-4 w-4" />
                  Cadastrar Cliente
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <TrendingUp className="mr-2 h-4 w-4" />
                  Ver Relatórios
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>

            {/* Notifications */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  className="relative hover:bg-accent"
                  aria-label={`Notificações${notifications > 0 ? ` (${notifications})` : ''}`}
                >
                  <Bell className="h-4 w-4" />
                  {notifications > 0 && (
                    <Badge 
                      className="absolute -top-1 -right-1 h-5 w-5 rounded-full p-0 flex items-center justify-center text-xs bg-destructive text-destructive-foreground border-2 border-background"
                    >
                      {notifications > 99 ? '99+' : notifications}
                    </Badge>
                  )}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-80">
                <DropdownMenuLabel>Notificações</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <div className="max-h-[300px] overflow-y-auto">
                  <DropdownMenuItem className="flex flex-col items-start p-4">
                    <div className="flex items-start space-x-3 w-full">
                      <div className="w-2 h-2 bg-blue-500 rounded-full mt-2 flex-shrink-0"></div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium">Novo pedido recebido</p>
                        <p className="text-xs text-muted-foreground mt-1">
                          Pedido #1234 foi criado por João Silva
                        </p>
                        <p className="text-xs text-muted-foreground mt-1">há 5 minutos</p>
                      </div>
                    </div>
                  </DropdownMenuItem>
                  <DropdownMenuItem className="flex flex-col items-start p-4">
                    <div className="flex items-start space-x-3 w-full">
                      <div className="w-2 h-2 bg-green-500 rounded-full mt-2 flex-shrink-0"></div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium">Pagamento aprovado</p>
                        <p className="text-xs text-muted-foreground mt-1">
                          Pagamento do pedido #1233 foi processado
                        </p>
                        <p className="text-xs text-muted-foreground mt-1">há 15 minutos</p>
                      </div>
                    </div>
                  </DropdownMenuItem>
                  <DropdownMenuItem className="flex flex-col items-start p-4">
                    <div className="flex items-start space-x-3 w-full">
                      <div className="w-2 h-2 bg-yellow-500 rounded-full mt-2 flex-shrink-0"></div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium">Estoque baixo</p>
                        <p className="text-xs text-muted-foreground mt-1">
                          Produto XYZ tem apenas 5 unidades em estoque
                        </p>
                        <p className="text-xs text-muted-foreground mt-1">há 1 hora</p>
                      </div>
                    </div>
                  </DropdownMenuItem>
                </div>
                <DropdownMenuSeparator />
                <DropdownMenuItem className="text-center py-2">
                  Ver todas as notificações
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>

            {/* User menu */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button 
                  variant="ghost" 
                  className="h-9 px-2 hover:bg-accent"
                  aria-label="Menu do usuário"
                >
                  <Avatar className="h-7 w-7">
                    <AvatarImage src={user.avatar} alt={`Avatar de ${user.name}`} />
                    <AvatarFallback className="text-xs">
                      {user.name?.slice(0, 2).toUpperCase() || 'AU'}
                    </AvatarFallback>
                  </Avatar>
                  <span className="ml-2 text-sm hidden lg:block">
                    {user.name}
                  </span>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent 
                align="end" 
                className="w-56"
                sideOffset={8}
              >
                <DropdownMenuLabel>
                  <div className="flex flex-col space-y-1">
                    <p className="text-sm font-medium">{user.name}</p>
                    <p className="text-xs text-muted-foreground">{user.email}</p>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem>
                  <User className="mr-2 h-4 w-4" />
                  Meu Perfil
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <Settings className="mr-2 h-4 w-4" />
                  Configurações
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <HelpCircle className="mr-2 h-4 w-4" />
                  Ajuda & Suporte
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem className="text-destructive focus:text-destructive">
                  <LogOut className="mr-2 h-4 w-4" />
                  Sair
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </header>

      {/* Mobile Search Overlay */}
      {showMobileSearch && (
        <div className="fixed inset-0 bg-background/95 backdrop-blur-sm z-50 md:hidden">
          <div className="p-4 border-b">
            <form onSubmit={handleSearchSubmit} className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                placeholder="Buscar pedidos, produtos..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={handleSearchKeyDown}
                autoFocus
                className="pl-10 pr-12"
              />
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="absolute right-2 top-1/2 -translate-y-1/2"
                onClick={() => setShowMobileSearch(false)}
                aria-label="Fechar busca"
              >
                ×
              </Button>
            </form>
          </div>
          <div 
            className="flex-1" 
            onClick={() => setShowMobileSearch(false)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                setShowMobileSearch(false)
              }
            }}
            aria-label="Fechar busca"
          />
        </div>
      )}
    </>
  )
}