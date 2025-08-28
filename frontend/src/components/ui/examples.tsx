/**
 * EXEMPLOS DE USO DOS COMPONENTES AVANÇADOS
 * DataTable, Modal, NavigationSidebar
 */

import React from 'react'
import { 
  Edit, 
  Trash2, 
  Eye, 
  Package, 
  ShoppingCart, 
  DollarSign, 
  Users,
  Settings,
  BarChart3,
  FileText
} from 'lucide-react'
import { DataTable, type TableColumn, type TableAction } from './data-table'
import { Modal, ModalHeader, ModalTitle, ModalBody, ModalFooter, useModal } from './modal'
import { NavigationSidebar, type SidebarSection } from './navigation-sidebar'
import { Button } from './button'

// =============================================================================
// EXEMPLO 1: DATA TABLE COMPLETA
// =============================================================================

// Tipos para os dados de exemplo
interface Order {
  id: string
  customerName: string
  total: number
  status: 'pending' | 'processing' | 'completed' | 'cancelled'
  date: string
  items: number
}

// Dados de exemplo
const ordersData: Order[] = [
  {
    id: '001',
    customerName: 'João Silva',
    total: 299.99,
    status: 'completed',
    date: '2024-01-15',
    items: 3
  },
  {
    id: '002', 
    customerName: 'Maria Santos',
    total: 149.50,
    status: 'processing',
    date: '2024-01-14',
    items: 2
  },
  {
    id: '003',
    customerName: 'Pedro Costa',
    total: 89.90,
    status: 'pending',
    date: '2024-01-13',
    items: 1
  }
]

