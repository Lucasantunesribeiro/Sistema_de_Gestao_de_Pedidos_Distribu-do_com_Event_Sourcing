# Design Document

## Overview

O sistema atual está funcionando perfeitamente com Order Service e Query Service operacionais. O design foca em duas áreas principais: (1) completar a implementação dos microsserviços Payment e Inventory para funcionalidade completa, e (2) modernizar completamente o frontend com design contemporâneo, componentes interativos e experiência de usuário premium.

## Architecture

### Current State Analysis
- ✅ **Order Service**: Totalmente funcional com persistência PostgreSQL
- ✅ **Query Service**: Retornando dados corretos
- ✅ **Health Check**: Sistema operacional
- ⚠️ **Payment Service**: Implementação básica, precisa ser completada
- ⚠️ **Inventory Service**: Implementação básica, precisa ser completada
- ⚠️ **Frontend**: Funcional mas design básico, precisa modernização

### Target Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Modern Frontend                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │  Dashboard  │ │   Orders    │ │  Analytics  │          │
│  │   Modern    │ │   Modern    │ │   Modern    │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────────────┐
                    │   API Gateway   │
                    │   (Nginx)       │
                    └─────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ Order Service │    │Payment Service│    │Inventory Svc  │
│   ✅ Ready   │    │ 🔧 Complete   │    │ 🔧 Complete   │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
                    ┌─────────────────┐
                    │   PostgreSQL    │
                    │    Database     │
                    └─────────────────┘
```

## Components and Interfaces

### 1. Payment Service Enhancement

**Current Implementation:**
```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    // Basic structure exists but needs full implementation
}
```

**Enhanced Implementation:**
```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        // Real payment processing logic
        // Integration with payment providers (mock for demo)
        // Transaction management
        // Error handling and rollback
    }
    
    @GetMapping("/status/{paymentId}")
    public ResponseEntity<PaymentStatus> getPaymentStatus(@PathVariable String paymentId) {
        // Payment status tracking
    }
    
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> processRefund(@RequestBody RefundRequest request) {
        // Refund processing
    }
}
```

### 2. Inventory Service Enhancement

**Current Implementation:**
```java
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    // Basic structure exists but needs full implementation
}
```

**Enhanced Implementation:**
```java
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserveItems(@RequestBody ReservationRequest request) {
        // Stock reservation logic
        // Atomic operations for multiple items
        // Timeout handling for reservations
    }
    
    @PostMapping("/release")
    public ResponseEntity<ReleaseResponse> releaseReservation(@RequestBody ReleaseRequest request) {
        // Release reserved stock
    }
    
    @GetMapping("/stock/{productId}")
    public ResponseEntity<StockInfo> getStockInfo(@PathVariable String productId) {
        // Real-time stock information
    }
    
    @PutMapping("/stock/{productId}")
    public ResponseEntity<StockUpdateResponse> updateStock(@PathVariable String productId, @RequestBody StockUpdateRequest request) {
        // Stock level management
    }
}
```

### 3. Modern Frontend Architecture

**Technology Stack:**
- **Base**: HTML5, CSS3, JavaScript ES6+
- **Styling**: Modern CSS with CSS Grid, Flexbox, Custom Properties
- **Components**: Modular component architecture
- **State Management**: Local storage + real-time updates
- **Animations**: CSS transitions and keyframes
- **Icons**: Modern icon library (Feather Icons or similar)

**Component Structure:**
```
frontend/
├── components/
│   ├── dashboard/
│   │   ├── MetricsCard.js
│   │   ├── ServiceStatus.js
│   │   └── RealtimeChart.js
│   ├── orders/
│   │   ├── OrderList.js
│   │   ├── OrderForm.js
│   │   └── OrderDetails.js
│   ├── common/
│   │   ├── Button.js
│   │   ├── Modal.js
│   │   ├── LoadingSpinner.js
│   │   └── Toast.js
│   └── layout/
│       ├── Header.js
│       ├── Sidebar.js
│       └── Footer.js
├── styles/
│   ├── variables.css
│   ├── components.css
│   ├── animations.css
│   └── responsive.css
└── utils/
    ├── api.js
    ├── websocket.js
    └── helpers.js
```

## Data Models

### Payment Service Models

```java
// Payment Request
public class PaymentRequest {
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private CustomerInfo customer;
    private String correlationId;
}

// Payment Response
public class PaymentResponse {
    private String paymentId;
    private PaymentStatus status;
    private String transactionId;
    private String message;
    private LocalDateTime processedAt;
}

