export interface InventoryItem {
  productId: string;
  productName: string;
  availableQuantity: number;
  reservedQuantity: number;
  totalQuantity: number;
}

export interface InventoryStatus {
  totalProducts: number;
  totalAvailable: number;
  totalReserved: number;
  lowStockItems: number;
  items: InventoryItem[];
}

export interface WebSocketInventoryEvent {
  productId: string;
  productName: string;
  availableQuantity: number;
  reservedQuantity: number;
  timestamp: string;
}
