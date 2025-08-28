import * as React from "react"
import { 
  ChevronLeft, 
  ChevronRight, 
  Menu, 
  X,
  User,
  Settings,
  LogOut
} from "lucide-react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"
import { Button } from "./button"

// Types
export interface SidebarItem {
  id: string
  label: string
  icon?: React.ComponentType<{ className?: string }>
  href?: string
  onClick?: () => void
  badge?: string | number
  disabled?: boolean
  children?: SidebarItem[]
}

export interface SidebarSection {
  id: string
  title?: string
  items: SidebarItem[]
}

export interface SidebarUser {
  name: string
  email?: string
  avatar?: string
  role?: string
}

export interface SidebarProps extends VariantProps<typeof sidebarVariants> {
  sections: SidebarSection[]
  user?: SidebarUser
  activeItemId?: string
  collapsed?: boolean
  onCollapsedChange?: (collapsed: boolean) => void
  onItemClick?: (item: SidebarItem) => void
  onUserMenuClick?: (action: 'profile' | 'settings' | 'logout') => void
  className?: string
  mobileBreakpoint?: number
  persistState?: boolean
  storageKey?: string
}

// Variantes do sidebar
const sidebarVariants = cva(
  "flex flex-col bg-background border-r border-border transition-all duration-300 ease-in-out",
  {
    variants: {
      position: {
        left: "left-0",
        right: "right-0 border-l border-r-0",
      },
    },
    defaultVariants: {
      position: "left",
    },
  }
)

// Hook para gerenciar estado do localStorage
const usePersistentState = <T,>(
  key: string,
  defaultValue: T,
  enabled: boolean = true
): [T, React.Dispatch<React.SetStateAction<T>>] => {
  const [state, setState] = React.useState<T>(() => {
    if (!enabled || typeof window === 'undefined') return defaultValue
    
    try {
      const item = localStorage.getItem(key)
      return item ? JSON.parse(item) : defaultValue
    } catch {
      return defaultValue
    }
  })

  React.useEffect(() => {
    if (!enabled || typeof window === 'undefined') return
    
    try {
      localStorage.setItem(key, JSON.stringify(state))
    } catch {
      // Fail silently
    }
  }, [key, state, enabled])

  return [state, setState]
}

// Hook para detectar mobile
const useIsMobile = (breakpoint: number = 768) => {
  const [isMobile, setIsMobile] = React.useState(false)

  React.useEffect(() => {
    const checkIsMobile = () => {
      setIsMobile(window.innerWidth < breakpoint)
    }

    checkIsMobile()
    window.addEventListener('resize', checkIsMobile)
    return () => window.removeEventListener('resize', checkIsMobile)
  }, [breakpoint])

  return isMobile
}

// Context para comunicação entre componentes
interface SidebarContextValue {
  collapsed: boolean
  isMobile: boolean
  activeItemId?: string
  onItemClick: (item: SidebarItem) => void
}

const SidebarContext = React.createContext<SidebarContextValue | null>(null)

const useSidebarContext = () => {
  const context = React.useContext(SidebarContext)
  if (!context) {
    throw new Error("Sidebar components must be used within a Sidebar")
  }
  return context
}

