import { useState, useMemo } from 'react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useToast } from '@/components/ui/use-toast'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import apiClient from '@/lib/api'
import { formatCurrency, cn } from '@/lib/utils'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Eye,
  Plus,
  Search,
  RefreshCw,
  CheckCircle,
  Clock,
  XCircle,
  AlertCircle,
  Database,
  Activity,
  History,
  Filter,
  Download,
  MoreHorizontal,
  Calendar,
  ArrowUpDown,
  ChevronDown,
  FileText,
  TrendingUp,
  Users,
  DollarSign,
  Package,
  ShoppingCart,
  Zap,
  CheckCircle2,
  Ban,
  AlertTriangle,
  Settings,
  Edit3
} from 'lucide-react'

interface OrderStatus {
  value: string
  label: string
  color: string
  bgColor: string
  icon: React.ElementType
}

const orderStatuses: OrderStatus[] = [
  { value: 'PENDING', label: 'Pendente', color: 'text-yellow-600', bgColor: 'bg-yellow-100 dark:bg-yellow-900/20', icon: Clock },
  { value: 'CONFIRMED', label: 'Confirmado', color: 'text-blue-600', bgColor: 'bg-blue-100 dark:bg-blue-900/20', icon: CheckCircle2 },
  { value: 'PROCESSING', label: 'Processando', color: 'text-purple-600', bgColor: 'bg-purple-100 dark:bg-purple-900/20', icon: Activity },
  { value: 'PAID', label: 'Pago', color: 'text-green-600', bgColor: 'bg-green-100 dark:bg-green-900/20', icon: CheckCircle },
  { value: 'COMPLETED', label: 'Concluído', color: 'text-green-600', bgColor: 'bg-green-100 dark:bg-green-900/20', icon: CheckCircle },
  { value: 'CANCELLED', label: 'Cancelado', color: 'text-gray-600', bgColor: 'bg-gray-100 dark:bg-gray-900/20', icon: Ban },
  { value: 'FAILED', label: 'Falhou', color: 'text-red-600', bgColor: 'bg-red-100 dark:bg-red-900/20', icon: XCircle },
]

