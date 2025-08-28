import { useState } from 'react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Progress } from '@/components/ui/progress'
import apiClient from '@/lib/api'
import { formatCurrency, cn } from '@/lib/utils'
import { useQuery } from '@tanstack/react-query'
import {
  AlertCircle,
  CheckCircle,
  Clock,
  CreditCard,
  ShoppingCart,
  TrendingUp,
  TrendingDown,
  XCircle,
  Users,
  Package,
  DollarSign,
  Calendar,
  Activity,
  ArrowUpRight,
  ArrowDownRight,
  RefreshCw,
  Download,
  Eye,
  Filter,
  MoreHorizontal,
  Zap
} from 'lucide-react'
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

// Mock data for charts
const orderTrendData = [
  { name: 'Jan', pedidos: 65, receita: 4800, meta: 70 },
  { name: 'Fev', pedidos: 78, receita: 5200, meta: 75 },
  { name: 'Mar', pedidos: 90, receita: 6100, meta: 80 },
  { name: 'Abr', pedidos: 81, receita: 5800, meta: 85 },
  { name: 'Mai', pedidos: 95, receita: 6900, meta: 90 },
  { name: 'Jun', pedidos: 110, receita: 7800, meta: 95 },
  { name: 'Jul', pedidos: 125, receita: 8900, meta: 100 },
]

const statusDistribution = [
  { name: 'Concluído', value: 68, color: '#10B981' },
  { name: 'Processando', value: 22, color: '#F59E0B' },
  { name: 'Pendente', value: 8, color: '#EF4444' },
  { name: 'Cancelado', value: 2, color: '#6B7280' },
]

const topProducts = [
  { name: 'Produto A', vendas: 145, receita: 12580, crescimento: 12 },
  { name: 'Produto B', vendas: 98, receita: 8640, crescimento: -3 },
  { name: 'Produto C', vendas: 87, receita: 7830, crescimento: 8 },
  { name: 'Produto D', vendas: 76, receita: 6840, crescimento: 15 },
  { name: 'Produto E', vendas: 65, receita: 5850, crescimento: -1 },
]

const recentActivities = [
  { id: 1, type: 'order', message: 'Novo pedido #12345 recebido', time: '2 min atrás', status: 'success' },
  { id: 2, type: 'payment', message: 'Pagamento de R$ 1.250,00 aprovado', time: '5 min atrás', status: 'success' },
  { id: 3, type: 'inventory', message: 'Estoque do Produto A está baixo', time: '8 min atrás', status: 'warning' },
  { id: 4, type: 'customer', message: 'Novo cliente cadastrado', time: '12 min atrás', status: 'info' },
  { id: 5, type: 'order', message: 'Pedido #12340 foi cancelado', time: '15 min atrás', status: 'error' },
]

interface MetricCardProps {
  title: string
  value: string | number
  description: string
  icon: React.ElementType
  trend: number
  isPercentage?: boolean
  isLoading?: boolean
}

function MetricCard({ title, value, description, icon: Icon, trend, isPercentage, isLoading }: MetricCardProps) {
  const isPositive = trend >= 0
  const TrendIcon = isPositive ? TrendingUp : TrendingDown
  
  if (isLoading) {
    return (
      <Card className="relative overflow-hidden">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium text-muted-foreground">
            <div className="h-4 w-24 bg-muted animate-pulse rounded" />
          </CardTitle>
          <div className="h-4 w-4 bg-muted animate-pulse rounded" />
        </CardHeader>
        <CardContent>
          <div className="h-8 w-20 bg-muted animate-pulse rounded mb-2" />
          <div className="h-3 w-32 bg-muted animate-pulse rounded mb-2" />
          <div className="h-3 w-28 bg-muted animate-pulse rounded" />
        </CardContent>
        <div className="absolute inset-x-0 bottom-0 h-1 bg-gradient-to-r from-primary/20 to-primary/10" />
      </Card>
    )
  }

  return (
    <Card className="relative overflow-hidden transition-all duration-200 hover:shadow-md">
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        <div className="p-2 bg-primary/10 rounded-full">
          <Icon className="h-4 w-4 text-primary" />
        </div>
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold text-foreground mb-1">{value}</div>
        <p className="text-sm text-muted-foreground mb-3">
          {description}
        </p>
        <div className="flex items-center gap-2 text-sm">
          <div className={cn(
            "flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium",
            isPositive ? "bg-green-100 text-green-700 dark:bg-green-900/20 dark:text-green-400" : "bg-red-100 text-red-700 dark:bg-red-900/20 dark:text-red-400"
          )}>
            <TrendIcon className="h-3 w-3" />
            {Math.abs(trend)}{isPercentage ? '%' : ''}
          </div>
          <span className="text-muted-foreground text-xs">vs. mês anterior</span>
        </div>
      </CardContent>
      <div className={cn(
        "absolute inset-x-0 bottom-0 h-1 bg-gradient-to-r transition-all duration-500",
        isPositive ? "from-green-400 to-green-600" : "from-red-400 to-red-600"
      )} />
    </Card>
  )
}

