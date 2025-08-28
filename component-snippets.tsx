/**
 * COMPONENT SNIPPETS - Sistema de Gestão de Pedidos v2.0
 * Exemplos de implementação ready-to-use dos componentes chave
 * 
 * Para desenvolvedores: 
 * 1. Copie os snippets necessários para seus arquivos
 * 2. Ajuste props conforme necessário
 * 3. Certifique-se de ter as dependências instaladas
 */

import React from 'react'
import { Search, Plus, Eye, Edit, Trash2, MoreHorizontal, CheckCircle, Clock, XCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'

// ========================
// 1. SEARCH BAR COMPONENT
// ========================
interface SearchBarProps {
  placeholder?: string
  value: string
  onChange: (value: string) => void
  className?: string
}

export const SearchBar: React.FC<SearchBarProps> = ({
  placeholder = "Buscar...",
  value,
  onChange,
  className = ""
}) => {
  return (
    <div className={`relative ${className}`}>
      <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
      <Input
        type="search"
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="pl-10 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60"
      />
    </div>
  )
}

// ========================
// 2. STATUS BADGE COMPONENT
// ========================
interface StatusBadgeProps {
  status: 'pending' | 'processing' | 'completed' | 'failed' | 'cancelled'
  size?: 'sm' | 'default' | 'lg'
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'default' }) => {
  const statusConfig = {
    pending: {
      variant: 'secondary' as const,
      icon: Clock,
      label: 'Pendente',
      className: 'bg-yellow-100 text-yellow-800 hover:bg-yellow-200 dark:bg-yellow-900/20 dark:text-yellow-400'
    },
    processing: {
      variant: 'default' as const,
      icon: Clock,
      label: 'Processando',
      className: 'bg-blue-100 text-blue-800 hover:bg-blue-200 dark:bg-blue-900/20 dark:text-blue-400'
    },
    completed: {
      variant: 'default' as const,
      icon: CheckCircle,
      label: 'Concluído',
      className: 'bg-green-100 text-green-800 hover:bg-green-200 dark:bg-green-900/20 dark:text-green-400'
    },
    failed: {
      variant: 'destructive' as const,
      icon: XCircle,
      label: 'Falhou',
      className: 'bg-red-100 text-red-800 hover:bg-red-200 dark:bg-red-900/20 dark:text-red-400'
    },
    cancelled: {
      variant: 'outline' as const,
      icon: XCircle,
      label: 'Cancelado',
      className: 'bg-gray-100 text-gray-800 hover:bg-gray-200 dark:bg-gray-900/20 dark:text-gray-400'
    }
  }

  const config = statusConfig[status]
  const Icon = config.icon

  return (
    <Badge variant={config.variant} className={`inline-flex items-center gap-1.5 ${config.className}`}>
      <Icon className="h-3 w-3" />
      {config.label}
    </Badge>
  )
}

// ========================
// 3. DATA TABLE WITH ACTIONS
// ========================
interface TableAction {
  label: string
  icon: React.ComponentType<{ className?: string }>
  onClick: (item: any) => void
  variant?: 'default' | 'destructive'
}

interface DataTableProps<T> {
  data: T[]
  columns: {
    key: keyof T
    label: string
    render?: (value: any, item: T) => React.ReactNode
  }[]
  actions?: TableAction[]
  loading?: boolean
  emptyMessage?: string
}

