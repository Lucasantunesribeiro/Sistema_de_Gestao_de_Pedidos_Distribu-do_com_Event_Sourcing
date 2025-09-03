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
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>");
            html.append("<html lang='pt-BR'>");
            html.append("<head>");
            html.append("<meta charset='UTF-8'>");
            html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            html.append("<title>Sistema de Gestão de Pedidos - Dashboard</title>");
            html.append("<style>");
            html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
            html.append("body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; color: #1a202c; }");
            html.append(".container { max-width: 1200px; margin: 0 auto; padding: 24px; }");
            html.append(".header { background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(10px); border-radius: 16px; padding: 32px; margin-bottom: 32px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); text-align: center; }");
            html.append(".header h1 { font-size: 2.5rem; font-weight: 800; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 8px; }");
            html.append(".header p { color: #64748b; font-size: 1.1rem; font-weight: 500; }");
            html.append(".grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 24px; }");
            html.append(".card { background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(10px); border-radius: 16px; padding: 24px; box-shadow: 0 8px 32px rgba(0,0,0,0.1); transition: transform 0.2s ease; }");
            html.append(".card:hover { transform: translateY(-4px); }");
            html.append(".card h2, .card h3 { font-weight: 700; margin-bottom: 16px; font-size: 1.4rem; color: #2d3748; }");
            html.append(".status { display: inline-flex; align-items: center; padding: 6px 12px; border-radius: 20px; font-size: 13px; font-weight: 600; gap: 6px; }");
            html.append(".status.up { background: #d1fae5; color: #065f46; }");
            html.append(".btn { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; padding: 12px 20px; border-radius: 10px; cursor: pointer; margin: 6px; font-weight: 600; font-size: 14px; transition: all 0.2s ease; }");
            html.append(".btn:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4); }");
            html.append(".btn.success { background: linear-gradient(135deg, #10b981 0%, #059669 100%); }");
            html.append(".result { margin-top: 16px; padding: 14px; border-radius: 10px; font-size: 14px; border-left: 4px solid; }");
            html.append(".result.success { background: #ecfdf5; color: #065f46; border-color: #10b981; }");
            html.append(".result.error { background: #fef2f2; color: #dc2626; border-color: #f87171; }");
            html.append(".stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin: 20px 0; }");
            html.append(".stat-item { text-align: center; padding: 16px; background: #f8fafc; border-radius: 12px; }");
            html.append(".stat-number { font-size: 2rem; font-weight: 800; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }");
            html.append(".stat-label { font-size: 12px; color: #64748b; font-weight: 600; text-transform: uppercase; margin-top: 4px; }");
            html.append(".system-badge { background: rgba(102, 126, 234, 0.1); color: #667eea; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; margin-left: 8px; }");
            html.append(".footer { text-align: center; color: rgba(255,255,255,0.8); margin-top: 48px; font-weight: 500; }");
            html.append("</style>");
            html.append("</head>");
            html.append("<body>");
            html.append("<div class='container'>");
            html.append("<div class='header'>");
            html.append("<h1>🛒 Sistema de Gestão de Pedidos</h1>");
            html.append("<p>Dashboard Moderno - Sistema Unificado <span class='system-badge'>v2.0</span></p>");
            html.append("</div>");
            html.append("<div class='grid'>");
            
            // System Status Card
            html.append("<div class='card'>");
            html.append("<h2>Status do Sistema</h2>");
            html.append("<div><span class='status up'>🟢 OPERACIONAL</span></div>");
            html.append("<p style='margin-top: 10px;'>Sistema funcionando corretamente</p>");
            html.append("<button class='btn' onclick='checkHealth()'>🔧 Verificar Health</button>");
            html.append("<div id='health-result' class='result'></div>");
            html.append("</div>");
            
            // Stats Card
            html.append("<div class='card'>");
            html.append("<h3>📊 Estatísticas Rápidas</h3>");
            html.append("<div class='stats'>");
            html.append("<div class='stat-item'><div class='stat-number'>100</div><div class='stat-label'>Pedidos</div></div>");
            html.append("<div class='stat-item'><div class='stat-number'>25</div><div class='stat-label'>Pendentes</div></div>");
            html.append("<div class='stat-item'><div class='stat-number'>75</div><div class='stat-label'>Concluídos</div></div>");
            html.append("</div>");
            html.append("<button class='btn success' onclick='loadStats()'>📈 Atualizar</button>");
            html.append("<div id='stats-result' class='result'></div>");
            html.append("</div>");
            
            // Actions Card
            html.append("<div class='card'>");
            html.append("<h3>⚡ Ações Rápidas</h3>");
            html.append("<button class='btn' onclick='testAPI(\"orders\")'>📦 Orders API</button>");
            html.append("<button class='btn' onclick='testAPI(\"payments\")'>💳 Payments API</button>");
            html.append("<button class='btn' onclick='testAPI(\"inventory\")'>📊 Inventory API</button>");
            html.append("<div id='api-result' class='result'></div>");
            html.append("</div>");
            
            // System Info Card
            html.append("<div class='card'>");
            html.append("<h3>ℹ️ Informações do Sistema</h3>");
            html.append("<div style='text-align: left; font-size: 14px; line-height: 1.6;'>");
            html.append("<p><strong>Versão:</strong> 2.0.0</p>");
            html.append("<p><strong>Ambiente:</strong> Produção (Render)</p>");
            html.append("<p><strong>Arquitetura:</strong> Sistema Unificado</p>");
            html.append("<p><strong>Banco:</strong> PostgreSQL</p>");
            html.append("<p><strong>Status:</strong> <span class='status up'>Ativo</span></p>");
            html.append("</div>");
            html.append("</div>");
            
            html.append("</div>"); // grid
            html.append("<div class='footer'>");
            html.append("<p><strong>Sistema de Gestão de Pedidos v2.0</strong> | Deploy em Produção</p>");
            html.append("<p>🚀 Rodando no Render.com com arquitetura moderna</p>");
            html.append("</div>");
            html.append("</div>"); // container
            
            // JavaScript
            html.append("<script>");
            html.append("async function checkHealth() {");
            html.append("  const resultDiv = document.getElementById('health-result');");
            html.append("  resultDiv.innerHTML = '<div>🔄 Verificando...</div>';");
            html.append("  try {");
            html.append("    const response = await fetch('/health');");
            html.append("    const data = await response.json();");
            html.append("    if (response.ok) {");
            html.append("      resultDiv.innerHTML = '<div class=\"result success\"><strong>✅ Sistema Saudável!</strong><br>Status: ' + data.status + '</div>';");
            html.append("    } else { throw new Error('HTTP ' + response.status); }");
            html.append("  } catch (error) {");
            html.append("    resultDiv.innerHTML = '<div class=\"result error\"><strong>❌ Erro:</strong><br>' + error.message + '</div>';");
            html.append("  }");
            html.append("}");
            html.append("async function loadStats() {");
            html.append("  const resultDiv = document.getElementById('stats-result');");
            html.append("  resultDiv.innerHTML = '<div>📊 Carregando...</div>';");
            html.append("  try {");
            html.append("    const response = await fetch('/api/dashboard');");
            html.append("    const data = await response.json();");
            html.append("    if (response.ok) {");
            html.append("      resultDiv.innerHTML = '<div class=\"result success\"><strong>📈 Dados Atualizados!</strong><br>Total: ' + data.totalOrders + '</div>';");
            html.append("    } else { throw new Error('HTTP ' + response.status); }");
            html.append("  } catch (error) {");
            html.append("    resultDiv.innerHTML = '<div class=\"result error\"><strong>❌ Erro:</strong><br>' + error.message + '</div>';");
            html.append("  }");
            html.append("}");
            html.append("async function testAPI(endpoint) {");
            html.append("  const resultDiv = document.getElementById('api-result');");
            html.append("  resultDiv.innerHTML = '<div>🧪 Testando ' + endpoint + '...</div>';");
            html.append("  try {");
            html.append("    const response = await fetch('/api/' + endpoint);");
            html.append("    const text = await response.text();");
            html.append("    const status = response.ok ? 'success' : 'error';");
            html.append("    const icon = response.ok ? '✅' : '❌';");
            html.append("    resultDiv.innerHTML = '<div class=\"result ' + status + '\"><strong>' + icon + ' ' + endpoint.toUpperCase() + ' (' + response.status + ')</strong><br><small>' + text.substring(0, 100) + '</small></div>';");
            html.append("  } catch (error) {");
            html.append("    resultDiv.innerHTML = '<div class=\"result error\"><strong>❌ Erro na API:</strong><br>' + error.message + '</div>';");
            html.append("  }");
            html.append("}");
            html.append("document.addEventListener('DOMContentLoaded', function() { setTimeout(checkHealth, 1000); });");
            html.append("</script>");
            html.append("</body>");
            html.append("</html>");
            
            return html.toString();
        } catch (Exception e) {
            return "<html><body><h1>Sistema de Gestão de Pedidos</h1><p>Dashboard carregando... Erro: " + e.getMessage() + "</p><a href='/health'>Health Check</a></body></html>";
        }
    }
}