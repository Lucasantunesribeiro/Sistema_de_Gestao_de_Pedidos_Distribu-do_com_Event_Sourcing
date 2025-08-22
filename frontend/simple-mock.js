import http from 'http';
import url from 'url';

const mockOrders = [
    {
        orderId: '1',
        customerId: 'cust-001',
        status: 'COMPLETED',
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
    }
];

const server = http.createServer((req, res) => {
    const parsedUrl = url.parse(req.url, true);
    
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    
    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }
    
    res.setHeader('Content-Type', 'application/json');
    
    switch (parsedUrl.pathname) {
        case '/api/orders':
            if (req.method === 'POST') {
                let body = '';
                req.on('data', chunk => {
                    body += chunk.toString();
                });
                req.on('end', () => {
                    try {
                        const orderData = JSON.parse(body);
                        const newOrder = {
                            orderId: Date.now().toString(),
                            ...orderData,
                            status: 'PENDING',
                            totalAmount: orderData.items?.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0) || 0,
                            createdAt: new Date().toISOString(),
                            updatedAt: new Date().toISOString()
                        };
                        mockOrders.push(newOrder);
                        res.writeHead(201);
                        res.end(JSON.stringify(newOrder));
                    } catch (error) {
                        res.writeHead(400);
                        res.end(JSON.stringify({ error: 'Invalid JSON' }));
                    }
                });
            } else {
                res.writeHead(200);
                res.end(JSON.stringify(mockOrders));
            }
            break;
        case '/api/payments':
            res.writeHead(200);
            res.end(JSON.stringify(mockPayments));
            break;
        case '/api/inventory':
            res.writeHead(200);
            res.end(JSON.stringify(mockInventory));
            break;
        case '/api/dashboard/metrics':
            res.writeHead(200);
            res.end(JSON.stringify({
                totalOrders: 156,
                totalRevenue: 45678.90,
                pendingOrders: 23
            }));
            break;
        default:
            res.writeHead(404);
            res.end(JSON.stringify({ error: 'Not found' }));
    }
});

server.listen(8080, () => {
    console.log('Simple mock API running on port 8080');
});