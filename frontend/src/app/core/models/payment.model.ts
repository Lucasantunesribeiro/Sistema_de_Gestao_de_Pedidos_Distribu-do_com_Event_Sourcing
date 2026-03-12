export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'APPROVED' | 'REJECTED' | 'REFUNDED';

export interface Payment {
  paymentId: string;
  orderId: string;
  amount: number;
  method: string;
  status: PaymentStatus;
  transactionId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WebSocketPaymentEvent {
  paymentId: string;
  orderId: string;
  status: PaymentStatus;
  amount: number;
  timestamp: string;
}
