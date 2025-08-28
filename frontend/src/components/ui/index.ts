/**
 * EXPORTAÇÕES UNIFICADAS DOS COMPONENTES UI
 * Sistema de Gestão de Pedidos - Frontend Library
 */

// Componentes básicos existentes
export { Button, buttonVariants } from './button'
export type { ButtonProps } from './button'

export { Badge, badgeVariants } from './badge'
export type { BadgeProps } from './badge'

export { Card, CardHeader, CardContent, CardFooter, CardTitle, CardDescription } from './card'

export { Input } from './input'
export type { InputProps } from './input'

export { Label } from './label'

export { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './select'

export { 
  Table, 
  TableHeader, 
  TableBody, 
  TableFooter, 
  TableRow, 
  TableHead, 
  TableCell, 
  TableCaption 
} from './table'

export { Tabs, TabsList, TabsTrigger, TabsContent } from './tabs'

export { toast, useToast } from './use-toast'
export { Toaster } from './toaster'

// Componentes avançados novos
export { DataTable, TablePagination } from './data-table'
export type { 
  DataTableProps,
  TableColumn,
  TableAction,
  TablePagination as TablePaginationProps,
  SortDirection 
} from './data-table'

export {
  Modal,
  ModalContent,
  ModalHeader,
  ModalTitle,
  ModalDescription,
  ModalBody,
  ModalFooter,
  useModal
} from './modal'
export type { 
  ModalProps,
  ModalContentProps,
  ModalHeaderProps,
  ModalFooterProps 
} from './modal'

export { NavigationSidebar, useSidebar } from './navigation-sidebar'
export type {
  SidebarProps,
  SidebarItem,
  SidebarSection,
  SidebarUser
} from './navigation-sidebar'

// Exemplos (para desenvolvimento/teste)
export {
  DataTableExample,
  ModalExample,
  SidebarExample,
  CompleteDashboardExample
} from './examples'