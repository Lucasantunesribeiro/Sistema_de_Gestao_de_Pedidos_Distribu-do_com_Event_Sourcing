const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8084';

class ApiService {
  constructor() {
    this.correlationId = this.generateCorrelationId();
  }

  generateCorrelationId() {
    return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
  }

  async makeRequest(url, options = {}) {
    const correlationId = this.generateCorrelationId();
    console.log(`[${correlationId}] Making request to: ${url}`, options);

    try {
      const response = await fetch(url, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          'X-Correlation-ID': correlationId,
          ...options.headers,
        },
      });

      console.log(`[${correlationId}] Response status: ${response.status}`);

      if (!response.ok) {
        const errorText = await response.text();
        console.error(`[${correlationId}] HTTP error! status: ${response.status}, body: ${errorText}`);
        throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
      }

      const data = await response.json();
      console.log(`[${correlationId}] Response data:`, data);
      return data;
    } catch (error) {
      console.error(`[${correlationId}] Request failed:`, error);
      throw error;
    }
  }

  async getAllOrders() {
    try {
      console.log('ApiService.getAllOrders() - Starting to fetch all orders');
      const data = await this.makeRequest(`${API_BASE_URL}/api/orders`);
      console.log(`ApiService.getAllOrders() - Successfully retrieved ${data.length} orders`);
      return data;
    } catch (error) {
      console.error('ApiService.getAllOrders() - Error fetching orders:', error);
      throw new Error(`Failed to fetch orders: ${error.message}`);
    }
  }

  async createOrder(orderData) {
    try {
      console.log('ApiService.createOrder() - Creating order:', orderData);

      // Validate order data
      if (!orderData.customerId || !orderData.productId || !orderData.quantity || !orderData.price) {
        throw new Error('Missing required order fields');
      }

      if (orderData.quantity <= 0) {
        throw new Error('Quantity must be greater than 0');
      }

      if (orderData.price <= 0) {
        throw new Error('Price must be greater than 0');
      }

      const data = await this.makeRequest(`${API_BASE_URL}/api/orders`, {
        method: 'POST',
        body: JSON.stringify(orderData),
      });

      console.log('ApiService.createOrder() - Order created successfully:', data);
      return data;
    } catch (error) {
      console.error('ApiService.createOrder() - Error creating order:', error);
      throw new Error(`Failed to create order: ${error.message}`);
    }
  }

  async getOrderById(id) {
    try {
      console.log(`ApiService.getOrderById(${id}) - Starting to fetch order`);

      if (!id) {
        throw new Error('Order ID is required');
      }

      const data = await this.makeRequest(`${API_BASE_URL}/api/orders/${id}`);
      console.log(`ApiService.getOrderById(${id}) - Order retrieved successfully`);
      return data;
    } catch (error) {
      console.error(`ApiService.getOrderById(${id}) - Error fetching order:`, error);
      throw new Error(`Failed to fetch order: ${error.message}`);
    }
  }

  async getOrdersByCustomerId(customerId) {
    try {
      console.log(`ApiService.getOrdersByCustomerId(${customerId}) - Starting to fetch orders`);

      if (!customerId) {
        throw new Error('Customer ID is required');
      }

      const data = await this.makeRequest(`${API_BASE_URL}/api/orders/customer/${customerId}`);
      console.log(`ApiService.getOrdersByCustomerId(${customerId}) - Successfully retrieved ${data.length} orders`);
      return data;
    } catch (error) {
      console.error(`ApiService.getOrdersByCustomerId(${customerId}) - Error fetching orders:`, error);
      throw new Error(`Failed to fetch orders for customer: ${error.message}`);
    }
  }

  // Health check method
  async healthCheck() {
    try {
      console.log('ApiService.healthCheck() - Checking API health');
      const response = await fetch(`${API_BASE_URL}/actuator/health`);
      const data = await response.json();
      console.log('ApiService.healthCheck() - Health check result:', data);
      return data;
    } catch (error) {
      console.error('ApiService.healthCheck() - Health check failed:', error);
      return { status: 'DOWN', error: error.message };
    }
  }
}

export default new ApiService();