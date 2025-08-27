#!/usr/bin/env node

// Simple build script for Docker - creates dist folder with static HTML
import fs from 'fs';
import path from 'path';

const distDir = 'dist';
const indexHtml = `<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sistema de Gestão de Pedidos</title>
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🛒</text></svg>">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f8fafc; }
        .container { max-width: 1400px; margin: 0 auto; padding: 20px; }
        .header { background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 20px; margin-bottom: 20px; }
        .card { background: white; border-radius: 8px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
        .status { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
        .status.up { background: #dcfce7; color: #166534; }
        .status.loading { background: #fef3c7; color: #d97706; }
        .btn { background: #3b82f6; color: white; border: none; padding: 10px 16px; border-radius: 6px; cursor: pointer; margin: 5px; }
        .btn:hover { background: #2563eb; }
        .btn.success { background: #059669; }
        .btn.danger { background: #dc2626; }
        .btn.secondary { background: #6b7280; }
        .result { margin-top: 10px; padding: 10px; border-radius: 4px; font-size: 14px; }
        .result.success { background: #dcfce7; color: #166534; }
        .result.error { background: #fef2f2; color: #dc2626; }
        .result.info { background: #dbeafe; color: #1d4ed8; }
        input, select { width: 100%; padding: 8px; margin: 5px 0; border: 1px solid #d1d5db; border-radius: 4px; }
        .orders-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        .orders-table th, .orders-table td { padding: 8px; text-align: left; border-bottom: 1px solid #e5e7eb; }
        .orders-table th { background: #f9fafb; font-weight: 600; }
        .footer { text-align: center; color: #64748b; margin-top: 40px; }
        .stats { display: flex; justify-content: space-between; margin: 10px 0; }
        .stat-item { text-align: center; }
        .stat-number { font-size: 24px; font-weight: bold; color: #3b82f6; }
        .loading { color: #6b7280; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🛒 Sistema de Gestão de Pedidos</h1>
            <p>Sistema distribuído com Event Sourcing - Deploy no Render</p>
        </div>
        
        <div class="grid">
            <!-- Status System -->
            <div class="card">
                <h2>Status do Sistema</h2>
                <div id="system-status">
                    <span class="status loading">🟡 CARREGANDO...</span>
                    <p style="margin-top: 10px;" id="system-message">Verificando microsserviços...</p>
                </div>
                <button class="btn" onclick="checkHealth()">🔧 Verificar Health</button>
                <div id="health-result" class="result"></div>
            </div>

            <!-- Create Order -->
            <div class="card">
                <h3>🛍️ Criar Pedido</h3>
                <input type="text" id="customer-name" placeholder="Nome do Cliente" value="João Silva">
                <input type="text" id="product-name" placeholder="Nome do Produto" value="Notebook Dell">
                <input type="number" id="product-price" placeholder="Preço" value="2500.00" step="0.01">
                <input type="number" id="product-quantity" placeholder="Quantidade" value="1">
                <button class="btn success" onclick="createOrder()">➕ Criar Pedido</button>
                <div id="order-result" class="result"></div>
            </div>

            <!-- Orders List -->
            <div class="card">
                <h3>📋 Lista de Pedidos</h3>
                <div class="stats">
                    <div class="stat-item">
                        <div id="total-orders" class="stat-number">0</div>
                        <div>Total</div>
                    </div>
                    <div class="stat-item">
                        <div id="pending-orders" class="stat-number">0</div>
                        <div>Pendentes</div>
                    </div>
                    <div class="stat-item">
                        <div id="completed-orders" class="stat-number">0</div>
                        <div>Concluídos</div>
                    </div>
                </div>
                <button class="btn" onclick="loadOrders()">🔄 Carregar Pedidos</button>
                <div id="orders-container"></div>
            </div>

            <!-- Microservices Test -->
            <div class="card">
                <h3>🧪 Teste de Microsserviços</h3>
                <button class="btn" onclick="testService('order')">📦 Order Service</button>
                <button class="btn" onclick="testService('payment')">💳 Payment Service</button>
                <button class="btn" onclick="testService('inventory')">📊 Inventory Service</button>
                <button class="btn" onclick="testService('query')">🔍 Query Service</button>
                <div id="services-result" class="result"></div>
            </div>
        </div>

        <div class="footer">
            <p>Sistema de Gestão de Pedidos v2.0.0 | Arquitetura Distribuída</p>
            <p>Microsserviços: Order, Payment, Inventory, Query Services</p>
            <p>🚀 Funcionando no Render com Event Sourcing</p>
        </div>
    </div>

    <script>
        let orders = [];
        let systemReady = false;

        // Auto-initialize on load
        document.addEventListener('DOMContentLoaded', function() {
            setTimeout(initializeSystem, 500);
        });

        async function initializeSystem() {
            await checkHealth();
            await loadOrders();
            // Auto-refresh orders every 30 seconds
            setInterval(loadOrders, 30000);
        }

        async function checkHealth() {
            const resultDiv = document.getElementById('health-result');
            const statusDiv = document.getElementById('system-status');
            
            resultDiv.innerHTML = '<div class="loading">🔄 Verificando saúde do sistema...</div>';
            
            try {
                const response = await fetch('/health');
                const data = await response.json();
                
                if (response.ok) {
                    systemReady = data.service === 'query-service' || data.status === 'UP';
                    
                    statusDiv.innerHTML = \`
                        <span class="status up">🟢 OPERACIONAL</span>
                        <p style="margin-top: 10px;">Sistema funcionando corretamente (\${data.service || 'nginx'})</p>
                    \`;
                    
                    resultDiv.innerHTML = \`
                        <div class="result success">
                            <strong>✅ Health Check OK!</strong><br>
                            <small>\${JSON.stringify(data, null, 2)}</small>
                        </div>
                    \`;
                } else {
                    throw new Error(\`HTTP \${response.status}\`);
                }
            } catch (error) {
                statusDiv.innerHTML = \`
                    <span class="status loading">🟡 INICIANDO</span>
                    <p style="margin-top: 10px;">Serviços ainda carregando...</p>
                \`;
                
                resultDiv.innerHTML = \`
                    <div class="result error">
                        <strong>⚠️ Sistema Iniciando:</strong><br>
                        <small>\${error.message}</small>
                    </div>
                \`;
            }
        }

        async function createOrder() {
            const resultDiv = document.getElementById('order-result');
            const customerName = document.getElementById('customer-name').value;
            const productName = document.getElementById('product-name').value;
            const productPrice = parseFloat(document.getElementById('product-price').value);
            const productQuantity = parseInt(document.getElementById('product-quantity').value);

            if (!customerName || !productName || !productPrice || !productQuantity) {
                resultDiv.innerHTML = '<div class="result error">❌ Preencha todos os campos!</div>';
                return;
            }

            resultDiv.innerHTML = '<div class="loading">🔄 Criando pedido...</div>';

            try {
                const orderData = {
                    customerName,
                    items: [{
                        productName,
                        price: productPrice,
                        quantity: productQuantity
                    }],
                    totalAmount: productPrice * productQuantity
                };

                const response = await fetch('/api/orders', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(orderData)
                });

                if (response.ok) {
                    const result = await response.json();
                    resultDiv.innerHTML = \`
                        <div class="result success">
                            <strong>✅ Pedido criado com sucesso!</strong><br>
                            ID: \${result.orderId || result.id || 'Novo pedido'}
                        </div>
                    \`;
                    
                    // Clear form
                    document.getElementById('customer-name').value = '';
                    document.getElementById('product-name').value = '';
                    document.getElementById('product-price').value = '';
                    document.getElementById('product-quantity').value = '';
                    
                    // Reload orders
                    setTimeout(loadOrders, 1000);
                } else {
                    throw new Error(\`HTTP \${response.status}\`);
                }
            } catch (error) {
                resultDiv.innerHTML = \`
                    <div class="result error">
                        <strong>❌ Erro ao criar pedido:</strong><br>
                        <small>\${error.message}</small>
                    </div>
                \`;
            }
        }

        async function loadOrders() {
            const container = document.getElementById('orders-container');
            
            try {
                const response = await fetch('/api/orders');
                
                if (response.ok) {
                    const data = await response.json();
                    orders = data.orders || data || [];
                    
                    updateStats();
                    displayOrders();
                } else if (response.status === 404) {
                    // Service not ready yet
                    container.innerHTML = '<div class="result info">📡 Aguardando microsserviços...</div>';
                } else {
                    throw new Error(\`HTTP \${response.status}\`);
                }
            } catch (error) {
                container.innerHTML = \`
                    <div class="result info">
                        🔄 Conectando aos microsserviços...<br>
                        <small>Aguarde, sistema inicializando</small>
                    </div>
                \`;
            }
        }

        function updateStats() {
            document.getElementById('total-orders').textContent = orders.length;
            document.getElementById('pending-orders').textContent = orders.filter(o => o.status === 'PENDING' || o.status === 'CREATED').length;
            document.getElementById('completed-orders').textContent = orders.filter(o => o.status === 'COMPLETED' || o.status === 'CONFIRMED').length;
        }

        function displayOrders() {
            const container = document.getElementById('orders-container');
            
            if (orders.length === 0) {
                container.innerHTML = '<div class="result info">📝 Nenhum pedido encontrado. Crie o primeiro!</div>';
                return;
            }

            const tableHtml = \`
                <table class="orders-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Cliente</th>
                            <th>Total</th>
                            <th>Status</th>
                            <th>Data</th>
                        </tr>
                    </thead>
                    <tbody>
                        \${orders.map(order => \`
                            <tr>
                                <td>\${order.id || order.orderId || 'N/A'}</td>
                                <td>\${order.customerName || order.customer || 'N/A'}</td>
                                <td>R$ \${(order.totalAmount || order.total || 0).toFixed(2)}</td>
                                <td><span class="status \${order.status === 'COMPLETED' ? 'up' : 'loading'}">\${order.status || 'PENDING'}</span></td>
                                <td>\${order.createdAt ? new Date(order.createdAt).toLocaleString('pt-BR') : 'Agora'}</td>
                            </tr>
                        \`).join('')}
                    </tbody>
                </table>
            \`;
            
            container.innerHTML = tableHtml;
        }

        async function testService(serviceName) {
            const resultDiv = document.getElementById('services-result');
            const endpoints = {
                'order': '/api/orders',
                'payment': '/api/payments', 
                'inventory': '/api/inventory',
                'query': '/api/orders'
            };
            
            resultDiv.innerHTML = \`<div class="loading">🧪 Testando \${serviceName} service...</div>\`;
            
            try {
                const response = await fetch(endpoints[serviceName]);
                const data = await response.text();
                
                resultDiv.innerHTML = \`
                    <div class="result \${response.ok ? 'success' : 'error'}">
                        <strong>\${response.ok ? '✅' : '❌'} \${serviceName.toUpperCase()} Service (\${response.status})</strong><br>
                        <small>\${data.substring(0, 200)}\${data.length > 200 ? '...' : ''}</small>
                    </div>
                \`;
            } catch (error) {
                resultDiv.innerHTML = \`
                    <div class="result error">
                        <strong>❌ \${serviceName.toUpperCase()} Service Error:</strong><br>
                        <small>\${error.message}</small>
                    </div>
                \`;
            }
        }
    </script>
</body>
</html>`;

// Create dist directory and write index.html
if (!fs.existsSync(distDir)) {
    fs.mkdirSync(distDir, { recursive: true });
}

fs.writeFileSync(path.join(distDir, 'index.html'), indexHtml);
console.log('✅ Simple frontend build completed: dist/index.html created');