export function DataTable<T extends Record<string, any>>({
  data,
  columns,
  actions = [],
  loading = false,
  emptyMessage = "Nenhum item encontrado"
}: DataTableProps<T>) {
  if (loading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-14 bg-muted animate-pulse rounded-lg" />
        ))}
      </div>
    )
  }

  return (
    <div className="rounded-xl border bg-card">
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent border-b">
            {columns.map((column) => (
              <TableHead key={String(column.key)} className="font-semibold">
                {column.label}
              </TableHead>
            ))}
            {actions.length > 0 && (
              <TableHead className="text-center w-[100px]">Ações</TableHead>
            )}
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.length === 0 ? (
            <TableRow>
              <TableCell 
                colSpan={columns.length + (actions.length > 0 ? 1 : 0)} 
                className="text-center py-12 text-muted-foreground"
              >
                {emptyMessage}
              </TableCell>
            </TableRow>
          ) : (
            data.map((item, index) => (
              <TableRow key={index} className="hover:bg-muted/50">
                {columns.map((column) => (
                  <TableCell key={String(column.key)}>
                    {column.render 
                      ? column.render(item[column.key], item) 
                      : String(item[column.key])
                    }
                  </TableCell>
                ))}
                {actions.length > 0 && (
                  <TableCell className="text-center">
                    <div className="flex items-center justify-center gap-1">
                      {actions.map((action, actionIndex) => {
                        const ActionIcon = action.icon
                        return (
                          <Button
                            key={actionIndex}
                            size="icon"
                            variant={action.variant === 'destructive' ? 'destructive' : 'ghost'}
                            onClick={() => action.onClick(item)}
                            className="h-8 w-8"
                          >
                            <ActionIcon className="h-4 w-4" />
                            <span className="sr-only">{action.label}</span>
                          </Button>
                        )
                      })}
                    </div>
                  </TableCell>
                )}
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </div>
  )
}

// ========================
// 4. METRIC CARD COMPONENT
// ========================
interface MetricCardProps {
  title: string
  value: string | number
  description?: string
  trend?: {
    value: string
    direction: 'up' | 'down' | 'neutral'
  }
  icon?: React.ComponentType<{ className?: string }>
  loading?: boolean
}

export const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  description,
  trend,
  icon: Icon,
  loading = false
}) => {
  if (loading) {
    return (
      <Card className="p-6">
        <div className="space-y-3">
          <div className="h-4 w-20 bg-muted animate-pulse rounded" />
          <div className="h-8 w-16 bg-muted animate-pulse rounded" />
          <div className="h-3 w-24 bg-muted animate-pulse rounded" />
        </div>
      </Card>
    )
  }

  return (
    <Card className="transition-all duration-200 hover:shadow-md">
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        {Icon && <Icon className="h-4 w-4 text-muted-foreground" />}
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold text-primary">{value}</div>
        {description && (
          <p className="text-xs text-muted-foreground mt-1">
            {description}
          </p>
        )}
        {trend && (
          <div className="flex items-center text-xs mt-2">
            <span 
              className={`font-medium ${
                trend.direction === 'up' 
                  ? 'text-green-600' 
                  : trend.direction === 'down' 
                    ? 'text-red-600' 
                    : 'text-muted-foreground'
              }`}
            >
              {trend.value}
            </span>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

// ========================
// 5. CLIENT CARD COMPONENT
// ========================
interface ClientCardProps {
  client: {
    id: string
    name: string
    email: string
    avatar?: string
    orderCount: number
    totalSpent: number
    lastOrderDate?: string
  }
  onEdit: (client: any) => void
  onView: (client: any) => void
  onDelete: (client: any) => void
}

export const ClientCard: React.FC<ClientCardProps> = ({
  client,
  onEdit,
  onView,
  onDelete
}) => {
  return (
    <Card className="group hover:shadow-lg transition-all duration-200 overflow-hidden">
      <div className="h-1 bg-gradient-to-r from-primary to-secondary" />
      <CardHeader className="pb-4">
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3">
            <Avatar className="h-12 w-12 border-2 border-primary/10">
              <AvatarImage src={client.avatar} alt={client.name} />
              <AvatarFallback className="bg-primary/10 text-primary font-semibold">
                {client.name.split(' ').map(n => n[0]).join('').substring(0, 2)}
              </AvatarFallback>
            </Avatar>
            <div>
              <CardTitle className="text-lg font-semibold text-foreground group-hover:text-primary transition-colors">
                {client.name}
              </CardTitle>
              <CardDescription className="text-sm">{client.email}</CardDescription>
            </div>
          </div>
          <div className="opacity-0 group-hover:opacity-100 transition-opacity">
            <Button size="sm" variant="ghost" className="h-8 w-8 p-0">
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div className="text-center">
            <div className="text-xl font-bold text-primary">{client.orderCount}</div>
            <div className="text-xs text-muted-foreground uppercase tracking-wide">
              Pedidos
            </div>
          </div>
          <div className="text-center">
            <div className="text-xl font-bold text-primary">
              R$ {client.totalSpent.toLocaleString('pt-BR')}
            </div>
            <div className="text-xs text-muted-foreground uppercase tracking-wide">
              Total
            </div>
          </div>
        </div>
        
        {client.lastOrderDate && (
          <div className="text-xs text-muted-foreground mb-4">
            Último pedido: {new Date(client.lastOrderDate).toLocaleDateString('pt-BR')}
          </div>
        )}

        <div className="flex gap-2">
          <Button 
            size="sm" 
            variant="outline" 
            className="flex-1"
            onClick={() => onView(client)}
          >
            <Eye className="h-4 w-4 mr-1" />
            Ver
          </Button>
          <Button 
            size="sm" 
            variant="outline"
            onClick={() => onEdit(client)}
          >
            <Edit className="h-4 w-4" />
          </Button>
          <Button 
            size="sm" 
            variant="outline"
            onClick={() => onDelete(client)}
            className="text-red-600 hover:text-red-700 hover:border-red-200"
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}

// ========================
// 6. FILTER TOOLBAR
// ========================
interface FilterToolbarProps {
  searchValue: string
  onSearchChange: (value: string) => void
  statusFilter: string
  onStatusChange: (value: string) => void
  onAddNew: () => void
  onExport?: () => void
  addButtonText?: string
  searchPlaceholder?: string
}

export const FilterToolbar: React.FC<FilterToolbarProps> = ({
  searchValue,
  onSearchChange,
  statusFilter,
  onStatusChange,
  onAddNew,
  onExport,
  addButtonText = "Adicionar",
  searchPlaceholder = "Buscar..."
}) => {
  return (
    <Card className="p-4 mb-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center flex-1">
          <SearchBar
            placeholder={searchPlaceholder}
            value={searchValue}
            onChange={onSearchChange}
            className="sm:w-80"
          />
          
          <Select value={statusFilter} onValueChange={onStatusChange}>
            <SelectTrigger className="w-full sm:w-48">
              <SelectValue placeholder="Filtrar por status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Todos os status</SelectItem>
              <SelectItem value="pending">Pendente</SelectItem>
              <SelectItem value="processing">Processando</SelectItem>
              <SelectItem value="completed">Concluído</SelectItem>
              <SelectItem value="failed">Falhou</SelectItem>
              <SelectItem value="cancelled">Cancelado</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="flex gap-2">
          {onExport && (
            <Button variant="outline" onClick={onExport}>
              Exportar
            </Button>
          )}
          <Button onClick={onAddNew}>
            <Plus className="h-4 w-4 mr-2" />
            {addButtonText}
          </Button>
        </div>
      </div>
    </Card>
  )
}

// ========================
// 7. EMPTY STATE COMPONENT
// ========================
interface EmptyStateProps {
  icon?: React.ComponentType<{ className?: string }>
  title: string
  description: string
  actionLabel?: string
  onAction?: () => void
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon,
  title,
  description,
  actionLabel,
  onAction
}) => {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
      {Icon && (
        <div className="mb-4 rounded-full bg-muted p-4">
          <Icon className="h-8 w-8 text-muted-foreground" />
        </div>
      )}
      <h3 className="mb-2 text-lg font-semibold text-foreground">{title}</h3>
      <p className="mb-6 text-sm text-muted-foreground max-w-sm">
        {description}
      </p>
      {actionLabel && onAction && (
        <Button onClick={onAction}>
          <Plus className="h-4 w-4 mr-2" />
          {actionLabel}
        </Button>
      )}
    </div>
  )
}

// ========================
// EXEMPLO DE USO COMPLETO
// ========================

// Exemplo: Lista de Pedidos com filtros e ações
export const OrdersExample: React.FC = () => {
  const [searchValue, setSearchValue] = React.useState('')
  const [statusFilter, setStatusFilter] = React.useState('all')
  
  const orders = [
    { 
      id: '#1234', 
      customer: 'João Silva', 
      total: 'R$ 299,90', 
      status: 'completed' as const,
      date: '2024-08-28'
    },
    { 
      id: '#1235', 
      customer: 'Maria Costa', 
      total: 'R$ 159,90', 
      status: 'pending' as const,
      date: '2024-08-28'
    }
  ]

  const tableActions: TableAction[] = [
    {
      label: 'Ver detalhes',
      icon: Eye,
      onClick: (order) => console.log('Ver:', order),
    },
    {
      label: 'Editar',
      icon: Edit,
      onClick: (order) => console.log('Editar:', order),
    },
    {
      label: 'Excluir',
      icon: Trash2,
      onClick: (order) => console.log('Excluir:', order),
      variant: 'destructive'
    }
  ]

  const columns = [
    { key: 'id' as const, label: 'ID' },
    { key: 'customer' as const, label: 'Cliente' },
    { key: 'total' as const, label: 'Total' },
    { 
      key: 'status' as const, 
      label: 'Status',
      render: (status: any) => <StatusBadge status={status} />
    },
    { key: 'date' as const, label: 'Data' }
  ]

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">Pedidos</h1>
          <p className="text-muted-foreground">
            Gerencie todos os pedidos do sistema
          </p>
        </div>
      </div>

      <FilterToolbar
        searchValue={searchValue}
        onSearchChange={setSearchValue}
        statusFilter={statusFilter}
        onStatusChange={setStatusFilter}
        onAddNew={() => console.log('Novo pedido')}
        onExport={() => console.log('Exportar')}
        addButtonText="Novo Pedido"
        searchPlaceholder="Buscar por ID ou cliente..."
      />

      <DataTable
        data={orders}
        columns={columns}
        actions={tableActions}
        emptyMessage="Nenhum pedido encontrado"
      />
    </div>
  )
}

// Exemplo: Dashboard com métricas
export const DashboardExample: React.FC = () => {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground">
          Visão geral do sistema de gestão de pedidos
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          title="Total de Pedidos"
          value="1,247"
          description="Pedidos processados hoje"
          trend={{ value: "+12%", direction: "up" }}
        />
        <MetricCard
          title="Receita Total"
          value="R$ 89.2K"
          description="Receita gerada hoje"
          trend={{ value: "+8%", direction: "up" }}
        />
        <MetricCard
          title="Taxa de Conversão"
          value="96.8%"
          description="Pedidos confirmados/total"
          trend={{ value: "+2.1%", direction: "up" }}
        />
        <MetricCard
          title="Pedidos Pendentes"
          value="23"
          description="Aguardando processamento"
          trend={{ value: "3 novos", direction: "neutral" }}
        />
      </div>
    </div>
  )
}

export default {
  SearchBar,
  StatusBadge,
  DataTable,
  MetricCard,
  ClientCard,
  FilterToolbar,
  EmptyState,
  OrdersExample,
  DashboardExample
}