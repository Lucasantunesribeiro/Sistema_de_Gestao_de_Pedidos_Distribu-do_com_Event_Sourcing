export type OrderStatus =
  | 'PENDING'
  | 'INVENTORY_RESERVED'
  | 'PAYMENT_PROCESSING'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'FAILED';

export type PaymentMethod = 'PIX' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'BOLETO';

export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface Order {
  orderId: string;
  customerId: string;
  customerName: string;
  status: OrderStatus;
  totalAmount: number;
  paymentMethod: PaymentMethod;
  createdAt: string;
  updatedAt: string;
  correlationId: string;
  reservationId?: string;
  paymentId?: string;
  transactionId?: string;
  cancellationReason?: string;
  items: OrderItem[];
}

export interface CreateOrderItemRequest {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateOrderRequest {
  customerId: string;
  customerName: string;
  paymentMethod: PaymentMethod;
  items: CreateOrderItemRequest[];
}

export interface OrderStatistics {
  totalOrders: number;
  confirmedOrders: number;
  cancelledOrders: number;
  pendingOrders: number;
  totalRevenue: number;
}

export interface OrdersPage {
  content: Order[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface WebSocketOrderEvent {
  orderId: string;
  status: OrderStatus;
  customerName: string;
  timestamp: string;
  message?: string;
}