// Payment Status Enum
public enum PaymentStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED
}
```

### Inventory Service Models

```java
// Reservation Request
public class ReservationRequest {
    private String orderId;
    private List<ItemReservation> items;
    private Duration reservationTimeout;
    private String correlationId;
}

// Item Reservation
public class ItemReservation {
    private String productId;
    private Integer quantity;
    private String warehouseId;
}

// Stock Info
public class StockInfo {
    private String productId;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;
    private LocalDateTime lastUpdated;
}
```

## User Interface Design

### 1. Modern Color Palette

```css
:root {
  /* Primary Colors */
  --primary-50: #f0f9ff;
  --primary-500: #3b82f6;
  --primary-600: #2563eb;
  --primary-700: #1d4ed8;
  
  /* Success Colors */
  --success-50: #f0fdf4;
  --success-500: #22c55e;
  --success-600: #16a34a;
  
  /* Warning Colors */
  --warning-50: #fffbeb;
  --warning-500: #f59e0b;
  --warning-600: #d97706;
  
  /* Error Colors */
  --error-50: #fef2f2;
  --error-500: #ef4444;
  --error-600: #dc2626;
  
  /* Neutral Colors */
  --gray-50: #f9fafb;
  --gray-100: #f3f4f6;
  --gray-200: #e5e7eb;
  --gray-300: #d1d5db;
  --gray-400: #9ca3af;
  --gray-500: #6b7280;
  --gray-600: #4b5563;
  --gray-700: #374151;
  --gray-800: #1f2937;
  --gray-900: #111827;
}
```

### 2. Modern Typography

```css
/* Typography Scale */
:root {
  --font-family-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-family-mono: 'JetBrains Mono', 'Fira Code', monospace;
  
  /* Font Sizes */
  --text-xs: 0.75rem;    /* 12px */
  --text-sm: 0.875rem;   /* 14px */
  --text-base: 1rem;     /* 16px */
  --text-lg: 1.125rem;   /* 18px */
  --text-xl: 1.25rem;    /* 20px */
  --text-2xl: 1.5rem;    /* 24px */
  --text-3xl: 1.875rem;  /* 30px */
  --text-4xl: 2.25rem;   /* 36px */
  
  /* Font Weights */
  --font-normal: 400;
  --font-medium: 500;
  --font-semibold: 600;
  --font-bold: 700;
}
```

### 3. Modern Component Styles

**Modern Button:**
```css
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-weight: var(--font-medium);
  font-size: var(--text-sm);
  transition: all 0.2s ease-in-out;
  border: none;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-500), var(--primary-600));
  color: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.06);
}

.btn-primary:hover {
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1), 0 2px 4px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}
```

**Modern Card:**
```css
.card {
  background: white;
  border-radius: 0.75rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--gray-200);
  transition: all 0.2s ease-in-out;
  overflow: hidden;
}

.card:hover {
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1), 0 4px 6px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.card-header {
  padding: 1.5rem;
  border-bottom: 1px solid var(--gray-200);
  background: var(--gray-50);
}

.card-body {
  padding: 1.5rem;
}
```

### 4. Dashboard Layout

**Modern Grid Layout:**
```css
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
  padding: 1.5rem;
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.chart-container {
  background: white;
  border-radius: 0.75rem;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid var(--gray-200);
}
```

## Real-time Features

### 1. WebSocket Integration

```javascript
class RealtimeManager {
  constructor() {
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 1000;
  }
  
  connect() {
    this.ws = new WebSocket('ws://localhost:8080/ws');
    
    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      this.updateConnectionStatus(true);
    };
    
    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.handleRealtimeUpdate(data);
    };
    
    this.ws.onclose = () => {
      this.updateConnectionStatus(false);
      this.attemptReconnect();
    };
  }
  
  handleRealtimeUpdate(data) {
    switch(data.type) {
      case 'ORDER_CREATED':
        this.updateOrdersList(data.payload);
        this.updateMetrics();
        break;
      case 'SERVICE_STATUS':
        this.updateServiceStatus(data.payload);
        break;
      case 'METRICS_UPDATE':
        this.updateDashboardMetrics(data.payload);
        break;
    }
  }
}
```

### 2. Auto-refresh Mechanism

```javascript
class AutoRefresh {
  constructor() {
    this.intervals = new Map();
    this.isVisible = true;
    
    // Pause updates when tab is not visible
    document.addEventListener('visibilitychange', () => {
      this.isVisible = !document.hidden;
      if (this.isVisible) {
        this.resumeAll();
      } else {
        this.pauseAll();
      }
    });
  }
  