export function Orders() {
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedOrder, setSelectedOrder] = useState<any>(null)
  const [createOrderOpen, setCreateOrderOpen] = useState(false)
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [dateFilter, setDateFilter] = useState<string>('all')
  const [sortBy, setSortBy] = useState<string>('created_desc')
  const [viewEventsOpen, setViewEventsOpen] = useState(false)
  const [selectedTab, setSelectedTab] = useState('all')
  const { toast } = useToast()
  const queryClient = useQueryClient()

  // Form states for creating orders
  const [customerId, setCustomerId] = useState('')
  const [totalAmount, setTotalAmount] = useState('')
  const [productIds, setProductIds] = useState<string[]>([''])

  // Fetch orders from backend
  const { data: ordersResponse, isLoading, refetch } = useQuery({
    queryKey: ['orders'],
    queryFn: () => apiClient.getOrders(),
    refetchInterval: 30000,
  })

  // Fetch order events
  const { data: eventsData, isLoading: eventsLoading } = useQuery({
    queryKey: ['order-events', selectedOrder?.id],
    queryFn: () => selectedOrder?.id ? apiClient.getOrderEvents(selectedOrder.id) : Promise.resolve([]),
    enabled: !!selectedOrder?.id && viewEventsOpen,
  })

  const orders = Array.isArray(ordersResponse) ? ordersResponse : []

  // Advanced filtering and sorting logic
  const filteredAndSortedOrders = useMemo(() => {
    let filtered = orders.filter((order: any) => {
      const matchesSearch = 
        order.id?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        order.customerId?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        order.orderId?.toLowerCase().includes(searchTerm.toLowerCase())
      
      const matchesStatus = statusFilter === 'all' || order.status === statusFilter
      
      const matchesTab = selectedTab === 'all' || 
        (selectedTab === 'active' && ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(order.status)) ||
        (selectedTab === 'completed' && ['PAID', 'COMPLETED'].includes(order.status)) ||
        (selectedTab === 'issues' && ['CANCELLED', 'FAILED'].includes(order.status))
      
      const matchesDate = dateFilter === 'all' || (() => {
        if (!order.createdAt) return true
        const orderDate = new Date(order.createdAt)
        const now = new Date()
        switch (dateFilter) {
          case 'today':
            return orderDate.toDateString() === now.toDateString()
          case 'week':
            const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
            return orderDate >= weekAgo
          case 'month':
            const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
            return orderDate >= monthAgo
          default:
            return true
        }
      })()
      
      return matchesSearch && matchesStatus && matchesTab && matchesDate
    })

    // Sort orders
    filtered.sort((a: any, b: any) => {
      switch (sortBy) {
        case 'created_desc':
          return new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
        case 'created_asc':
          return new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime()
        case 'amount_desc':
          return (b.totalAmount || 0) - (a.totalAmount || 0)
        case 'amount_asc':
          return (a.totalAmount || 0) - (b.totalAmount || 0)
        case 'customer_asc':
          return (a.customerId || '').localeCompare(b.customerId || '')
        case 'customer_desc':
          return (b.customerId || '').localeCompare(a.customerId || '')
        default:
          return 0
      }
    })

    return filtered
  }, [orders, searchTerm, statusFilter, selectedTab, dateFilter, sortBy])

  // Statistics calculations
  const stats = useMemo(() => {
    const total = orders.length
    const pending = orders.filter((o: any) => o.status === 'PENDING').length
    const completed = orders.filter((o: any) => ['PAID', 'COMPLETED'].includes(o.status)).length
    const revenue = orders.reduce((sum: number, order: any) => sum + (order.totalAmount || 0), 0)
    const avgOrderValue = total > 0 ? revenue / total : 0

    return {
      total,
      pending,
      completed,
      revenue,
      avgOrderValue,
      completionRate: total > 0 ? (completed / total) * 100 : 0
    }
  }, [orders])

  // Create order mutation
  const createOrderMutation = useMutation({
    mutationFn: (orderData: { customerId: string; totalAmount: number; productIds?: string[] }) =>
      apiClient.createOrder(orderData),
    onSuccess: (response) => {
      toast({
        title: "Pedido criado com sucesso!",
        description: `Pedido ${response.orderId} foi criado.`,
      })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setCreateOrderOpen(false)
      resetForm()
    },
    onError: (error: any) => {
      toast({
        title: "Erro ao criar pedido",
        description: error.message || "Não foi possível criar o pedido.",
        variant: "destructive",
      })
    }
  })

  // Update order status mutation
  const updateStatusMutation = useMutation({
    mutationFn: ({ orderId, status }: { orderId: string; status: string }) =>
      apiClient.updateOrderStatus(orderId, status),
    onSuccess: () => {
      toast({
        title: "Status atualizado!",
        description: "O status do pedido foi atualizado com sucesso.",
      })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
    onError: (error: any) => {
      toast({
        title: "Erro ao atualizar status",
        description: error.message || "Não foi possível atualizar o status.",
        variant: "destructive",
      })
    }
  })

  const resetForm = () => {
    setCustomerId('')
    setTotalAmount('')
    setProductIds([''])
  }

  const getStatusConfig = (status: string): OrderStatus => {
    return orderStatuses.find(s => s.value === status) || orderStatuses[0]
  }

  const handleCreateOrder = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!customerId || !totalAmount) {
      toast({
        title: "Campos obrigatórios",
        description: "Por favor, preencha todos os campos obrigatórios.",
        variant: "destructive",
      })
      return
    }

    const orderData = {
      customerId: customerId.trim(),
      totalAmount: parseFloat(totalAmount),
      productIds: productIds.filter(id => id.trim() !== '')
    }

    createOrderMutation.mutate(orderData)
  }

  const handleStatusUpdate = (orderId: string, newStatus: string) => {
    updateStatusMutation.mutate({ orderId, status: newStatus })
  }

  const addProductId = () => {
    setProductIds([...productIds, ''])
  }

  const updateProductId = (index: number, value: string) => {
    const newProductIds = [...productIds]
    newProductIds[index] = value
    setProductIds(newProductIds)
  }

  const removeProductId = (index: number) => {
    setProductIds(productIds.filter((_, i) => i !== index))
  }

  const handleViewEvents = (order: any) => {
    setSelectedOrder(order)
    setViewEventsOpen(true)
  }

  const handleRefresh = async () => {
    await refetch()
    toast({
      title: "Dados atualizados",
      description: "A lista de pedidos foi atualizada com sucesso.",
    })
  }

  return (
    <div className="space-y-6">
      {/* Header with Stats */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
              <div className="p-2 bg-primary/10 rounded-xl">
                <ShoppingCart className="h-6 w-6 text-primary" />
              </div>
              Pedidos
            </h1>
            <p className="text-muted-foreground mt-2">
              Sistema de gestão de pedidos com Event Sourcing e CQRS
            </p>
          </div>
          
          <div className="flex items-center gap-3">
            <Button variant="outline" onClick={handleRefresh} disabled={isLoading}>
              <RefreshCw className={cn("mr-2 h-4 w-4", isLoading && "animate-spin")} />
              Atualizar
            </Button>
            <Button onClick={() => setCreateOrderOpen(true)} className="bg-primary hover:bg-primary/90">
              <Plus className="mr-2 h-4 w-4" />
              Novo Pedido
            </Button>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <div className="p-2 bg-blue-100 rounded-lg dark:bg-blue-900/20">
                  <FileText className="h-5 w-5 text-blue-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-muted-foreground">Total de Pedidos</p>
                  <p className="text-2xl font-bold">{stats.total}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <div className="p-2 bg-yellow-100 rounded-lg dark:bg-yellow-900/20">
                  <Clock className="h-5 w-5 text-yellow-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-muted-foreground">Pendentes</p>
                  <p className="text-2xl font-bold">{stats.pending}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <div className="p-2 bg-green-100 rounded-lg dark:bg-green-900/20">
                  <TrendingUp className="h-5 w-5 text-green-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-muted-foreground">Taxa de Conclusão</p>
                  <p className="text-2xl font-bold">{stats.completionRate.toFixed(1)}%</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <div className="p-2 bg-purple-100 rounded-lg dark:bg-purple-900/20">
                  <DollarSign className="h-5 w-5 text-purple-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-muted-foreground">Receita Total</p>
                  <p className="text-2xl font-bold">{formatCurrency(stats.revenue)}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Filters and Search */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Filter className="h-5 w-5" />
            Filtros Avançados
          </CardTitle>
          <CardDescription>
            Use os filtros para refinar sua busca de pedidos
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Primary Filters */}
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-2">
              <Label htmlFor="search">Buscar</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 text-muted-foreground -translate-y-1/2" />
                <Input
                  id="search"
                  placeholder="ID do pedido, cliente..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>Status</Label>
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Todos os status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todos os status</SelectItem>
                  {orderStatuses.map((status) => (
                    <SelectItem key={status.value} value={status.value}>
                      <div className="flex items-center gap-2">
                        <status.icon className="h-4 w-4" />
                        {status.label}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Período</Label>
              <Select value={dateFilter} onValueChange={setDateFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Todos os períodos" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todos os períodos</SelectItem>
                  <SelectItem value="today">Hoje</SelectItem>
                  <SelectItem value="week">Última semana</SelectItem>
                  <SelectItem value="month">Último mês</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Ordenar por</Label>
              <Select value={sortBy} onValueChange={setSortBy}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="created_desc">Mais recentes</SelectItem>
                  <SelectItem value="created_asc">Mais antigos</SelectItem>
                  <SelectItem value="amount_desc">Maior valor</SelectItem>
                  <SelectItem value="amount_asc">Menor valor</SelectItem>
                  <SelectItem value="customer_asc">Cliente A-Z</SelectItem>
                  <SelectItem value="customer_desc">Cliente Z-A</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Orders Table with Tabs */}
      <Card>
        <Tabs value={selectedTab} onValueChange={setSelectedTab} className="w-full">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <CardTitle>Lista de Pedidos</CardTitle>
                <CardDescription>
                  {filteredAndSortedOrders.length} de {orders.length} pedidos
                </CardDescription>
              </div>

              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm">
                  <Download className="h-4 w-4 mr-2" />
                  Exportar
                </Button>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="outline" size="sm">
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem>
                      <FileText className="h-4 w-4 mr-2" />
                      Relatório Detalhado
                    </DropdownMenuItem>
                    <DropdownMenuItem>
                      <Settings className="h-4 w-4 mr-2" />
                      Configurar Colunas
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>

            <TabsList className="grid w-full grid-cols-4">
              <TabsTrigger value="all" className="flex items-center gap-2">
                <Database className="h-4 w-4" />
                Todos ({orders.length})
              </TabsTrigger>
              <TabsTrigger value="active" className="flex items-center gap-2">
                <Activity className="h-4 w-4" />
                Ativos ({orders.filter((o: any) => ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(o.status)).length})
              </TabsTrigger>
              <TabsTrigger value="completed" className="flex items-center gap-2">
                <CheckCircle className="h-4 w-4" />
                Concluídos ({orders.filter((o: any) => ['PAID', 'COMPLETED'].includes(o.status)).length})
              </TabsTrigger>
              <TabsTrigger value="issues" className="flex items-center gap-2">
                <AlertTriangle className="h-4 w-4" />
                Problemas ({orders.filter((o: any) => ['CANCELLED', 'FAILED'].includes(o.status)).length})
              </TabsTrigger>
            </TabsList>
          </CardHeader>

          <CardContent>
            <TabsContent value={selectedTab}>
              {isLoading ? (
                <div className="space-y-3">
                  {Array.from({ length: 8 }).map((_, i) => (
                    <div key={i} className="h-16 bg-muted animate-pulse rounded-lg" />
                  ))}
                </div>
              ) : (
                <div className="rounded-md border">
                  <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-[100px]">
                        <Button variant="ghost" size="sm" className="h-8 data-[state=open]:bg-accent">
                          ID
                          <ArrowUpDown className="ml-2 h-4 w-4" />
                        </Button>
                      </TableHead>
                      <TableHead>Cliente</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="text-right">Valor</TableHead>
                      <TableHead>Data</TableHead>
                      <TableHead className="w-[100px]">Ações</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredAndSortedOrders.map((order: any, index: number) => {
                      const statusConfig = getStatusConfig(order.status)
                      const StatusIcon = statusConfig.icon
                      
                      return (
                        <TableRow key={order.id || index} className="hover:bg-muted/50">
                          <TableCell className="font-mono text-sm">
                            #{(order.id || order.orderId || `ORDER-${index + 1}`).slice(-8)}
                          </TableCell>
                          
                          <TableCell>
                            <div className="flex items-center gap-3">
                              <div className="p-2 bg-primary/10 rounded-full">
                                <Users className="h-4 w-4 text-primary" />
                              </div>
                              <div>
                                <p className="font-medium">{order.customerId || 'N/A'}</p>
                                <p className="text-sm text-muted-foreground">Cliente</p>
                              </div>
                            </div>
                          </TableCell>
                          
                          <TableCell>
                            <Badge className={cn("flex items-center gap-1 w-fit", statusConfig.bgColor, statusConfig.color)}>
                              <StatusIcon className="h-3 w-3" />
                              {statusConfig.label}
                            </Badge>
                          </TableCell>
                          
                          <TableCell className="text-right">
                            <div>
                              <p className="font-semibold">{formatCurrency(order.totalAmount || order.total || 0)}</p>
                              <p className="text-sm text-muted-foreground">Total</p>
                            </div>
                          </TableCell>
                          
                          <TableCell>
                            <div>
                              <p className="text-sm">
                                {order.createdAt ? new Date(order.createdAt).toLocaleDateString('pt-BR') : 'N/A'}
                              </p>
                              <p className="text-xs text-muted-foreground">
                                {order.createdAt ? new Date(order.createdAt).toLocaleTimeString('pt-BR') : ''}
                              </p>
                            </div>
                          </TableCell>
                          
                          <TableCell>
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                                  <MoreHorizontal className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="w-48">
                                <DropdownMenuLabel>Ações</DropdownMenuLabel>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem onClick={() => setSelectedOrder(order)}>
                                  <Eye className="h-4 w-4 mr-2" />
                                  Ver Detalhes
                                </DropdownMenuItem>
                                <DropdownMenuItem onClick={() => handleViewEvents(order)}>
                                  <History className="h-4 w-4 mr-2" />
                                  Histórico de Eventos
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                {order.status === 'PENDING' && (
                                  <DropdownMenuItem 
                                    onClick={() => handleStatusUpdate(order.id || order.orderId, 'CONFIRMED')}
                                    className="text-green-600"
                                  >
                                    <CheckCircle2 className="h-4 w-4 mr-2" />
                                    Confirmar Pedido
                                  </DropdownMenuItem>
                                )}
                                {['PENDING', 'CONFIRMED'].includes(order.status) && (
                                  <DropdownMenuItem 
                                    onClick={() => handleStatusUpdate(order.id || order.orderId, 'CANCELLED')}
                                    className="text-red-600"
                                  >
                                    <Ban className="h-4 w-4 mr-2" />
                                    Cancelar Pedido
                                  </DropdownMenuItem>
                                )}
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
                
                {filteredAndSortedOrders.length === 0 && (
                  <div className="text-center py-12">
                    <div className="flex flex-col items-center gap-3">
                      <div className="p-3 bg-muted rounded-full">
                        <Database className="h-8 w-8 text-muted-foreground" />
                      </div>
                      <div>
                        <h3 className="text-lg font-semibold">Nenhum pedido encontrado</h3>
                        <p className="text-muted-foreground">
                          {orders.length === 0 
                            ? "Nenhum pedido foi criado ainda" 
                            : "Nenhum pedido corresponde aos filtros aplicados"}
                        </p>
                      </div>
                      <Button onClick={() => setCreateOrderOpen(true)}>
                        <Plus className="h-4 w-4 mr-2" />
                        Criar Primeiro Pedido
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            )}
            </TabsContent>
          </CardContent>
        </Tabs>
      </Card>

      {/* Create Order Dialog */}
      <Dialog open={createOrderOpen} onOpenChange={setCreateOrderOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Plus className="h-5 w-5" />
              Criar Novo Pedido
            </DialogTitle>
            <DialogDescription>
              Criar um novo pedido no sistema usando Event Sourcing
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreateOrder}>
            <div className="space-y-6">
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="customerId">ID do Cliente *</Label>
                  <Input
                    id="customerId"
                    value={customerId}
                    onChange={(e) => setCustomerId(e.target.value)}
                    placeholder="customer-123"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="totalAmount">Valor Total *</Label>
                  <Input
                    id="totalAmount"
                    type="number"
                    step="0.01"
                    min="0"
                    value={totalAmount}
                    onChange={(e) => setTotalAmount(e.target.value)}
                    placeholder="0.00"
                    required
                  />
                </div>
              </div>

              <div className="space-y-3">
                <Label>Produtos (opcional)</Label>
                <div className="space-y-2 max-h-48 overflow-y-auto">
                  {productIds.map((productId, index) => (
                    <div key={index} className="flex gap-2">
                      <Input
                        placeholder={`Produto ${index + 1}`}
                        value={productId}
                        onChange={(e) => updateProductId(index, e.target.value)}
                        className="flex-1"
                      />
                      {productIds.length > 1 && (
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => removeProductId(index)}
                        >
                          ×
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
                <Button type="button" variant="outline" size="sm" onClick={addProductId}>
                  <Package className="h-4 w-4 mr-2" />
                  Adicionar Produto
                </Button>
              </div>
            </div>
            
            <DialogFooter className="mt-6">
              <Button 
                type="button" 
                variant="outline" 
                onClick={() => {
                  setCreateOrderOpen(false)
                  resetForm()
                }}
              >
                Cancelar
              </Button>
              <Button 
                type="submit" 
                disabled={createOrderMutation.isPending}
              >
                {createOrderMutation.isPending ? (
                  <>
                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                    Criando...
                  </>
                ) : (
                  <>
                    <Plus className="h-4 w-4 mr-2" />
                    Criar Pedido
                  </>
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Order Details Dialog */}
      <Dialog open={!!selectedOrder && !viewEventsOpen} onOpenChange={() => setSelectedOrder(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Eye className="h-5 w-5" />
              Detalhes do Pedido
            </DialogTitle>
          </DialogHeader>
          {selectedOrder && (
            <div className="space-y-6">
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label>ID do Pedido</Label>
                  <div className="p-3 bg-muted rounded-lg font-mono text-sm">
                    {selectedOrder.id || selectedOrder.orderId}
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>Cliente</Label>
                  <div className="p-3 bg-muted rounded-lg">
                    {selectedOrder.customerId}
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>Status</Label>
                  <div className="flex items-center gap-2">
                    {(() => {
                      const statusConfig = getStatusConfig(selectedOrder.status)
                      const StatusIcon = statusConfig.icon
                      return (
                        <Badge className={cn("flex items-center gap-1", statusConfig.bgColor, statusConfig.color)}>
                          <StatusIcon className="h-3 w-3" />
                          {statusConfig.label}
                        </Badge>
                      )
                    })()}
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>Valor Total</Label>
                  <div className="p-3 bg-muted rounded-lg font-semibold">
                    {formatCurrency(selectedOrder.totalAmount || selectedOrder.total)}
                  </div>
                </div>
              </div>
              
              <div className="space-y-2">
                <Label>Data de Criação</Label>
                <div className="p-3 bg-muted rounded-lg">
                  {selectedOrder.createdAt 
                    ? new Date(selectedOrder.createdAt).toLocaleString('pt-BR')
                    : 'N/A'
                  }
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Order Events Dialog */}
      <Dialog open={viewEventsOpen} onOpenChange={setViewEventsOpen}>
        <DialogContent className="max-w-4xl max-h-[80vh]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <History className="h-5 w-5" />
              Histórico de Eventos
            </DialogTitle>
            <DialogDescription>
              Timeline completo de eventos do pedido {selectedOrder?.id} (Event Sourcing)
            </DialogDescription>
          </DialogHeader>
          
          <div className="flex-1 overflow-y-auto">
            {eventsLoading ? (
              <div className="flex items-center justify-center p-8">
                <RefreshCw className="h-6 w-6 animate-spin mr-2" />
                <span>Carregando eventos...</span>
              </div>
            ) : (
              <div className="space-y-3">
                {(eventsData || []).map((event: any, index: number) => (
                  <div key={event.id || index} className="border rounded-lg p-4 hover:bg-muted/50 transition-colors">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          <Badge variant="outline" className="font-mono text-xs">
                            {event.eventType || `Event-${index + 1}`}
                          </Badge>
                          <Badge variant="secondary" className="text-xs">
                            v{event.version || index + 1}
                          </Badge>
                        </div>
                        <p className="text-sm text-muted-foreground mb-2">
                          {event.occurredAt 
                            ? new Date(event.occurredAt).toLocaleString('pt-BR')
                            : 'Data não disponível'
                          }
                        </p>
                        {event.eventData && (
                          <div className="bg-background border rounded p-3">
                            <pre className="text-xs font-mono overflow-x-auto">
                              {typeof event.eventData === 'string' 
                                ? event.eventData 
                                : JSON.stringify(event.eventData, null, 2)
                              }
                            </pre>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
                
                {(!eventsData || eventsData.length === 0) && (
                  <div className="text-center py-12">
                    <div className="flex flex-col items-center gap-3">
                      <div className="p-3 bg-muted rounded-full">
                        <History className="h-8 w-8 text-muted-foreground" />
                      </div>
                      <div>
                        <h3 className="text-lg font-semibold">Nenhum evento encontrado</h3>
                        <p className="text-muted-foreground">
                          Este pedido ainda não possui eventos registrados
                        </p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}