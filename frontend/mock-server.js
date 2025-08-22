import cors from 'cors';
import express from 'express';

const app = express();

app.use(cors());
app.use(express.json());

const mockOrders = [
    {
        orderId: '1',
        customerId: 'cust-001',
        status: 'CONFIRMED',
        totalAmount: 299.99,
        items: [
            { productId: 'prod-1', productName: 'Produto A', quantity: 2, unitPrice: 149.99, totalPrice: 299.98 }
        ],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
    },
    {
        orderId: '2',
        customerId: 'cust-002',
        status: 'PENDING',
        totalAmount: 150.50,
        items: [
            { productId: 'prod-2', productName: 'Produto B', quantity: 1, unitPrice: 150.50, totalPrice: 150.50 }
        ],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
    }
];

const mockPayments = [
    {
        paymentId: 'pay-1',
        orderId: '1',
        amount: 299.99,
        status: 'APPROVED',
        paymentMethod: 'CREDIT_CARD',
        createdAt: new Date().toISOString(),
        processedAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        retryCount: 0,
        correlationId: 'corr-1'
    },
    {
        paymentId: 'pay-2',
        orderId: '2',
        amount: 150.50,
        status: 'PENDING',
        paymentMethod: 'PIX',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        retryCount: 0,
        correlationId: 'corr-2'
    }
];

const mockInventory = [
    {
        productId: 'prod-1',
        productName: 'Produto A',
        availableQuantity: 50,
        reservedQuantity: 5,
        totalQuantity: 55,
        lastUpdated: new Date().toISOString()
    },
    {
        productId: 'prod-2',
        productName: 'Produto B',
        availableQuantity: 25,
        reservedQuantity: 2,
        totalQuantity: 27,
        lastUpdated: new Date().toISOString()
    }
];

const mockMetrics = {
    totalOrders: 156,
    totalRevenue: 45678.90,
    pendingOrders: 23
};

app.get('/api/orders', (req, res) => res.json(mockOrders));
app.post('/api/orders', (req, res) => {
    const newOrder = { orderId: Date.now().toString(), ...req.body, status: 'PENDING' };
    mockOrders.push(newOrder);
    res.json(newOrder);
});
app.get('/api/payments', (req, res) => res.json(mockPayments));
app.get('/api/inventory', (req, res) => res.json(mockInventory));
app.get('/api/dashboard/metrics', (req, res) => res.json(mockMetrics));

app.listen(8080, () => console.log('Mock API running on :8080'));
