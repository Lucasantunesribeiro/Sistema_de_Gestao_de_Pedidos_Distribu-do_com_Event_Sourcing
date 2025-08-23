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
import { useToast } from '@/components/ui/use-toast'
import apiClient from '@/lib/api'
import { formatCurrency } from '@/lib/utils'
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
  History
} from 'lucide-react'
import { useState } from 'react'

export function Orders() {
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedOrder, setSelectedOrder] = useState<any>(null)
  const [createOrderOpen, setCreateOrderOpen] = useState(false)
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [viewEventsOpen, setViewEventsOpen] = useState(false)
  const { toast } = useToast()
  const queryClient = useQueryClient()

  // Form states for creating orders
  const [customerId, setCustomerId] = useState('')
  const [totalAmount, setTotalAmount] = useState('')
  const [productIds, setProductIds] = useState<string[]>([''])

  // Fetch orders from your backend
  const { data: ordersResponse, isLoading, refetch } = useQuery({
    queryKey: ['orders'],
    queryFn: () => apiClient.getOrders(),
    refetchInterval: 30000,
  })

  // Fetch order events when viewing events
  const { data: eventsData, isLoading: eventsLoading } = useQuery({
    queryKey: ['order-events', selectedOrder?.id],
    queryFn: () => selectedOrder?.id ? apiClient.getOrderEvents(selectedOrder.id) : Promise.resolve([]),
    enabled: !!selectedOrder?.id && viewEventsOpen,
  })

  const orders = Array.isArray(ordersResponse) ? ordersResponse : []

  const filteredOrders = orders.filter((order: any) => {
    const matchesSearch = 
      order.id?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.customerId?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.orderId?.toLowerCase().includes(searchTerm.toLowerCase())
    
    const matchesStatus = statusFilter === 'all' || order.status === statusFilter
    
    return matchesSearch && matchesStatus
  })

  // Create order mutation
  const createOrderMutation = useMutation({
    mutationFn: (orderData: { customerId: string; totalAmount: number; productIds?: string[] }) =>
      apiClient.createOrder(orderData),
    onSuccess: (response) => {
      toast({
        title: "✅ Pedido criado com sucesso!",
        description: `Pedido ${response.orderId} criado com Event Sourcing.`,
      })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setCreateOrderOpen(false)
      resetForm()
    },
    onError: (error: any) => {
      toast({
        title: "❌ Erro ao criar pedido",
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
        title: "✅ Status atualizado!",
        description: "O status do pedido foi atualizado com Event Sourcing.",
      })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
    onError: (error: any) => {
      toast({
        title: "❌ Erro ao atualizar status",
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

  const getStatusIcon = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'COMPLETED':
      case 'PAID':
        return <CheckCircle className="h-4 w-4 text-green-500" />
      case 'CANCELLED':
      case 'FAILED':
        return <XCircle className="h-4 w-4 text-red-500" />
      case 'PENDING':
        return <Clock className="h-4 w-4 text-yellow-500" />
      case 'CONFIRMED':
        return <AlertCircle className="h-4 w-4 text-blue-500" />
      default:
        return <Activity className="h-4 w-4 text-gray-500" />
    }
  }

  const getStatusVariant = (status: string): "default" | "secondary" | "destructive" | "outline" => {
    switch (status?.toUpperCase()) {
      case 'COMPLETED':
      case 'PAID':
        return 'default'
      case 'CANCELLED':
      case 'FAILED':
        return 'destructive'
      case 'PENDING':
        return 'secondary'
      default:
        return 'outline'
    }
  }

  const handleCreateOrder = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!customerId || !totalAmount) {
      toast({
        title: "⚠️ Campos obrigatórios",
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

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Pedidos</h1>
          <p className="text-muted-foreground">
            Sistema de Gestão de Pedidos - Event Sourcing + CQRS
          </p>
          <div className="flex items-center gap-2 mt-2">
            <Badge variant="outline">
              <Database className="mr-1 h-3 w-3" />
              {orders.length} pedidos
            </Badge>
            <Badge variant="outline">
              <Activity className="mr-1 h-3 w-3" />
              API Conectada
            </Badge>
          </div>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => refetch()}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Atualizar
          </Button>
          <Button onClick={() => setCreateOrderOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Novo Pedido
          </Button>
        </div>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Search className="h-5 w-5" />
            Filtros e Busca
          </CardTitle>
          <CardDescription>
            Use os filtros para encontrar pedidos específicos
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex gap-4 items-end">
            <div className="flex-1">
              <Label htmlFor="search">Buscar Pedido</Label>
              <div className="relative">
                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  id="search"
                  placeholder="Buscar por ID do pedido ou cliente..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>
            <div className="w-48">
              <Label>Status</Label>
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Todos os status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todos os status</SelectItem>
                  <SelectItem value="PENDING">PENDING</SelectItem>
                  <SelectItem value="CONFIRMED">CONFIRMED</SelectItem>
                  <SelectItem value="PAID">PAID</SelectItem>
                  <SelectItem value="COMPLETED">COMPLETED</SelectItem>
                  <SelectItem value="CANCELLED">CANCELLED</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Orders Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <span>Lista de Pedidos</span>
            <Badge variant="outline">
              {filteredOrders.length} encontrado(s)
            </Badge>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-2">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="h-16 bg-muted animate-pulse rounded-lg" />
              ))}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID do Pedido</TableHead>
                  <TableHead>Cliente</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Total</TableHead>
                  <TableHead>Criado em</TableHead>
                  <TableHead className="text-center">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredOrders.map((order: any, index: number) => (
                  <TableRow key={order.id || index}>
                    <TableCell className="font-mono">
                      #{(order.id || order.orderId || `ORDER-${index + 1}`).slice(-8)}
                    </TableCell>
                    <TableCell className="font-medium">
                      {order.customerId || 'N/A'}
                    </TableCell>
                    <TableCell>
                      <Badge variant={getStatusVariant(order.status)} className="flex items-center gap-1 w-fit">
                        {getStatusIcon(order.status)}
                        {order.status || 'PENDING'}
                      </Badge>
                    </TableCell>
                    <TableCell className="font-semibold">
                      {formatCurrency(order.totalAmount || order.total || 0)}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {order.createdAt ? new Date(order.createdAt).toLocaleDateString('pt-BR') : 'N/A'}
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1 justify-center">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setSelectedOrder(order)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleViewEvents(order)}
                        >
                          <History className="h-4 w-4" />
                        </Button>
                        {(order.status === 'PENDING' || !order.status) && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleStatusUpdate(order.id || order.orderId, 'CONFIRMED')}
                          >
                            Confirmar
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
                {filteredOrders.length === 0 && !isLoading && (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      {orders.length === 0 ? (
                        <div className="space-y-2">
                          <Database className="h-8 w-8 mx-auto opacity-50" />
                          <p>Nenhum pedido encontrado no sistema</p>
                          <Button variant="outline" onClick={() => setCreateOrderOpen(true)}>
                            Criar primeiro pedido
                          </Button>
                        </div>
                      ) : (
                        'Nenhum pedido corresponde aos filtros'
                      )}
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
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
              Criar um novo pedido usando Event Sourcing. Os dados serão persistidos como eventos imutáveis.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreateOrder}>
            <div className="space-y-4">
              <div>
                <Label htmlFor="customerId">ID do Cliente *</Label>
                <Input
                  id="customerId"
                  value={customerId}
                  onChange={(e) => setCustomerId(e.target.value)}
                  placeholder="Ex: customer-123"
                  required
                />
              </div>

              <div>
                <Label htmlFor="totalAmount">Valor Total *</Label>
                <Input
                  id="totalAmount"
                  type="number"
                  step="0.01"
                  min="0"
                  value={totalAmount}
                  onChange={(e) => setTotalAmount(e.target.value)}
                  placeholder="Ex: 299.99"
                  required
                />
              </div>

              <div>
                <Label>IDs dos Produtos (opcional)</Label>
                <div className="space-y-2">
                  {productIds.map((productId, index) => (
                    <div key={index} className="flex gap-2">
                      <Input
                        placeholder="Ex: product-1"
                        value={productId}
                        onChange={(e) => updateProductId(index, e.target.value)}
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
                  <Button type="button" variant="outline" size="sm" onClick={addProductId}>
                    + Adicionar Produto
                  </Button>
                </div>
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
                className="gap-2"
              >
                {createOrderMutation.isPending ? (
                  <>
                    <RefreshCw className="h-4 w-4 animate-spin" />
                    Criando...
                  </>
                ) : (
                  <>
                    <Plus className="h-4 w-4" />
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
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>ID do Pedido</Label>
                  <p className="text-sm font-mono bg-muted p-2 rounded">
                    {selectedOrder.id || selectedOrder.orderId}
                  </p>
                </div>
                <div>
                  <Label>Cliente</Label>
                  <p className="text-sm">{selectedOrder.customerId}</p>
                </div>
                <div>
                  <Label>Status</Label>
                  <Badge variant={getStatusVariant(selectedOrder.status)} className="flex items-center gap-1 w-fit">
                    {getStatusIcon(selectedOrder.status)}
                    {selectedOrder.status}
                  </Badge>
                </div>
                <div>
                  <Label>Total</Label>
                  <p className="text-sm font-semibold">
                    {formatCurrency(selectedOrder.totalAmount || selectedOrder.total)}
                  </p>
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Order Events Dialog (Event Sourcing) */}
      <Dialog open={viewEventsOpen} onOpenChange={setViewEventsOpen}>
        <DialogContent className="max-w-4xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <History className="h-5 w-5" />
              Histórico de Eventos - Event Sourcing
            </DialogTitle>
            <DialogDescription>
              Eventos imutáveis do pedido {selectedOrder?.id}
            </DialogDescription>
          </DialogHeader>
          <div className="max-h-96 overflow-y-auto">
            {eventsLoading ? (
              <div className="flex items-center justify-center p-8">
                <RefreshCw className="h-6 w-6 animate-spin mr-2" />
                Carregando eventos...
              </div>
            ) : (
              <div className="space-y-2">
                {(eventsData || []).map((event: any, index: number) => (
                  <div key={event.id || index} className="border rounded-lg p-4 bg-muted/50">
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="font-semibold text-sm">{event.eventType}</h4>
                        <p className="text-xs text-muted-foreground">
                          {event.occurredAt ? new Date(event.occurredAt).toLocaleString('pt-BR') : 'N/A'}
                        </p>
                      </div>
                      <Badge variant="outline">v{event.version || index + 1}</Badge>
                    </div>
                    {event.eventData && (
                      <pre className="text-xs mt-2 bg-background p-2 rounded overflow-x-auto">
                        {typeof event.eventData === 'string' 
                          ? event.eventData 
                          : JSON.stringify(event.eventData, null, 2)
                        }
                      </pre>
                    )}
                  </div>
                ))}
                {(!eventsData || eventsData.length === 0) && (
                  <div className="text-center py-8 text-muted-foreground">
                    <History className="h-8 w-8 mx-auto mb-2 opacity-50" />
                    <p>Nenhum evento encontrado para este pedido</p>
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