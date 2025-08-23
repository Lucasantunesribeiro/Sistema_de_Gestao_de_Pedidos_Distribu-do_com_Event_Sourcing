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
    // Using your deployed backend URL
    const baseURL = import.meta.env.VITE_API_URL || 'https://gestao-de-pedidos.onrender.com'
    
    // Force correct URL in production
    const finalURL = baseURL.includes('order-service') 
      ? 'https://gestao-de-pedidos.onrender.com' 
      : baseURL
    
    this.client = axios.create({
      baseURL: finalURL,
      timeout: 30000, // Increased timeout for Render cold starts
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
        const token = localStorage.getItem('auth_token') || 'demo-token'
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        
        // Temporary: Add basic auth header for demo
        config.headers['X-Demo-Auth'] = 'frontend-vercel'

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
    console.error('API Error Details:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      url: error.response?.config?.url,
      method: error.response?.config?.method,
    })

    if (error.response?.data) {
      return error.response.data
    }

    return {
      status: error.response?.status || 0,
      code: error.response?.status === 500 ? 'SERVER_ERROR' : 'NETWORK_ERROR',
      message: error.response?.statusText || error.message || 'Network error',
    }
  }

  // System Info & Health
  async getSystemInfo(): Promise<any> {
    return await this.request<any>({
      url: '/',
      method: 'GET',
    })
  }

  async getHealthCheck(): Promise<any> {
    return await this.request<any>({
      url: '/health',
      method: 'GET',
    })
  }

  async getSystemStats(): Promise<any> {
    return this.request<any>({
      url: '/api/system',
      method: 'GET',
    })
  }

  // Orders API
  async getOrders(filters?: OrderFilters): Promise<any> {
    return this.request<any>({
      url: '/api/orders',
      method: 'GET',
      params: filters,
    })
  }

  async getOrder(orderId: string): Promise<any> {
    const response = await this.request<any>({
      url: `/api/orders/${orderId}`,
      method: 'GET',
    })
    return response.order || response
  }

  async createOrder(orderData: { customerId: string; totalAmount: number; productIds?: string[] }): Promise<any> {
    return this.request<any>({
      url: '/api/orders',
      method: 'POST',
      data: orderData,
    })
  }

  async updateOrderStatus(orderId: string, status: string): Promise<any> {
    return this.request<any>({
      url: `/api/orders/${orderId}/status`,
      method: 'PUT',
      data: { status },
    })
  }

  async getOrdersByCustomer(customerId: string): Promise<any> {
    const response = await this.request<any>({
      url: `/api/orders/customer/${customerId}`,
      method: 'GET',
    })
    return response.orders || response
  }

  async getOrderEvents(orderId: string): Promise<any> {
    const response = await this.request<any>({
      url: `/api/orders/${orderId}/events`,
      method: 'GET',
    })
    return response.events || response
  }

  // Payments API
  async getPayments(filters?: PaymentFilters): Promise<Payment[]> {
    return this.request<Payment[]>({
      url: '/api/payments',
      method: 'GET',
      params: filters,
    })
  }

  async getPayment(paymentId: string): Promise<Payment> {
    return this.request<Payment>({
      url: `/api/payments/${paymentId}`,
      method: 'GET',
    })
  }

  async getPaymentByOrderId(orderId: string): Promise<Payment> {
    return this.request<Payment>({
      url: `/api/payments/order/${orderId}`,
      method: 'GET',
    })
  }

  async retryPayment(paymentId: string): Promise<Payment> {
    return this.request<Payment>({
      url: `/api/payments/${paymentId}/retry`,
      method: 'POST',
    })
  }

  // Inventory API
  async getInventory(): Promise<InventoryItem[]> {
    return this.request<InventoryItem[]>({
      url: '/api/inventory',
      method: 'GET',
    })
  }

  // Dashboard API
  async getDashboardMetrics(): Promise<DashboardMetrics> {
    return this.request<DashboardMetrics>({
      url: '/api/dashboard/metrics',
      method: 'GET',
    })
  }
}

export const apiClient = new ApiClient()
export default apiClient