// Sidebar principal
const NavigationSidebar: React.FC<SidebarProps> = ({
  sections,
  user,
  activeItemId,
  collapsed: controlledCollapsed,
  onCollapsedChange,
  onItemClick,
  onUserMenuClick,
  position = "left",
  className,
  mobileBreakpoint = 768,
  persistState = true,
  storageKey = "sidebar-collapsed",
}) => {
  const isMobile = useIsMobile(mobileBreakpoint)
  
  // Estado do collapse (controlado ou internal)
  const [internalCollapsed, setInternalCollapsed] = usePersistentState(
    storageKey,
    false,
    persistState && !isMobile
  )
  
  const collapsed = controlledCollapsed ?? internalCollapsed
  
  // Estado do mobile overlay
  const [mobileOpen, setMobileOpen] = React.useState(false)
  
  const handleCollapsedChange = React.useCallback((newCollapsed: boolean) => {
    if (onCollapsedChange) {
      onCollapsedChange(newCollapsed)
    } else {
      setInternalCollapsed(newCollapsed)
    }
  }, [onCollapsedChange, setInternalCollapsed])
  
  const handleItemClick = React.useCallback((item: SidebarItem) => {
    if (isMobile) {
      setMobileOpen(false)
    }
    onItemClick?.(item)
  }, [isMobile, onItemClick])
  
  const contextValue: SidebarContextValue = React.useMemo(() => ({
    collapsed,
    isMobile,
    activeItemId,
    onItemClick: handleItemClick,
  }), [collapsed, isMobile, activeItemId, handleItemClick])
  
  // Fechar mobile overlay quando ESC é pressionado
  React.useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && mobileOpen) {
        setMobileOpen(false)
      }
    }
    
    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [mobileOpen])
  
  // Prevent body scroll quando mobile overlay está aberto
  React.useEffect(() => {
    if (isMobile && mobileOpen) {
      document.body.style.overflow = 'hidden'
      return () => {
        document.body.style.overflow = ''
      }
    }
  }, [isMobile, mobileOpen])

  const sidebarContent = (
    <SidebarContext.Provider value={contextValue}>
      <div className={cn(
        sidebarVariants({ position }),
        isMobile 
          ? "fixed inset-y-0 z-50 w-64" 
          : collapsed 
            ? "w-16" 
            : "w-64",
        className
      )}>
        {/* Header */}
        <div className="flex items-center justify-between p-4 min-h-[64px]">
          {!collapsed && (
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
                <span className="text-primary-foreground text-sm font-bold">S</span>
              </div>
              <span className="font-semibold text-lg">Sistema</span>
            </div>
          )}
          
          {!isMobile && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleCollapsedChange(!collapsed)}
              className="h-8 w-8 p-0"
              aria-label={collapsed ? "Expandir sidebar" : "Recolher sidebar"}
            >
              {collapsed ? (
                <ChevronRight className="h-4 w-4" />
              ) : (
                <ChevronLeft className="h-4 w-4" />
              )}
            </Button>
          )}
          
          {isMobile && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setMobileOpen(false)}
              className="h-8 w-8 p-0"
              aria-label="Fechar menu"
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
        
        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto px-2 pb-4">
          <div className="space-y-6">
            {sections.map((section) => (
              <SidebarSection key={section.id} section={section} />
            ))}
          </div>
        </nav>
        
        {/* User section */}
        {user && (
          <div className="border-t border-border p-2">
            <SidebarUser 
              user={user} 
              onUserMenuClick={onUserMenuClick}
            />
          </div>
        )}
      </div>
    </SidebarContext.Provider>
  )

  if (isMobile) {
    return (
      <>
        {/* Mobile trigger */}
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setMobileOpen(true)}
          className="fixed top-4 left-4 z-40 md:hidden h-8 w-8 p-0"
          aria-label="Abrir menu"
        >
          <Menu className="h-4 w-4" />
        </Button>
        
        {/* Mobile overlay */}
        {mobileOpen && (
          <div className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm md:hidden">
            <div 
              className="absolute inset-0" 
              onClick={() => setMobileOpen(false)} 
              aria-hidden="true"
            />
            {sidebarContent}
          </div>
        )}
      </>
    )
  }

  return sidebarContent
}

// Componente de seção
interface SidebarSectionProps {
  section: SidebarSection
}

const SidebarSection: React.FC<SidebarSectionProps> = ({ section }) => {
  const { collapsed } = useSidebarContext()
  
  return (
    <div>
      {section.title && !collapsed && (
        <div className="px-2 py-1 mb-2">
          <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
            {section.title}
          </span>
        </div>
      )}
      <div className="space-y-1">
        {section.items.map((item) => (
          <SidebarMenuItem key={item.id} item={item} />
        ))}
      </div>
    </div>
  )
}

// Componente de item do menu
interface SidebarMenuItemProps {
  item: SidebarItem
  level?: number
}

const SidebarMenuItem: React.FC<SidebarMenuItemProps> = ({ item, level = 0 }) => {
  const { collapsed, activeItemId, onItemClick } = useSidebarContext()
  const [expanded, setExpanded] = React.useState(false)
  
  const isActive = activeItemId === item.id
  const hasChildren = item.children && item.children.length > 0
  const Icon = item.icon
  
  const handleClick = () => {
    if (item.disabled) return
    
    if (hasChildren) {
      setExpanded(!expanded)
    } else {
      onItemClick(item)
    }
  }
  
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      handleClick()
    }
  }
  
  return (
    <div>
      <div
        role="button"
        tabIndex={item.disabled ? -1 : 0}
        onClick={handleClick}
        onKeyDown={handleKeyDown}
        className={cn(
          "flex items-center gap-3 px-2 py-2 mx-1 rounded-lg text-sm font-medium transition-colors cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary",
          isActive 
            ? "bg-primary text-primary-foreground" 
            : "text-foreground hover:bg-accent hover:text-accent-foreground",
          item.disabled && "opacity-50 cursor-not-allowed",
          collapsed && "justify-center px-0",
          level > 0 && !collapsed && "ml-6"
        )}
        style={{ paddingLeft: collapsed ? undefined : `${8 + (level * 16)}px` }}
        aria-label={item.label}
        aria-expanded={hasChildren ? expanded : undefined}
      >
        {Icon && (
          <Icon className={cn(
            "h-4 w-4 flex-shrink-0",
            collapsed ? "h-5 w-5" : ""
          )} />
        )}
        
        {!collapsed && (
          <>
            <span className="flex-1">{item.label}</span>
            
            {item.badge && (
              <span className={cn(
                "inline-flex items-center justify-center px-2 py-0.5 text-xs font-medium rounded-full",
                isActive 
                  ? "bg-primary-foreground/20 text-primary-foreground" 
                  : "bg-primary text-primary-foreground"
              )}>
                {item.badge}
              </span>
            )}
            
            {hasChildren && (
              <ChevronRight className={cn(
                "h-4 w-4 flex-shrink-0 transition-transform",
                expanded && "rotate-90"
              )} />
            )}
          </>
        )}
      </div>
      
      {/* Subitems */}
      {hasChildren && expanded && !collapsed && (
        <div className="mt-1 space-y-1">
          {item.children!.map((child) => (
            <SidebarMenuItem 
              key={child.id} 
              item={child} 
              level={level + 1} 
            />
          ))}
        </div>
      )}
    </div>
  )
}

