#!/bin/bash

# Sistema de Gestão de Pedidos Distribuído - Quick Setup
echo "🚀 SISTEMA DE GESTÃO DE PEDIDOS - QUICK SETUP"

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker não encontrado. Instale Docker Desktop."
    exit 1
fi

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js não encontrado. Instale Node.js 18+."
    exit 1
fi

echo "✅ Pré-requisitos atendidos"

# Start infrastructure
echo "📡 Iniciando infraestrutura..."
docker-compose up -d order-db query-db rabbitmq redis

# Setup frontend
echo "🎨 Configurando frontend..."
cd frontend
npm install
npm run build
cd ..

# Create mock API
npm install express cors --prefix frontend --save-dev

cat > frontend/mock-server.js << 'EOF'
const express = require('express');
const cors = require('cors');
const app = express();

app.use(cors());
app.use(express.json());

const mockOrders = [
    { orderId: '1', customerId: 'cust-001', status: 'CONFIRMED', totalAmount: 299.99 },
    { orderId: '2', customerId: 'cust-002', status: 'PENDING', totalAmount: 150.50 }
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
app.get('/api/payments', (req, res) => res.json([]));
app.get('/api/inventory', (req, res) => res.json([]));
app.get('/api/dashboard/metrics', (req, res) => res.json(mockMetrics));

app.listen(8080, () => console.log('🎭 Mock API running on :8080'));
EOF

# Create start script
cat > start-dev.sh << 'EOF'
#!/bin/bash
echo "🚀 Starting Order Management System..."
cd frontend && node mock-server.js &
sleep 3
cd frontend && npm run dev &
echo "✅ System started! Frontend: http://localhost:3000"
wait
EOF

chmod +x start-dev.sh

echo ""
echo "🎉 SETUP COMPLETO!"
echo ""
echo "Para iniciar: ./start-dev.sh"
echo "Frontend: http://localhost:3000"
echo "API Mock: http://localhost:8080"