  register(key, callback, interval = 30000) {
    if (this.intervals.has(key)) {
      clearInterval(this.intervals.get(key));
    }
    
    const intervalId = setInterval(() => {
      if (this.isVisible) {
        callback();
      }
    }, interval);
    
    this.intervals.set(key, intervalId);
  }
}
```

## Performance Optimization

### 1. Lazy Loading

```javascript
class LazyLoader {
  constructor() {
    this.observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            this.loadComponent(entry.target);
          }
        });
      },
      { threshold: 0.1 }
    );
  }
  
  observe(element) {
    this.observer.observe(element);
  }
  
  async loadComponent(element) {
    const componentName = element.dataset.component;
    try {
      const module = await import(`./components/${componentName}.js`);
      const component = new module.default();
      component.render(element);
      this.observer.unobserve(element);
    } catch (error) {
      console.error(`Failed to load component: ${componentName}`, error);
    }
  }
}
```

### 2. Caching Strategy

```javascript
class CacheManager {
  constructor() {
    this.cache = new Map();
    this.ttl = new Map();
    this.defaultTTL = 5 * 60 * 1000; // 5 minutes
  }
  
  set(key, value, ttl = this.defaultTTL) {
    this.cache.set(key, value);
    this.ttl.set(key, Date.now() + ttl);
  }
  
  get(key) {
    if (!this.cache.has(key)) return null;
    
    if (Date.now() > this.ttl.get(key)) {
      this.cache.delete(key);
      this.ttl.delete(key);
      return null;
    }
    
    return this.cache.get(key);
  }
  
  async getOrFetch(key, fetchFn, ttl) {
    let data = this.get(key);
    if (data === null) {
      data = await fetchFn();
      this.set(key, data, ttl);
    }
    return data;
  }
}
```

## Testing Strategy

### 1. Backend Service Tests

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PaymentServiceIntegrationTest {
    
    @Autowired
    private PaymentService paymentService;
    
    @Test
    void shouldProcessPaymentSuccessfully() {
        // Test payment processing
    }
    
    @Test
    void shouldHandlePaymentFailure() {
        // Test failure scenarios
    }
    
    @Test
    void shouldProcessRefund() {
        // Test refund functionality
    }
}
```

### 2. Frontend Component Tests

```javascript
// Modern testing with Jest and Testing Library
describe('OrderForm Component', () => {
  test('should submit order with valid data', async () => {
    render(<OrderForm />);
    
    await userEvent.type(screen.getByLabelText(/customer name/i), 'João Silva');
    await userEvent.type(screen.getByLabelText(/product/i), 'Notebook');
    await userEvent.click(screen.getByRole('button', { name: /create order/i }));
    
    expect(await screen.findByText(/order created successfully/i)).toBeInTheDocument();
  });
  
  test('should show validation errors for invalid data', async () => {
    render(<OrderForm />);
    
    await userEvent.click(screen.getByRole('button', { name: /create order/i }));
    
    expect(screen.getByText(/customer name is required/i)).toBeInTheDocument();
  });
});
```

## Implementation Phases

### Phase 1: Complete Backend Services (Week 1)
1. Implement full Payment Service functionality
2. Implement full Inventory Service functionality
3. Integrate services with Order Service
4. Add comprehensive error handling
5. Write integration tests

### Phase 2: Modern Frontend Foundation (Week 2)
1. Implement modern CSS framework
2. Create reusable component library
3. Implement modern layout and navigation
4. Add responsive design
5. Implement loading states and animations

### Phase 3: Real-time Features (Week 3)
1. Implement WebSocket connections
2. Add real-time dashboard updates
3. Implement auto-refresh mechanisms
4. Add connection status indicators
5. Implement offline handling

### Phase 4: Advanced Features (Week 4)
1. Add advanced analytics and charts
2. Implement search and filtering
3. Add export functionality
4. Implement user preferences
5. Performance optimization and caching

## Success Metrics

### Backend Completion
- ✅ All 4 services (Order, Payment, Inventory, Query) fully operational
- ✅ End-to-end order flow working with all validations
- ✅ Comprehensive error handling and rollback mechanisms
- ✅ 95%+ test coverage for all services

### Frontend Modernization
- ✅ Modern, responsive design across all devices
- ✅ Real-time updates without page refresh
- ✅ Page load times under 2 seconds
- ✅ Smooth animations and transitions
- ✅ Accessibility compliance (WCAG 2.1 AA)

### User Experience
- ✅ Intuitive navigation and workflow
- ✅ Clear error messages and feedback
- ✅ Professional, polished appearance
- ✅ Mobile-first responsive design
- ✅ Fast, fluid interactions