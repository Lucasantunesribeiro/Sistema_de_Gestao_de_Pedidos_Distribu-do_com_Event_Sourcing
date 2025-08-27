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
    <title>Sistema de Gestão de Pedidos - Event Sourcing</title>
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🛒</text></svg>">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            color: #1a202c;
        }
        .container { max-width: 1400px; margin: 0 auto; padding: 24px; }
        .header { 
            background: rgba(255, 255, 255, 0.95); 
            backdrop-filter: blur(10px);
            border-radius: 16px; 
            padding: 32px; 
            margin-bottom: 32px; 
            box-shadow: 0 10px 25px rgba(0,0,0,0.1); 
            border: 1px solid rgba(255,255,255,0.2);
            text-align: center;
        }
        .header h1 { 
            font-size: 2.5rem; 
            font-weight: 800; 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 8px;
        }
        .header p { 
            color: #64748b; 
            font-size: 1.1rem; 
            font-weight: 500;
        }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(380px, 1fr)); gap: 24px; margin-bottom: 32px; }
        .card { 
            background: rgba(255, 255, 255, 0.95); 
            backdrop-filter: blur(10px);
            border-radius: 16px; 
            padding: 24px; 
            box-shadow: 0 8px 32px rgba(0,0,0,0.1); 
            border: 1px solid rgba(255,255,255,0.2);
            transition: transform 0.2s ease, box-shadow 0.2s ease;
        }
        .card:hover { 
            transform: translateY(-4px); 
            box-shadow: 0 12px 40px rgba(0,0,0,0.15); 
        }
        .card h2, .card h3 { 
            font-weight: 700; 
            margin-bottom: 16px; 
            font-size: 1.4rem;
            color: #2d3748;
        }
        .status { 
            display: inline-flex; 
            align-items: center;
            padding: 6px 12px; 
            border-radius: 20px; 
            font-size: 13px; 
            font-weight: 600; 
            gap: 6px;
        }
        .status.up { background: #d1fae5; color: #065f46; }
        .status.loading { background: #fef3c7; color: #d97706; }
        .status.error { background: #fee2e2; color: #dc2626; }
        .btn { 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white; 
            border: none; 
            padding: 12px 20px; 
            border-radius: 10px; 
            cursor: pointer; 
            margin: 6px; 
            font-weight: 600;
            font-size: 14px;
            transition: all 0.2s ease;
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }
        .btn:hover { 
            transform: translateY(-2px); 
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4); 
        }
        .btn.success { background: linear-gradient(135deg, #10b981 0%, #059669 100%); }
        .btn.success:hover { box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4); }
        .btn.danger { background: linear-gradient(135deg, #f87171 0%, #dc2626 100%); }
        .btn.danger:hover { box-shadow: 0 4px 12px rgba(248, 113, 113, 0.4); }
        .btn.secondary { background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%); }
        .btn.secondary:hover { box-shadow: 0 4px 12px rgba(148, 163, 184, 0.4); }
        .result { 
            margin-top: 16px; 
            padding: 14px; 
            border-radius: 10px; 
            font-size: 14px;
            border-left: 4px solid;
        }
        .result.success { background: #ecfdf5; color: #065f46; border-color: #10b981; }
        .result.error { background: #fef2f2; color: #dc2626; border-color: #f87171; }
        .result.info { background: #eff6ff; color: #1e40af; border-color: #3b82f6; }
        input, select { 
            width: 100%; 
            padding: 12px; 
            margin: 8px 0; 
            border: 2px solid #e2e8f0; 
            border-radius: 8px; 
            font-size: 14px;
            transition: border-color 0.2s ease;
        }
        input:focus, select:focus { 
            outline: none; 
            border-color: #667eea; 
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1); 
        }
        .orders-table { 
            width: 100%; 
            border-collapse: collapse; 
            margin-top: 16px; 
            border-radius: 10px;
            overflow: hidden;
            box-shadow: 0 4px 6px rgba(0,0,0,0.05);
        }
        .orders-table th, .orders-table td { 
            padding: 12px; 
            text-align: left; 
            border-bottom: 1px solid #f1f5f9; 
        }
        .orders-table th { 
            background: #f8fafc; 
            font-weight: 700; 
            color: #475569;
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        .orders-table tbody tr:hover { 
            background: #f8fafc; 
        }
        .footer { 
            text-align: center; 
            color: rgba(255,255,255,0.8); 
            margin-top: 48px; 
            font-weight: 500;
        }
        .footer p { margin: 6px 0; }
        .stats { 
            display: grid; 
            grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); 
            gap: 16px; 
            margin: 20px 0; 
        }
        .stat-item { 
            text-align: center; 
            padding: 16px;
            background: #f8fafc;
            border-radius: 12px;
            border: 1px solid #e2e8f0;
        }
        .stat-number { 
            font-size: 2rem; 
            font-weight: 800; 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .stat-label { 
            font-size: 12px; 
            color: #64748b; 
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            margin-top: 4px;
        }
        .loading { 
            color: #64748b; 
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }
        @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
        .loading { animation: pulse 1.5s ease-in-out infinite; }
        
        .system-badge { 
            display: inline-block; 
            background: rgba(102, 126, 234, 0.1); 
            color: #667eea; 
            padding: 4px 10px; 
            border-radius: 12px; 
            font-size: 11px; 
            font-weight: 600; 
            margin-left: 8px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🛒 Sistema de Gestão de Pedidos</h1>
            <p>Arquitetura Distribuída com Event Sourcing <span class="system-badge">v2.0</span></p>
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
                        <div class="stat-label">Total</div>
                    </div>
                    <div class="stat-item">
                        <div id="pending-orders" class="stat-number">0</div>
                        <div class="stat-label">Pendentes</div>
                    </div>
                    <div class="stat-item">
                        <div id="completed-orders" class="stat-number">0</div>
                        <div class="stat-label">Concluídos</div>
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
            <p><strong>Sistema de Gestão de Pedidos v2.0.0</strong> | Arquitetura Distribuída</p>
            <p>Microsserviços: Order Service • Payment Service • Inventory Service • Query Service</p>
            <p>🚀 <strong>Deploy em Produção</strong> no Render.com com Event Sourcing & Redis</p>
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