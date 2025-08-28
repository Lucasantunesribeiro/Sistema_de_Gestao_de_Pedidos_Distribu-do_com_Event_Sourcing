import * as React from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Menu, Bell, User, Search, Command } from 'lucide-react'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'

interface HeaderProps {
  onMenuClick: () => void
  onSearch?: (query: string) => void
  notifications?: number
  user?: {
    name?: string
    avatar?: string
  }
}

export function Header({ 
  onMenuClick, 
  onSearch, 
  notifications = 0,
  user = { name: "Usuário" }
}: HeaderProps) {
  const [searchQuery, setSearchQuery] = React.useState('')
  const [isSearchFocused, setIsSearchFocused] = React.useState(false)
  const [showMobileSearch, setShowMobileSearch] = React.useState(false)
  
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
    <header 
      className="fixed top-0 right-0 left-0 lg:left-64 z-40 h-16 bg-background/80 backdrop-blur-md border-b border-border/60 shadow-sm"
      role="banner"
    >
      <div className="flex items-center justify-between h-full px-4 md:px-6">
        {/* Left section */}
        <div className="flex items-center flex-1 min-w-0">
          <Button
            variant="ghost"
            size="icon"
            onClick={onMenuClick}
            className="lg:hidden shrink-0 hover:bg-accent"
            aria-label="Abrir menu lateral"
          >
            <Menu className="h-5 w-5" />
          </Button>
          
          <div className="hidden sm:block ml-4 lg:ml-0 min-w-0">
            <h1 className="text-lg font-semibold text-foreground truncate">
              Sistema de Gestão de Pedidos
            </h1>
          </div>

          {/* Desktop Search */}
          <div className="hidden md:flex ml-8 max-w-md flex-1">
            <form onSubmit={handleSearchSubmit} className="relative w-full">
              <Input
                type="search"
                placeholder="Buscar pedidos, produtos... (⌘K)"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => setIsSearchFocused(true)}
                onBlur={() => setIsSearchFocused(false)}
                onKeyDown={handleSearchKeyDown}
                className={`w-full transition-all duration-200 ${
                  isSearchFocused ? 'shadow-md' : ''
                }`}
                aria-label="Campo de busca"
              />
              <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1 pointer-events-none">
                <Command className="h-3 w-3 text-muted-foreground" />
                <span className="text-xs text-muted-foreground">K</span>
              </div>
            </form>
          </div>
        </div>

        {/* Right section */}
        <div className="flex items-center gap-2 md:gap-4">
          {/* Mobile Search Button */}
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            onClick={() => setShowMobileSearch(true)}
            aria-label="Abrir busca"
          >
            <Search className="h-5 w-5" />
          </Button>

          {/* Notifications */}
          <Button 
            variant="ghost" 
            size="icon" 
            className="relative hover:bg-accent"
            aria-label={`Notificações${notifications > 0 ? ` (${notifications})` : ''}`}
          >
            <Bell className="h-5 w-5" />
            {notifications > 0 && (
              <Badge 
                className="absolute -top-1 -right-1 h-5 w-5 rounded-full p-0 flex items-center justify-center text-xs bg-red-500 text-white border-2 border-background"
                aria-hidden="true"
              >
                {notifications > 99 ? '99+' : notifications}
              </Badge>
            )}
          </Button>

          {/* User menu */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button 
                variant="ghost" 
                size="icon"
                className="hover:bg-accent"
                aria-label="Menu do usuário"
              >
                {user.avatar ? (
                  <img 
                    src={user.avatar} 
                    alt={`Avatar de ${user.name}`}
                    className="h-5 w-5 rounded-full"
                  />
                ) : (
                  <User className="h-5 w-5" />
                )}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent 
              align="end" 
              className="w-56"
              sideOffset={8}
            >
              <DropdownMenuLabel>
                {user.name || "Minha Conta"}
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem className="cursor-pointer">
                Perfil
              </DropdownMenuItem>
              <DropdownMenuItem className="cursor-pointer">
                Configurações
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem className="cursor-pointer text-red-600 focus:text-red-600 focus:bg-red-50 dark:focus:bg-red-950/20">
                Sair
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {/* Mobile Search Overlay */}
      {showMobileSearch && (
        <div className="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 md:hidden">
          <div className="p-4 border-b">
            <form onSubmit={handleSearchSubmit} className="relative">
              <Input
                type="search"
                placeholder="Buscar pedidos, produtos..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={handleSearchKeyDown}
                autoFocus
                className="pr-12"
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
    </header>
  )
}