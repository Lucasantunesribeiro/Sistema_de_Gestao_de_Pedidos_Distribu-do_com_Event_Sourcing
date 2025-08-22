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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useToast } from '@/components/ui/use-toast'
import api from '@/lib/api'
import { formatCurrency, formatDate } from '@/lib/utils'
import { OrderStatus, type Order, type OrderFilters } from '@/types'
import { useQuery } from '@tanstack/react-query'
import { Eye, Filter, Plus, Search, X } from 'lucide-react'
import { useState } from 'react'

export function Orders() {
  const [filters, setFilters] = useState<OrderFilters>({})
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [createOrderOpen, setCreateOrderOpen] = useState(false)
  const { toast } = useToast()

  const { data: orders, isLoading, refetch } = useQuery({
    queryKey: ['orders', filters],
    queryFn: () => api.getOrders().then(res => res.data),
  })

  const filteredOrders = orders?.filter(order =>
    order.orderId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    order.customerId.toLowerCase().includes(searchTerm.toLowerCase())
  ) || []

  const getStatusVariant = (status: OrderStatus): "default" | "secondary" | "destructive" | "outline" => {
    switch (status) {
      case OrderStatus.COMPLETED:
        return 'default'
      case OrderStatus.FAILED:
      case OrderStatus.CANCELLED:
        return 'destructive'
      case OrderStatus.PENDING:
      case OrderStatus.PAYMENT_PROCESSING:
        return 'secondary'
      default:
        return 'outline'
    }
  }

  const handleCancelOrder = async (orderId: string) => {
    try {
      await api.cancelOrder(orderId)
      toast({
        title: "Pedido cancelado",
        description: "O pedido foi cancelado com sucesso.",
      })
      refetch()
    } catch (error) {
      toast({
        title: "Erro ao cancelar pedido",
        description: "Não foi possível cancelar o pedido. Tente novamente.",
        variant: "destructive",
      })
    }
  }

  const CreateOrderDialog = () => {
    const [customerId, setCustomerId] = useState('')
    const [items, setItems] = useState([{ productId: '', productName: '', quantity: 1, price: 0 }])

    const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault()
      try {
        await api.createOrder({
          customerId,
          items: items.map(item => ({
            productId: item.productId,
            productName: item.productName,
            quantity: item.quantity,
            price: item.price,
          }))
        })
        toast({
          title: "Pedido criado",
          description: "O pedido foi criado com sucesso.",
        })
        setCreateOrderOpen(false)
        setCustomerId('')
        setItems([{ productId: '', productName: '', quantity: 1, price: 0 }])
        refetch()
      } catch (error) {
        toast({
          title: "Erro ao criar pedido",
          description: "Não foi possível criar o pedido. Tente novamente.",
          variant: "destructive",
        })
      }
    }

    const addItem = () => {
      setItems([...items, { productId: '', productName: '', quantity: 1, price: 0 }])
    }

    const removeItem = (index: number) => {
      setItems(items.filter((_, i) => i !== index))
    }

    const updateItem = (index: number, field: string, value: any) => {
      const newItems = [...items]
      newItems[index] = { ...newItems[index], [field]: value }
      setItems(newItems)
    }

    return (
      <Dialog open={createOrderOpen} onOpenChange={setCreateOrderOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Criar Novo Pedido</DialogTitle>
            <DialogDescription>
              Preencha os dados para criar um novo pedido no sistema.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSubmit}>
            <div className="space-y-4">
              <div>
                <Label htmlFor="customerId">ID do Cliente</Label>
                <Input
                  id="customerId"
                  value={customerId}
                  onChange={(e) => setCustomerId(e.target.value)}
                  placeholder="Digite o ID do cliente"
                  required
                />
              </div>

              <div>
                <Label>Itens do Pedido</Label>
                <div className="space-y-2">
                  {items.map((item, index) => (
                    <div key={index} className="flex gap-2 items-end">
                      <div className="flex-1">
                        <Input
                          placeholder="ID do Produto"
                          value={item.productId}
                          onChange={(e) => updateItem(index, 'productId', e.target.value)}
                          required
                        />
                      </div>
                      <div className="flex-1">
                        <Input
                          placeholder="Nome do Produto"
                          value={item.productName}
                          onChange={(e) => updateItem(index, 'productName', e.target.value)}
                          required
                        />
                      </div>
                      <div className="w-20">
                        <Input
                          type="number"
                          placeholder="Qtd"
                          value={item.quantity}
                          onChange={(e) => updateItem(index, 'quantity', parseInt(e.target.value))}
                          min="1"
                          required
                        />
                      </div>
                      <div className="w-24">
                        <Input
                          type="number"
                          placeholder="Preço"
                          value={item.price}
                          onChange={(e) => updateItem(index, 'price', parseFloat(e.target.value))}
                          min="0"
                          step="0.01"
                          required
                        />
                      </div>
                      {items.length > 1 && (
                        <Button
                          type="button"
                          variant="outline"
                          size="icon"
                          onClick={() => removeItem(index)}
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  ))}
                  <Button type="button" variant="outline" onClick={addItem}>
                    Adicionar Item
                  </Button>
                </div>
              </div>
            </div>
            <DialogFooter className="mt-6">
              <Button type="button" variant="outline" onClick={() => setCreateOrderOpen(false)}>
                Cancelar
              </Button>
              <Button type="submit">Criar Pedido</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    )
  }

  const OrderDetailsDialog = () => (
    <Dialog open={!!selectedOrder} onOpenChange={() => setSelectedOrder(null)}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Detalhes do Pedido #{selectedOrder?.orderId.slice(-8)}</DialogTitle>
          <DialogDescription>
            Informações completas do pedido
          </DialogDescription>
        </DialogHeader>
        {selectedOrder && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>ID do Pedido</Label>
                <p className="text-sm font-mono">{selectedOrder.orderId}</p>
              </div>
              <div>
                <Label>Cliente</Label>
                <p className="text-sm">{selectedOrder.customerId}</p>
              </div>
              <div>
                <Label>Status</Label>
                <Badge variant={getStatusVariant(selectedOrder.status)}>
                  {selectedOrder.status}
                </Badge>
              </div>
              <div>
                <Label>Total</Label>
                <p className="text-sm font-semibold">{formatCurrency(selectedOrder.totalAmount)}</p>
              </div>
              <div>
                <Label>Criado em</Label>
                <p className="text-sm">{formatDate(selectedOrder.createdAt)}</p>
              </div>
              <div>
                <Label>Atualizado em</Label>
                <p className="text-sm">{formatDate(selectedOrder.updatedAt)}</p>
              </div>
            </div>

            <div>
              <Label>Itens do Pedido</Label>
              <div className="mt-2 border rounded-lg">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Produto</TableHead>
                      <TableHead>Quantidade</TableHead>
                      <TableHead>Preço Unit.</TableHead>
                      <TableHead>Total</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {selectedOrder.items.map((item, index) => (
                      <TableRow key={index}>
                        <TableCell>
                          <div>
                            <p className="font-medium">{item.productName}</p>
                            <p className="text-sm text-muted-foreground">{item.productId}</p>
                          </div>
                        </TableCell>
                        <TableCell>{item.quantity}</TableCell>
                        <TableCell>{formatCurrency(item.unitPrice)}</TableCell>
                        <TableCell>{formatCurrency(item.totalPrice)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Pedidos</h1>
          <p className="text-muted-foreground">
            Gerencie todos os pedidos do sistema
          </p>
        </div>
        <Button onClick={() => setCreateOrderOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Novo Pedido
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Filtros</CardTitle>
          <CardDescription>
            Use os filtros abaixo para encontrar pedidos específicos
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex gap-4 items-end">
            <div className="flex-1">
              <Label htmlFor="search">Buscar</Label>
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
              <Select
                value={filters.status?.[0] || 'all'}
                onValueChange={(value) =>
                  setFilters({ ...filters, status: value === 'all' ? undefined : [value as OrderStatus] })
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Todos os status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todos os status</SelectItem>
                  {Object.values(OrderStatus).map((status) => (
                    <SelectItem key={status} value={status}>
                      {status}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <Button variant="outline">
              <Filter className="mr-2 h-4 w-4" />
              Mais Filtros
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Orders Table */}
      <Card>
        <CardHeader>
          <CardTitle>Lista de Pedidos</CardTitle>
          <CardDescription>
            {filteredOrders.length} pedido(s) encontrado(s)
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-2">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="h-12 bg-muted animate-pulse rounded" />
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
                  <TableHead>Data</TableHead>
                  <TableHead>Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredOrders.map((order) => (
                  <TableRow key={order.orderId}>
                    <TableCell className="font-mono">
                      #{order.orderId.slice(-8)}
                    </TableCell>
                    <TableCell>{order.customerId}</TableCell>
                    <TableCell>
                      <Badge variant={getStatusVariant(order.status)}>
                        {order.status}
                      </Badge>
                    </TableCell>
                    <TableCell>{formatCurrency(order.totalAmount)}</TableCell>
                    <TableCell>{formatDate(order.createdAt)}</TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setSelectedOrder(order)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        {(order.status === OrderStatus.PENDING || order.status === OrderStatus.PAYMENT_PROCESSING) && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleCancelOrder(order.orderId)}
                          >
                            Cancelar
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
                {filteredOrders.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      Nenhum pedido encontrado
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <CreateOrderDialog />
      <OrderDetailsDialog />
    </div>
  )
}