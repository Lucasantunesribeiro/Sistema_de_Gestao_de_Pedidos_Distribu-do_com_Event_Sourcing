import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Search, Package, AlertTriangle, CheckCircle } from 'lucide-react'
import { formatDate } from '@/lib/utils'
import { useState } from 'react'
import api from '@/lib/api'

export function Inventory() {
  const [searchTerm, setSearchTerm] = useState('')

  const { data: inventory, isLoading } = useQuery({
    queryKey: ['inventory'],
    queryFn: () => api.getInventory().then(res => res.data),
    refetchInterval: 30000, // Refresh every 30 seconds
  })

  const filteredInventory = inventory?.filter(item => 
    item.productName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    item.productId.toLowerCase().includes(searchTerm.toLowerCase())
  ) || []

  const getStockStatus = (item: any) => {
    const availablePercentage = (item.availableQuantity / item.totalQuantity) * 100
    
    if (item.availableQuantity === 0) {
      return { status: 'OUT_OF_STOCK', label: 'Sem Estoque', variant: 'destructive' as const, icon: AlertTriangle }
    } else if (availablePercentage <= 20) {
      return { status: 'LOW_STOCK', label: 'Estoque Baixo', variant: 'secondary' as const, icon: AlertTriangle }
    } else {
      return { status: 'IN_STOCK', label: 'Em Estoque', variant: 'default' as const, icon: CheckCircle }
    }
  }

  const totalProducts = filteredInventory.length
  const outOfStock = filteredInventory.filter(item => item.availableQuantity === 0).length
  const lowStock = filteredInventory.filter(item => {
    const percentage = (item.availableQuantity / item.totalQuantity) * 100
    return percentage > 0 && percentage <= 20
  }).length
  const totalValue = filteredInventory.reduce((sum, item) => sum + (item.totalQuantity * 10), 0) // Assuming $10 per unit

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Estoque</h1>
        <p className="text-muted-foreground">
          Monitore e gerencie o inventário de produtos
        </p>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Total de Produtos
            </CardTitle>
            <Package className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalProducts}</div>
            <p className="text-xs text-muted-foreground">
              Produtos cadastrados
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Sem Estoque
            </CardTitle>
            <AlertTriangle className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{outOfStock}</div>
            <p className="text-xs text-muted-foreground">
              Produtos esgotados
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Estoque Baixo
            </CardTitle>
            <AlertTriangle className="h-4 w-4 text-yellow-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-yellow-600">{lowStock}</div>
            <p className="text-xs text-muted-foreground">
              Produtos com estoque baixo
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Valor Total
            </CardTitle>
            <CheckCircle className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">R$ {totalValue.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">
              Valor estimado do estoque
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Search */}
      <Card>
        <CardHeader>
          <CardTitle>Buscar Produtos</CardTitle>
          <CardDescription>
            Encontre produtos específicos no inventário
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
                  placeholder="Buscar por nome ou ID do produto..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Inventory Table */}
      <Card>
        <CardHeader>
          <CardTitle>Lista de Produtos</CardTitle>
          <CardDescription>
            {filteredInventory.length} produto(s) encontrado(s)
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
                  <TableHead>Produto</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Disponível</TableHead>
                  <TableHead>Reservado</TableHead>
                  <TableHead>Total</TableHead>
                  <TableHead>Última Atualização</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredInventory.map((item) => {
                  const stockStatus = getStockStatus(item)
                  const StatusIcon = stockStatus.icon
                  
                  return (
                    <TableRow key={item.productId}>
                      <TableCell>
                        <div>
                          <p className="font-medium">{item.productName}</p>
                          <p className="text-sm text-muted-foreground font-mono">
                            {item.productId}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <StatusIcon className="h-4 w-4" />
                          <Badge variant={stockStatus.variant}>
                            {stockStatus.label}
                          </Badge>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className={`font-medium ${
                          item.availableQuantity === 0 ? 'text-red-600' : 
                          item.availableQuantity <= item.totalQuantity * 0.2 ? 'text-yellow-600' : 
                          'text-green-600'
                        }`}>
                          {item.availableQuantity}
                        </span>
                      </TableCell>
                      <TableCell>
                        <span className="text-blue-600 font-medium">
                          {item.reservedQuantity}
                        </span>
                      </TableCell>
                      <TableCell>
                        <span className="font-medium">
                          {item.totalQuantity}
                        </span>
                      </TableCell>
                      <TableCell>
                        {formatDate(item.lastUpdated)}
                      </TableCell>
                    </TableRow>
                  )
                })}
                {filteredInventory.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      Nenhum produto encontrado
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}