// Componente de usuário
interface SidebarUserProps {
  user: SidebarUser
  onUserMenuClick?: (action: 'profile' | 'settings' | 'logout') => void
}

const SidebarUser: React.FC<SidebarUserProps> = ({ user, onUserMenuClick }) => {
  const { collapsed } = useSidebarContext()
  const [menuOpen, setMenuOpen] = React.useState(false)
  
  const handleAction = (action: 'profile' | 'settings' | 'logout') => {
    setMenuOpen(false)
    onUserMenuClick?.(action)
  }
  
  if (collapsed) {
    return (
      <div className="relative">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setMenuOpen(!menuOpen)}
          className="h-10 w-10 p-0 rounded-full"
          aria-label="Menu do usuário"
        >
          {user.avatar ? (
            <img 
              src={user.avatar} 
              alt={user.name} 
              className="h-8 w-8 rounded-full"
            />
          ) : (
            <User className="h-5 w-5" />
          )}
        </Button>
        
        {menuOpen && (
          <div className="absolute bottom-full left-0 mb-2 bg-popover border rounded-lg shadow-md py-2 min-w-48 z-50">
            <div className="px-4 py-2 border-b">
              <div className="font-medium text-sm">{user.name}</div>
              {user.email && (
                <div className="text-xs text-muted-foreground">{user.email}</div>
              )}
            </div>
            <button
              onClick={() => handleAction('profile')}
              className="flex items-center gap-2 w-full px-4 py-2 text-sm hover:bg-accent"
            >
              <User className="h-4 w-4" />
              Perfil
            </button>
            <button
              onClick={() => handleAction('settings')}
              className="flex items-center gap-2 w-full px-4 py-2 text-sm hover:bg-accent"
            >
              <Settings className="h-4 w-4" />
              Configurações
            </button>
            <button
              onClick={() => handleAction('logout')}
              className="flex items-center gap-2 w-full px-4 py-2 text-sm text-destructive hover:bg-destructive/10"
            >
              <LogOut className="h-4 w-4" />
              Sair
            </button>
          </div>
        )}
      </div>
    )
  }
  
  return (
    <div className="relative">
      <button
        onClick={() => setMenuOpen(!menuOpen)}
        className="flex items-center gap-3 p-2 w-full rounded-lg hover:bg-accent transition-colors"
      >
        {user.avatar ? (
          <img 
            src={user.avatar} 
            alt={user.name} 
            className="h-8 w-8 rounded-full"
          />
        ) : (
          <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <User className="h-4 w-4" />
          </div>
        )}
        <div className="flex-1 text-left">
          <div className="font-medium text-sm">{user.name}</div>
          {user.email && (
            <div className="text-xs text-muted-foreground truncate">{user.email}</div>
          )}
        </div>
        <ChevronRight className={cn(
          "h-4 w-4 transition-transform",
          menuOpen && "rotate-90"
        )} />
      </button>
      
      {menuOpen && (
        <div className="absolute bottom-full left-2 right-2 mb-2 bg-popover border rounded-lg shadow-md py-2 z-50">
          <button
            onClick={() => handleAction('profile')}
            className="flex items-center gap-2 w-full px-4 py-2 text-sm hover:bg-accent"
          >
            <User className="h-4 w-4" />
            Perfil
          </button>
          <button
            onClick={() => handleAction('settings')}
            className="flex items-center gap-2 w-full px-4 py-2 text-sm hover:bg-accent"
          >
            <Settings className="h-4 w-4" />
            Configurações
          </button>
          <button
            onClick={() => handleAction('logout')}
            className="flex items-center gap-2 w-full px-4 py-2 text-sm text-destructive hover:bg-destructive/10"
          >
            <LogOut className="h-4 w-4" />
            Sair
          </button>
        </div>
      )}
    </div>
  )
}

// Hook personalizado para controlar sidebar
export const useSidebar = () => {
  const context = React.useContext(SidebarContext)
  return context
}

NavigationSidebar.displayName = "NavigationSidebar"

export { NavigationSidebar }