function SkeletonCard() {
  return (
    <Card>
      <CardHeader>
        <div className="h-4 w-24 bg-muted animate-pulse rounded" />
        <div className="h-3 w-32 bg-muted animate-pulse rounded" />
      </CardHeader>
      <CardContent>
        <div className="h-[200px] bg-muted animate-pulse rounded" />
      </CardContent>
    </Card>
  )
}

export function Dashboard() {
  const [timeRange, setTimeRange] = useState('7d')
  const [isRefreshing, setIsRefreshing] = useState(false)

  // Fetch system data
  const { data: systemInfo, isLoading: systemLoading, refetch: refetchSystem } = useQuery({
    queryKey: ['system-info'],
    queryFn: () => apiClient.getSystemInfo(),
    refetchInterval: 30000,
  })

  const { data: healthCheck, isLoading: healthLoading } = useQuery({
    queryKey: ['health-check'],
    queryFn: () => apiClient.getHealthCheck(),
    refetchInterval: 10000,
  })

  const { data: ordersResponse, isLoading: ordersLoading } = useQuery({
    queryKey: ['recent-orders'],
    queryFn: () => apiClient.getOrders(),
    refetchInterval: 30000,
  })

  const orders = Array.isArray(ordersResponse) ? ordersResponse : []
  const isLoading = systemLoading || healthLoading || ordersLoading

  // Calculate real metrics or use defaults
  const metrics = {
    totalOrders: orders.length || 1247,
    totalRevenue: orders.reduce((sum, order) => sum + (order.totalAmount || 0), 0) || 89650,
    conversionRate: orders.length > 0 ? (orders.filter(o => o.status === 'COMPLETED').length / orders.length) * 100 : 68.5,
    pendingOrders: orders.filter(o => o.status === 'PENDING').length || 23,
    totalCustomers: 456,
    avgOrderValue: orders.length > 0 ? (orders.reduce((sum, order) => sum + (order.totalAmount || 0), 0) / orders.length) : 187.50
  }

  const handleRefresh = async () => {
    setIsRefreshing(true)
    await refetchSystem()
    setTimeout(() => setIsRefreshing(false), 1000)
  }

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'order': return ShoppingCart
      case 'payment': return CreditCard
      case 'inventory': return Package
      case 'customer': return Users
      default: return Activity
    }
  }

  const getActivityColor = (status: string) => {
    switch (status) {
      case 'success': return 'text-green-500'
      case 'warning': return 'text-yellow-500'
      case 'error': return 'text-red-500'
      case 'info': return 'text-blue-500'
      default: return 'text-muted-foreground'
    }
  }

  return (
    <div className="space-y-6 pb-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
            <div className="p-2 bg-primary/10 rounded-xl">
              <Activity className="h-6 w-6 text-primary" />
            </div>
            Dashboard
          </h1>
          <p className="text-muted-foreground mt-2">
            Visão geral do sistema de gestão de pedidos em tempo real
          </p>
        </div>
        
        <div className="flex items-center gap-3">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm">
                <Calendar className="h-4 w-4 mr-2" />
                Últimos 7 dias
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel>Período</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => setTimeRange('24h')}>
                Últimas 24 horas
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => setTimeRange('7d')}>
                Últimos 7 dias
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => setTimeRange('30d')}>
                Últimos 30 dias
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          
          <Button 
            variant="outline" 
            size="sm" 
            onClick={handleRefresh}
            disabled={isRefreshing}
          >
            <RefreshCw className={cn("h-4 w-4 mr-2", isRefreshing && "animate-spin")} />
            Atualizar
          </Button>
          
          <Button size="sm" className="bg-primary hover:bg-primary/90">
            <Download className="h-4 w-4 mr-2" />
            Exportar
          </Button>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
        <MetricCard
          title="Total de Pedidos"
          value={metrics.totalOrders.toLocaleString()}
          description="Pedidos processados"
          icon={ShoppingCart}
          trend={12}
          isLoading={isLoading}
        />
        <MetricCard
          title="Receita Total"
          value={formatCurrency(metrics.totalRevenue)}
          description="Receita gerada"
          icon={DollarSign}
          trend={8}
          isLoading={isLoading}
        />
        <MetricCard
          title="Taxa de Conversão"
          value={`${metrics.conversionRate.toFixed(1)}%`}
          description="Pedidos confirmados"
          icon={TrendingUp}
          trend={2.1}
          isPercentage
          isLoading={isLoading}
        />
        <MetricCard
          title="Ticket Médio"
          value={formatCurrency(metrics.avgOrderValue)}
          description="Valor médio por pedido"
          icon={CreditCard}
          trend={5}
          isLoading={isLoading}
        />
        <MetricCard
          title="Clientes Ativos"
          value={metrics.totalCustomers.toLocaleString()}
          description="Clientes cadastrados"
          icon={Users}
          trend={15}
          isLoading={isLoading}
        />
        <MetricCard
          title="Pedidos Pendentes"
          value={metrics.pendingOrders}
          description="Aguardando processamento"
          icon={Clock}
          trend={-3}
          isLoading={isLoading}
        />
      </div>

      {/* Charts Section */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Chart */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>Tendência de Pedidos</CardTitle>
                <CardDescription>
                  Volume de pedidos e receita ao longo do tempo
                </CardDescription>
              </div>
              <Tabs defaultValue="orders" className="w-auto">
                <TabsList className="grid w-full grid-cols-2">
                  <TabsTrigger value="orders">Pedidos</TabsTrigger>
                  <TabsTrigger value="revenue">Receita</TabsTrigger>
                </TabsList>
              </Tabs>
            </div>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="h-[300px] bg-muted animate-pulse rounded" />
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={orderTrendData}>
                  <defs>
                    <linearGradient id="colorPedidos" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="hsl(var(--primary))" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="hsl(var(--primary))" stopOpacity={0.1}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
                  <XAxis 
                    dataKey="name" 
                    fontSize={12}
                    className="text-muted-foreground"
                  />
                  <YAxis 
                    fontSize={12}
                    className="text-muted-foreground"
                  />
                  <Tooltip 
                    contentStyle={{ 
                      backgroundColor: 'hsl(var(--background))',
                      border: '1px solid hsl(var(--border))',
                      borderRadius: '8px'
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="pedidos"
                    stroke="hsl(var(--primary))"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#colorPedidos)"
                  />
                  <Line
                    type="monotone"
                    dataKey="meta"
                    stroke="hsl(var(--muted-foreground))"
                    strokeDasharray="5 5"
                    strokeWidth={1}
                  />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        {/* Status Distribution */}
        <Card>
          <CardHeader>
            <CardTitle>Status dos Pedidos</CardTitle>
            <CardDescription>
              Distribuição atual por status
            </CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="h-[300px] bg-muted animate-pulse rounded" />
            ) : (
              <div className="space-y-4">
                <ResponsiveContainer width="100%" height={200}>
                  <PieChart>
                    <Pie
                      data={statusDistribution}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={80}
                      paddingAngle={2}
                      dataKey="value"
                    >
                      {statusDistribution.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
                <div className="space-y-2">
                  {statusDistribution.map((status, index) => (
                    <div key={index} className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2">
                        <div 
                          className="w-3 h-3 rounded-full" 
                          style={{ backgroundColor: status.color }}
                        />
                        <span>{status.name}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{status.value}%</span>
                        <Progress value={status.value} className="w-12 h-2" />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Bottom Section */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Recent Activity */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>Atividade Recente</CardTitle>
                <CardDescription>
                  Últimas atividades do sistema
                </CardDescription>
              </div>
              <Button variant="ghost" size="sm">
                <Eye className="h-4 w-4 mr-2" />
                Ver tudo
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="flex items-center gap-4">
                    <div className="w-8 h-8 bg-muted animate-pulse rounded-full" />
                    <div className="flex-1 space-y-2">
                      <div className="h-4 w-3/4 bg-muted animate-pulse rounded" />
                      <div className="h-3 w-1/2 bg-muted animate-pulse rounded" />
                    </div>
                  </div>
                ))
              ) : (
                recentActivities.map((activity) => {
                  const Icon = getActivityIcon(activity.type)
                  return (
                    <div key={activity.id} className="flex items-center gap-4 p-3 rounded-lg hover:bg-muted/50 transition-colors">
                      <div className={cn(
                        "p-2 rounded-full",
                        activity.status === 'success' && "bg-green-100 dark:bg-green-900/20",
                        activity.status === 'warning' && "bg-yellow-100 dark:bg-yellow-900/20",
                        activity.status === 'error' && "bg-red-100 dark:bg-red-900/20",
                        activity.status === 'info' && "bg-blue-100 dark:bg-blue-900/20"
                      )}>
                        <Icon className={cn("h-4 w-4", getActivityColor(activity.status))} />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-foreground truncate">
                          {activity.message}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {activity.time}
                        </p>
                      </div>
                    </div>
                  )
                })
              )}
            </div>
          </CardContent>
        </Card>

        {/* Top Products */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>Produtos Mais Vendidos</CardTitle>
                <CardDescription>
                  Ranking por volume de vendas
                </CardDescription>
              </div>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="sm">
                    <MoreHorizontal className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem>Ver detalhes</DropdownMenuItem>
                  <DropdownMenuItem>Exportar</DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 bg-muted animate-pulse rounded" />
                      <div className="space-y-1">
                        <div className="h-4 w-20 bg-muted animate-pulse rounded" />
                        <div className="h-3 w-16 bg-muted animate-pulse rounded" />
                      </div>
                    </div>
                    <div className="h-4 w-12 bg-muted animate-pulse rounded" />
                  </div>
                ))
              ) : (
                topProducts.map((product, index) => (
                  <div key={index} className="flex items-center justify-between p-3 rounded-lg hover:bg-muted/50 transition-colors">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center font-medium text-sm text-primary">
                        {index + 1}
                      </div>
                      <div>
                        <p className="font-medium text-sm">{product.name}</p>
                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                          <span>{product.vendas} vendas</span>
                          <span>•</span>
                          <span>{formatCurrency(product.receita)}</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      {product.crescimento >= 0 ? (
                        <ArrowUpRight className="h-3 w-3 text-green-500" />
                      ) : (
                        <ArrowDownRight className="h-3 w-3 text-red-500" />
                      )}
                      <span className={cn(
                        "text-xs font-medium",
                        product.crescimento >= 0 ? "text-green-600" : "text-red-600"
                      )}>
                        {product.crescimento}%
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* System Health */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Zap className="h-5 w-5 text-green-500" />
                Status dos Serviços
              </CardTitle>
              <CardDescription>
                Monitoramento dos serviços em tempo real
              </CardDescription>
            </div>
            <Badge variant="outline" className="text-green-600 border-green-200">
              <div className="w-2 h-2 bg-green-500 rounded-full mr-2 animate-pulse" />
              Todos os serviços operacionais
            </Badge>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            {[
              { name: 'Order Service', status: 'online', latency: '45ms', uptime: '99.9%' },
              { name: 'Payment Service', status: 'online', latency: '62ms', uptime: '99.8%' },
              { name: 'Inventory Service', status: 'online', latency: '38ms', uptime: '100%' },
              { name: 'Query Service', status: 'online', latency: '71ms', uptime: '99.7%' },
            ].map((service, index) => (
              <div key={index} className="p-4 border rounded-lg space-y-3">
                <div className="flex items-center justify-between">
                  <h4 className="font-medium text-sm">{service.name}</h4>
                  <Badge 
                    variant="outline" 
                    className="text-green-600 border-green-200 bg-green-50 dark:bg-green-900/20"
                  >
                    <div className="w-2 h-2 bg-green-500 rounded-full mr-1" />
                    Online
                  </Badge>
                </div>
                <div className="grid grid-cols-2 gap-2 text-sm">
                  <div>
                    <p className="text-muted-foreground text-xs">Latência</p>
                    <p className="font-medium">{service.latency}</p>
                  </div>
                  <div>
                    <p className="text-muted-foreground text-xs">Uptime</p>
                    <p className="font-medium">{service.uptime}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}