// Componente de status
const StatusBadge: React.FC<{ status: Order['status'] }> = ({ status }) => {
  const variants = {
    pending: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-400',
    processing: 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-400',
    completed: 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400',
    cancelled: 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400'
  }
  
  const labels = {
    pending: 'Pendente',
    processing: 'Processando',
    completed: 'Completo',
    cancelled: 'Cancelado'
  }
  
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${variants[status]}`}>
      {labels[status]}
    </span>
  )
}

export const DataTableExample: React.FC = () => {
  const [selectedRows, setSelectedRows] = React.useState<Order[]>([])
  const [page, setPage] = React.useState(1)
  const [pageSize, setPageSize] = React.useState(10)
  const [loading, setLoading] = React.useState(false)
  
  // Definir colunas
  const columns: TableColumn<Order>[] = [
    {
      key: 'id',
      title: 'ID',
      accessor: 'id',
      sortable: true,
      width: '80px'
    },
    {
      key: 'customerName',
      title: 'Cliente',
      accessor: 'customerName',
      sortable: true
    },
    {
      key: 'items',
      title: 'Items',
      accessor: 'items',
      sortable: true,
      width: '80px'
    },
    {
      key: 'total',
      title: 'Total',
      accessor: (order) => `R$ ${order.total.toFixed(2)}`,
      sortable: true,
      width: '100px'
    },
    {
      key: 'status',
      title: 'Status',
      accessor: (order) => <StatusBadge status={order.status} />,
      sortable: true,
      width: '120px'
    },
    {
      key: 'date',
      title: 'Data',
      accessor: (order) => new Date(order.date).toLocaleDateString('pt-BR'),
      sortable: true,
      width: '100px'
    }
  ]
  
  // Definir ações
  const actions: TableAction<Order>[] = [
    {
      label: 'Ver',
      icon: Eye,
      onClick: (order) => console.log('Ver pedido:', order.id)
    },
    {
      label: 'Editar',
      icon: Edit,
      onClick: (order) => console.log('Editar pedido:', order.id),
      disabled: (order) => order.status === 'completed'
    },
    {
      label: 'Excluir',
      icon: Trash2,
      variant: 'danger',
      onClick: (order) => console.log('Excluir pedido:', order.id),
      disabled: (order) => order.status === 'processing'
    }
  ]
  
  const handleSort = (column: string, direction: 'asc' | 'desc' | null) => {
    console.log('Ordenar por:', column, direction)
  }
  
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Pedidos</h2>
        <div className="flex gap-2">
          <Button variant="outline">
            Filtros
          </Button>
          <Button>
            Novo Pedido
          </Button>
        </div>
      </div>
      
      {selectedRows.length > 0 && (
        <div className="bg-muted p-3 rounded-lg">
          <span className="text-sm font-medium">
            {selectedRows.length} item(s) selecionado(s)
          </span>
          <Button 
            variant="outline" 
            size="sm" 
            className="ml-4"
            onClick={() => setSelectedRows([])}
          >
            Limpar seleção
          </Button>
        </div>
      )}
      
      <DataTable
        data={ordersData}
        columns={columns}
        actions={actions}
        selectable
        selectedRows={selectedRows}
        onSelectionChange={setSelectedRows}
        onSort={handleSort}
        loading={loading}
        pagination={{
          page,
          pageSize,
          total: 50, // Total simulado
          onPageChange: setPage,
          onPageSizeChange: setPageSize
        }}
        emptyMessage="Nenhum pedido encontrado"
        className="w-full"
      />
    </div>
  )
}

// =============================================================================
// EXEMPLO 2: MODAL AVANÇADO
// =============================================================================

export const ModalExample: React.FC = () => {
  const confirmModal = useModal()
  const editModal = useModal()
  const fullscreenModal = useModal()
  
  return (
    <div className="space-y-4">
      <h2 className="text-2xl font-bold">Modais</h2>
      
      <div className="flex gap-4">
        <Button onClick={confirmModal.openModal}>
          Modal de Confirmação
        </Button>
        <Button variant="secondary" onClick={editModal.openModal}>
          Modal de Edição
        </Button>
        <Button variant="outline" onClick={fullscreenModal.openModal}>
          Modal Fullscreen
        </Button>
      </div>
      
      {/* Modal de Confirmação */}
      <Modal 
        open={confirmModal.open}
        onOpenChange={confirmModal.setOpen}
        size="sm"
      >
        <ModalHeader>
          <ModalTitle>Confirmar Exclusão</ModalTitle>
        </ModalHeader>
        <ModalBody>
          <p>Tem certeza que deseja excluir este item? Esta ação não pode ser desfeita.</p>
        </ModalBody>
        <ModalFooter>
          <Button 
            variant="outline" 
            onClick={confirmModal.closeModal}
          >
            Cancelar
          </Button>
          <Button 
            variant="danger"
            onClick={() => {
              console.log('Item excluído')
              confirmModal.closeModal()
            }}
          >
            Excluir
          </Button>
        </ModalFooter>
      </Modal>
      
      {/* Modal de Edição */}
      <Modal 
        open={editModal.open}
        onOpenChange={editModal.setOpen}
        size="lg"
      >
        <ModalHeader>
          <ModalTitle>Editar Produto</ModalTitle>
        </ModalHeader>
        <ModalBody>
          <form className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2">
                Nome do Produto
              </label>
              <input 
                type="text" 
                className="w-full p-2 border rounded-lg"
                defaultValue="Produto Exemplo"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-2">
                Descrição
              </label>
              <textarea 
                className="w-full p-2 border rounded-lg h-24"
                defaultValue="Descrição do produto..."
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-2">
                  Preço
                </label>
                <input 
                  type="number" 
                  className="w-full p-2 border rounded-lg"
                  defaultValue="99.99"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">
                  Estoque
                </label>
                <input 
                  type="number" 
                  className="w-full p-2 border rounded-lg"
                  defaultValue="100"
                />
              </div>
            </div>
          </form>
        </ModalBody>
        <ModalFooter>
          <Button 
            variant="outline" 
            onClick={editModal.closeModal}
          >
            Cancelar
          </Button>
          <Button onClick={editModal.closeModal}>
            Salvar
          </Button>
        </ModalFooter>
      </Modal>
      
      {/* Modal Fullscreen */}
      <Modal 
        open={fullscreenModal.open}
        onOpenChange={fullscreenModal.setOpen}
        size="full"
      >
        <ModalHeader>
          <ModalTitle>Relatório Completo</ModalTitle>
        </ModalHeader>
        <ModalBody>
          <div className="h-96 bg-muted rounded-lg flex items-center justify-center">
            <p className="text-muted-foreground">Conteúdo do relatório em tela cheia</p>
          </div>
        </ModalBody>
        <ModalFooter>
          <Button 
            variant="outline" 
            onClick={fullscreenModal.closeModal}
          >
            Fechar
          </Button>
          <Button>
            Exportar PDF
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  )
}

// =============================================================================
// EXEMPLO 3: NAVIGATION SIDEBAR
// =============================================================================

export const SidebarExample: React.FC = () => {
  const [activeItem, setActiveItem] = React.useState('dashboard')
  const [collapsed, setCollapsed] = React.useState(false)
  
  // Definir seções do menu
  const sections: SidebarSection[] = [
    {
      id: 'main',
      title: 'Principal',
      items: [
        {
          id: 'dashboard',
          label: 'Dashboard',
          icon: BarChart3,
          href: '/dashboard'
        },
        {
          id: 'orders',
          label: 'Pedidos',
          icon: ShoppingCart,
          badge: '12',
          children: [
            {
              id: 'orders-all',
              label: 'Todos os Pedidos',
              href: '/orders'
            },
            {
              id: 'orders-pending',
              label: 'Pendentes',
              href: '/orders/pending',
              badge: '5'
            },
            {
              id: 'orders-completed',
              label: 'Concluídos',
              href: '/orders/completed'
            }
          ]
        },
        {
          id: 'products',
          label: 'Produtos',
          icon: Package,
          href: '/products'
        },
        {
          id: 'customers',
          label: 'Clientes',
          icon: Users,
          href: '/customers'
        },
        {
          id: 'payments',
          label: 'Pagamentos',
          icon: DollarSign,
          href: '/payments'
        }
      ]
    },
    {
      id: 'reports',
      title: 'Relatórios',
      items: [
        {
          id: 'sales-report',
          label: 'Vendas',
          icon: FileText,
          href: '/reports/sales'
        },
        {
          id: 'inventory-report',
          label: 'Estoque',
          icon: FileText,
          href: '/reports/inventory'
        }
      ]
    }
  ]
  
  const user = {
    name: 'João Silva',
    email: 'joao@exemplo.com',
    role: 'Administrador'
  }
  
  const handleItemClick = (item: any) => {
    setActiveItem(item.id)
    console.log('Navegando para:', item)
  }
  
  const handleUserMenuClick = (action: 'profile' | 'settings' | 'logout') => {
    console.log('Ação do usuário:', action)
  }
  
  return (
    <div className="flex h-screen bg-muted/30">
      <NavigationSidebar
        sections={sections}
        user={user}
        activeItemId={activeItem}
        collapsed={collapsed}
        onCollapsedChange={setCollapsed}
        onItemClick={handleItemClick}
        onUserMenuClick={handleUserMenuClick}
        persistState
      />
      
      {/* Conteúdo principal */}
      <main className="flex-1 p-8 overflow-y-auto">
        <div className="max-w-4xl">
          <h1 className="text-3xl font-bold mb-8">
            Sistema de Gestão de Pedidos
          </h1>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
            <div className="bg-background p-6 rounded-lg border">
              <h3 className="font-semibold mb-2">Total de Pedidos</h3>
              <p className="text-2xl font-bold text-primary">1,234</p>
            </div>
            <div className="bg-background p-6 rounded-lg border">
              <h3 className="font-semibold mb-2">Receita Total</h3>
              <p className="text-2xl font-bold text-green-600">R$ 56,789</p>
            </div>
            <div className="bg-background p-6 rounded-lg border">
              <h3 className="font-semibold mb-2">Produtos Ativos</h3>
              <p className="text-2xl font-bold text-blue-600">89</p>
            </div>
          </div>
          
          <div className="bg-background p-6 rounded-lg border">
            <h3 className="font-semibold mb-4">Conteúdo Principal</h3>
            <p className="text-muted-foreground">
              Esta é a área de conteúdo principal da aplicação. O sidebar pode ser 
              colapsado/expandido e é totalmente responsivo. Em dispositivos móveis, 
              ele se torna um overlay com backdrop blur.
            </p>
            <p className="text-muted-foreground mt-4">
              Item ativo: <strong>{activeItem}</strong>
            </p>
            <p className="text-muted-foreground">
              Sidebar colapsado: <strong>{collapsed ? 'Sim' : 'Não'}</strong>
            </p>
          </div>
        </div>
      </main>
    </div>
  )
}

// =============================================================================
// EXEMPLO COMBINADO - DASHBOARD COMPLETO
// =============================================================================

export const CompleteDashboardExample: React.FC = () => {
  return (
    <div className="space-y-8">
      <SidebarExample />
      <DataTableExample />
      <ModalExample />
    </div>
  )
}