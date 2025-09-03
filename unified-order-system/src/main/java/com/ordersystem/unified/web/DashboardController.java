package com.ordersystem.unified.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Dashboard controller for web interface.
 * Serves both HTML pages and JSON API endpoints.
 */
@RestController
public class DashboardController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return getModernDashboardHTML();
    }

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        return getModernDashboardHTML();
    }

    @GetMapping("/api/dashboard")

    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("title", "Order Management Dashboard");
        dashboard.put("totalOrders", 100);
        dashboard.put("pendingOrders", 25);
        dashboard.put("completedOrders", 70);
        dashboard.put("cancelledOrders", 5);
        dashboard.put("totalRevenue", 10000.0);
        dashboard.put("timestamp", System.currentTimeMillis());
        return dashboard;
    }

    public List<Map<String, Object>> getRecentOrders() {
        List<Map<String, Object>> orders = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> order = new HashMap<>();
            order.put("orderId", "ORDER-" + i);
            order.put("status", "PENDING");
            order.put("customerName", "Customer " + i);
            order.put("totalAmount", 100.0 * i);
            order.put("timestamp", System.currentTimeMillis());
            orders.add(order);
        }
        
        return orders;
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "Connected");
        health.put("cache", "Active");
        health.put("services", "Running");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }

    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", 100);
        stats.put("todayOrders", 15);
        stats.put("weekOrders", 75);
        stats.put("monthOrders", 300);
        stats.put("averageOrderValue", 150.0);
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    private String getModernDashboardHTML() {
        return """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sistema de Gestão de Pedidos - Dashboard</title>
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
        .footer { 
            text-align: center; 
            color: rgba(255,255,255,0.8); 
            margin-top: 48px; 
            font-weight: 500;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🛒 Sistema de Gestão de Pedidos</h1>
            <p>Dashboard Moderno - Sistema Unificado <span class="system-badge">v2.0</span></p>
        </div>
        
        <div class="grid">
            <!-- System Status -->
            <div class="card">
                <h2>Status do Sistema</h2>
                <div id="system-status">
                    <span class="status up">🟢 OPERACIONAL</span>
                    <p style="margin-top: 10px;" id="system-message">Sistema funcionando corretamente</p>
                </div>
                <button class="btn" onclick="checkHealth()">🔧 Verificar Health</button>
                <div id="health-result" class="result"></div>
            </div>

            <!-- Quick Stats -->
            <div class="card">
                <h3>📊 Estatísticas Rápidas</h3>
                <div class="stats">
                    <div class="stat-item">
                        <div class="stat-number">100</div>
                        <div class="stat-label">Pedidos</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-number">25</div>
                        <div class="stat-label">Pendentes</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-number">75</div>
                        <div class="stat-label">Concluídos</div>
                    </div>
                </div>
                <button class="btn success" onclick="loadStats()">📈 Atualizar</button>
                <div id="stats-result" class="result"></div>
            </div>

            <!-- Quick Actions -->
            <div class="card">
                <h3>⚡ Ações Rápidas</h3>
                <button class="btn" onclick="testAPI('orders')">📦 Testar Orders API</button>
                <button class="btn" onclick="testAPI('payments')">💳 Testar Payments API</button>
                <button class="btn" onclick="testAPI('inventory')">📊 Testar Inventory API</button>
                <div id="api-result" class="result"></div>
            </div>

            <!-- System Info -->
            <div class="card">
                <h3>ℹ️ Informações do Sistema</h3>
                <div style="text-align: left; font-size: 14px; line-height: 1.6;">
                    <p><strong>Versão:</strong> 2.0.0</p>
                    <p><strong>Ambiente:</strong> Produção (Render)</p>
                    <p><strong>Arquitetura:</strong> Sistema Unificado</p>
                    <p><strong>Banco:</strong> PostgreSQL</p>
                    <p><strong>Status:</strong> <span class="status up">Ativo</span></p>
                </div>
            </div>
        </div>

        <div class="footer">
            <p><strong>Sistema de Gestão de Pedidos v2.0</strong> | Deploy em Produção</p>
            <p>🚀 Rodando no Render.com com arquitetura moderna</p>
        </div>
    </div>

    <script>
        async function checkHealth() {
            const resultDiv = document.getElementById('health-result');
            resultDiv.innerHTML = '<div class="loading">🔄 Verificando saúde do sistema...</div>';
            
            try {
                const response = await fetch('/health');
                const data = await response.json();
                
                if (response.ok) {
                    resultDiv.innerHTML = `
                        <div class="result success">
                            <strong>✅ Sistema Saudável!</strong><br>
                            Status: ${data.status} | Serviço: ${data.service}
                        </div>
                    `;
                } else {
                    throw new Error(`HTTP ${response.status}`);
                }
            } catch (error) {
                resultDiv.innerHTML = `
                    <div class="result error">
                        <strong>❌ Erro no Health Check:</strong><br>
                        ${error.message}
                    </div>
                `;
            }
        }

        async function loadStats() {
            const resultDiv = document.getElementById('stats-result');
            resultDiv.innerHTML = '<div class="loading">📊 Carregando estatísticas...</div>';
            
            try {
                const response = await fetch('/api/dashboard');
                const data = await response.json();
                
                if (response.ok) {
                    resultDiv.innerHTML = `
                        <div class="result success">
                            <strong>📈 Dados Atualizados!</strong><br>
                            Total: ${data.totalOrders} | Pendentes: ${data.pendingOrders}
                        </div>
                    `;
                } else {
                    throw new Error(`HTTP ${response.status}`);
                }
            } catch (error) {
                resultDiv.innerHTML = `
                    <div class="result error">
                        <strong>❌ Erro ao carregar:</strong><br>
                        ${error.message}
                    </div>
                `;
            }
        }

        async function testAPI(endpoint) {
            const resultDiv = document.getElementById('api-result');
            resultDiv.innerHTML = `<div class="loading">🧪 Testando ${endpoint} API...</div>`;
            
            try {
                const response = await fetch(`/api/${endpoint}`);
                const text = await response.text();
                
                resultDiv.innerHTML = `
                    <div class="result ${response.ok ? 'success' : 'error'}">
                        <strong>${response.ok ? '✅' : '❌'} ${endpoint.toUpperCase()} API (${response.status})</strong><br>
                        <small>${text.substring(0, 100)}${text.length > 100 ? '...' : ''}</small>
                    </div>
                `;
            } catch (error) {
                resultDiv.innerHTML = `
                    <div class="result error">
                        <strong>❌ Erro na API:</strong><br>
                        ${error.message}
                    </div>
                `;
            }
        }

        // Auto-check health on load
        document.addEventListener('DOMContentLoaded', function() {
            setTimeout(checkHealth, 1000);
        });
    </script>
</body>
</html>
        """;
    }
}