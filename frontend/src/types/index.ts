// Order Management System Types

export interface OrderItem {
  productId: string
  productName: string
  quantity: number
  unitPrice: number
  totalPrice: number
}

export interface CreateOrderRequest {
  customerId: string
  items: OrderItemRequest[]
}

export interface OrderItemRequest {
  productId: string
  productName: string
  quantity: number
  price: number
}

export interface Order {
  orderId: string
  customerId: string
  status: OrderStatus
  items: OrderItem[]
  totalAmount: number
  paymentStatus?: PaymentStatus
  inventoryStatus?: InventoryStatus
  createdAt: string
  updatedAt: string
}

export interface Payment {
  paymentId: string
  orderId: string
  amount: number
  status: PaymentStatus
  paymentMethod: string
  gatewayTransactionId?: string
  failureReason?: string
  errorCode?: string
  createdAt: string
  processedAt?: string
  updatedAt: string
  retryCount: number
  correlationId: string
}

export interface InventoryItem {
  productId: string
  productName: string
  availableQuantity: number
  reservedQuantity: number
  totalQuantity: number
  lastUpdated: string
}

// Enums
export enum OrderStatus {
  PENDING = 'PENDING',
  INVENTORY_RESERVED = 'INVENTORY_RESERVED',
  INVENTORY_RESERVATION_FAILED = 'INVENTORY_RESERVATION_FAILED',
  PAYMENT_PROCESSING = 'PAYMENT_PROCESSING',
  PAYMENT_APPROVED = 'PAYMENT_APPROVED',
  PAYMENT_FAILED = 'PAYMENT_FAILED',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  APPROVED = 'APPROVED',
  DECLINED = 'DECLINED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
  REFUNDED = 'REFUNDED'
}

export enum InventoryStatus {
  AVAILABLE = 'AVAILABLE',
  RESERVED = 'RESERVED',
  CONFIRMED = 'CONFIRMED',
  RELEASED = 'RELEASED',
  OUT_OF_STOCK = 'OUT_OF_STOCK',
  LOW_STOCK = 'LOW_STOCK'
}

// Dashboard Types

export interface ChartDataPoint {
  date: string
  value: number
  label?: string
}

export interface RevenueDataPoint {
  date: string
  revenue: number
  orders: number
}

export interface StatusDistribution {
  status: string
  count: number
  percentage: number
  color: string
}

// API Response Types
export interface ApiResponse<T> {
  data: T
  message?: string
  success: boolean
}

export interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  code: string
  path: string
  correlationId: string
  validationErrors?: Record<string, string[]>
}

// WebSocket Types
export interface WebSocketMessage {
  type: string
  data: any
  timestamp: string
  correlationId?: string
}

export interface OrderEvent {
  eventType: string
  orderId: string
  timestamp: string
  data: any
}

// UI State Types
export interface AppState {
  theme: 'light' | 'dark'
  sidebarOpen: boolean
  user: User | null
  isAuthenticated: boolean
  notifications: Notification[]
  connectionStatus: 'connected' | 'disconnected' | 'reconnecting'
}

export interface User {
  id: string
  name: string
  email: string
  role: string
}

export interface Notification {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  message: string
  timestamp: string
  read: boolean
  action?: {
    label: string
    onClick: () => void
  }
}

// Form Types
export interface OrderFilters {
  status?: OrderStatus[]
  customerId?: string
  dateFrom?: string
  dateTo?: string
  minAmount?: number
  maxAmount?: number
}

export interface PaymentFilters {
  status?: PaymentStatus[]
  orderId?: string
  dateFrom?: string
  dateTo?: string
  minAmount?: number
  maxAmount?: number
}

// Table Types
export interface TableColumn<T> {
  key: keyof T
  label: string
  sortable?: boolean
  render?: (value: any, item: T) => React.ReactNode
  width?: string
}

export interface TableProps<T> {
  data: T[]
  columns: TableColumn<T>[]
  loading?: boolean
  pagination?: {
    page: number
    pageSize: number
    total: number
    onPageChange: (page: number) => void
    onPageSizeChange: (pageSize: number) => void
  }
  sorting?: {
    column: keyof T
    direction: 'asc' | 'desc'
    onSort: (column: keyof T, direction: 'asc' | 'desc') => void
  }
  filters?: React.ReactNode
  actions?: React.ReactNode
}