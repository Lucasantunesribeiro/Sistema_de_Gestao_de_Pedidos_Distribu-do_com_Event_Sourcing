import type {
  CreateOrderRequest,
  DashboardMetrics,
  InventoryItem,
  Order,
  OrderFilters,
  Payment,
  PaymentFilters
} from '@/types'
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { generateCorrelationId } from './utils'

class ApiClient {
  private client: AxiosInstance

  constructor() {
    this.client = axios.create({
      baseURL: '/api',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    this.setupInterceptors()
  }

  private setupInterceptors() {
    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        // Add correlation ID to all requests
        const correlationId = generateCorrelationId()
        config.headers['X-Correlation-ID'] = correlationId

        // Add auth token if available
        const token = localStorage.getItem('auth_token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }

        console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`, {
          correlationId,
          data: config.data,
        })

        return config
      },
      (error) => {
        console.error('API Request Error:', error)
        return Promise.reject(error)
      }
    )

    // Response interceptor
    this.client.interceptors.response.use(
      (response: AxiosResponse) => {
        const correlationId = response.headers['x-correlation-id']
        console.log(`API Response: ${response.status} ${response.config.url}`, {
          correlationId,
          data: response.data,
        })
        return response
      },
      (error) => {
        const correlationId = error.response?.headers['x-correlation-id']
        console.error('API Response Error:', {
          status: error.response?.status,
          url: error.response?.config?.url,
          correlationId,
          data: error.response?.data,
        })

        // Handle specific error cases
        if (error.response?.status === 401) {
          // Unauthorized - redirect to login
          localStorage.removeItem('auth_token')
          window.location.href = '/login'
        }

        return Promise.reject(error)
      }
    )
  }

  // Generic request method
  private async request<T>(config: AxiosRequestConfig): Promise<T> {
    try {
      const response = await this.client.request<T>(config)
      return response.data
    } catch (error: any) {
      this.handleError(error)
      throw error
    }
  }

  private handleError(error: any) {
    if (error.response?.data) {
      return error.response.data
    }

    return {
      status: 0,
      code: 'NETWORK_ERROR',
      message: error.message || 'Network error',
    }
  }

  // Orders API
  async getOrders(filters?: OrderFilters): Promise<Order[]> {
    const params = new URLSearchParams()

    if (filters?.status?.length) {
      filters.status.forEach(status => params.append('status', status))
    }
    if (filters?.customerId) {
      params.append('customerId', filters.customerId)
    }
    if (filters?.dateFrom) {
      params.append('dateFrom', filters.dateFrom)
    }
    if (filters?.dateTo) {
      params.append('dateTo', filters.dateTo)
    }
    if (filters?.minAmount) {
      params.append('minAmount', filters.minAmount.toString())
    }
    if (filters?.maxAmount) {
      params.append('maxAmount', filters.maxAmount.toString())
    }

    return this.request<Order[]>({
      url: '/orders',
      method: 'GET',
      params: Object.fromEntries(params),
    })
  }

  async getOrder(orderId: string): Promise<Order> {
    return this.request<Order>({
      url: `/orders/${orderId}`,
      method: 'GET',
    })
  }

  async createOrder(order: CreateOrderRequest): Promise<Order> {
    return this.request<Order>({
      url: '/orders',
      method: 'POST',
      data: order,
    })
  }

  async cancelOrder(orderId: string, reason?: string): Promise<void> {
    const params = reason ? { reason } : {}
    return this.request<void>({
      url: `/orders/${orderId}`,
      method: 'DELETE',
      params,
    })
  }

  async reserveInventory(orderId: string): Promise<void> {
    return this.request<void>({
      url: `/orders/${orderId}/reserve-inventory`,
      method: 'POST',
    })
  }

  // Payments API
  async getPayments(filters?: PaymentFilters): Promise<Payment[]> {
    const params = new URLSearchParams()

    if (filters?.status?.length) {
      filters.status.forEach(status => params.append('status', status))
    }
    if (filters?.orderId) {
      params.append('orderId', filters.orderId)
    }
    if (filters?.dateFrom) {
      params.append('dateFrom', filters.dateFrom)
    }
    if (filters?.dateTo) {
      params.append('dateTo', filters.dateTo)
    }
    if (filters?.minAmount) {
      params.append('minAmount', filters.minAmount.toString())
    }
    if (filters?.maxAmount) {
      params.append('maxAmount', filters.maxAmount.toString())
    }

    return this.request<Payment[]>({
      url: '/payments',
      method: 'GET',
      params: Object.fromEntries(params),
    })
  }

  async getPayment(paymentId: string): Promise<Payment> {
    return this.request<Payment>({
      url: `/payments/${paymentId}`,
      method: 'GET',
    })
  }

  async getPaymentByOrderId(orderId: string): Promise<Payment> {
    return this.request<Payment>({
      url: `/payments/order/${orderId}`,
      method: 'GET',
    })
  }

  async retryPayment(paymentId: string): Promise<Payment> {
    return this.request<Payment>({
      url: `/payments/${paymentId}/retry`,
      method: 'POST',
    })
  }

  // Inventory API
  async getInventory(): Promise<InventoryItem[]> {
    return this.request<InventoryItem[]>({
      url: '/inventory',
      method: 'GET',
    })
  }

  // Dashboard API
  async getDashboardMetrics(): Promise<DashboardMetrics> {
    return this.request<DashboardMetrics>({
      url: '/dashboard/metrics',
      method: 'GET',
    })
  }
}

export const apiClient = new ApiClient()
export default apiClient