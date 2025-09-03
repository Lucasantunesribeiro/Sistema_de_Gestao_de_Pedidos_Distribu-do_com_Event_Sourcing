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
        return getSimpleDashboardHTML();
    }

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        return getSimpleDashboardHTML();
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

    private String getSimpleDashboardHTML() {
        return "<!DOCTYPE html>" +
               "<html lang='pt-BR'>" +
               "<head>" +
               "<meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "<title>Sistema de Gestão de Pedidos</title>" +
               "<style>" +
               "body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; }" +
               ".container { max-width: 1200px; margin: 0 auto; }" +
               ".header { background: white; padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 30px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
               ".header h1 { color: #333; margin: 0 0 10px 0; }" +
               ".header p { color: #666; margin: 0; }" +
               ".grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }" +
               ".card { background: white; padding: 20px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
               ".card h2, .card h3 { color: #333; margin-top: 0; }" +
               ".status { display: inline-block; padding: 5px 10px; border-radius: 15px; font-size: 12px; font-weight: bold; }" +
               ".status.up { background: #d4edda; color: #155724; }" +
               ".btn { background: #667eea; color: white; border: none; padding: 10px 15px; border-radius: 5px; cursor: pointer; margin: 5px; }" +
               ".btn:hover { background: #5a6fd8; }" +
               ".btn.success { background: #28a745; }" +
               ".btn.success:hover { background: #218838; }" +
               ".result { margin-top: 15px; padding: 10px; border-radius: 5px; font-size: 14px; }" +
               ".result.success { background: #d4edda; color: #155724; border-left: 4px solid #28a745; }" +
               ".result.error { background: #f8d7da; color: #721c24; border-left: 4px solid #dc3545; }" +
               ".stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin: 15px 0; }" +
               ".stat-item { text-align: center; padding: 15px; background: #f8f9fa; border-radius: 8px; }" +
               ".stat-number { font-size: 24px; font-weight: bold; color: #667eea; }" +
               ".stat-label { font-size: 12px; color: #666; text-transform: uppercase; margin-top: 5px; }" +
               ".footer { text-align: center; color: white; margin-top: 40px; }" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class='container'>" +
               "<div class='header'>" +
               "<h1>🛒 Sistema de Gestão de Pedidos</h1>" +
               "<p>Dashboard - Sistema Unificado v2.0</p>" +
               "</div>" +
               "<div class='grid'>" +
               "<div class='card'>" +
               "<h2>Status do Sistema</h2>" +
               "<div><span class='status up'>🟢 OPERACIONAL</span></div>" +
               "<p style='margin-top: 10px;'>Sistema funcionando corretamente</p>" +
               "<button class='btn' onclick='checkHealth()'>🔧 Verificar Health</button>" +
               "<div id='health-result'></div>" +
               "</div>" +
               "<div class='card'>" +
               "<h3>📊 Estatísticas</h3>" +
               "<div class='stats'>" +
               "<div class='stat-item'><div class='stat-number'>100</div><div class='stat-label'>Pedidos</div></div>" +
               "<div class='stat-item'><div class='stat-number'>25</div><div class='stat-label'>Pendentes</div></div>" +
               "<div class='stat-item'><div class='stat-number'>75</div><div class='stat-label'>Concluídos</div></div>" +
               "</div>" +
               "<button class='btn success' onclick='loadStats()'>📈 Atualizar</button>" +
               "<div id='stats-result'></div>" +
               "</div>" +
               "<div class='card'>" +
               "<h3>⚡ Testes de API</h3>" +
               "<button class='btn' onclick='testAPI(\"orders\")'>📦 Orders</button>" +
               "<button class='btn' onclick='testAPI(\"payments\")'>💳 Payments</button>" +
               "<button class='btn' onclick='testAPI(\"inventory\")'>📊 Inventory</button>" +
               "<div id='api-result'></div>" +
               "</div>" +
               "<div class='card'>" +
               "<h3>ℹ️ Sistema</h3>" +
               "<p><strong>Versão:</strong> 2.0.0</p>" +
               "<p><strong>Ambiente:</strong> Produção (Render)</p>" +
               "<p><strong>Banco:</strong> PostgreSQL</p>" +
               "<p><strong>Status:</strong> <span class='status up'>Ativo</span></p>" +
               "</div>" +
               "</div>" +
               "<div class='footer'>" +
               "<p><strong>Sistema de Gestão de Pedidos v2.0</strong></p>" +
               "<p>🚀 Rodando no Render.com</p>" +
               "</div>" +
               "</div>" +
               "<script>" +
               "async function checkHealth() {" +
               "  const div = document.getElementById('health-result');" +
               "  div.innerHTML = '<div>🔄 Verificando...</div>';" +
               "  try {" +
               "    const res = await fetch('/health');" +
               "    const data = await res.json();" +
               "    if (res.ok) {" +
               "      div.innerHTML = '<div class=\"result success\">✅ Sistema OK - Status: ' + data.status + '</div>';" +
               "    } else { throw new Error('HTTP ' + res.status); }" +
               "  } catch (e) {" +
               "    div.innerHTML = '<div class=\"result error\">❌ Erro: ' + e.message + '</div>';" +
               "  }" +
               "}" +
               "async function loadStats() {" +
               "  const div = document.getElementById('stats-result');" +
               "  div.innerHTML = '<div>📊 Carregando...</div>';" +
               "  try {" +
               "    const res = await fetch('/api/dashboard');" +
               "    const data = await res.json();" +
               "    if (res.ok) {" +
               "      div.innerHTML = '<div class=\"result success\">📈 Atualizado - Total: ' + data.totalOrders + '</div>';" +
               "    } else { throw new Error('HTTP ' + res.status); }" +
               "  } catch (e) {" +
               "    div.innerHTML = '<div class=\"result error\">❌ Erro: ' + e.message + '</div>';" +
               "  }" +
               "}" +
               "async function testAPI(endpoint) {" +
               "  const div = document.getElementById('api-result');" +
               "  div.innerHTML = '<div>🧪 Testando ' + endpoint + '...</div>';" +
               "  try {" +
               "    const res = await fetch('/api/' + endpoint);" +
               "    const text = await res.text();" +
               "    const status = res.ok ? 'success' : 'error';" +
               "    const icon = res.ok ? '✅' : '❌';" +
               "    div.innerHTML = '<div class=\"result ' + status + '\">' + icon + ' ' + endpoint + ' (' + res.status + ')</div>';" +
               "  } catch (e) {" +
               "    div.innerHTML = '<div class=\"result error\">❌ Erro: ' + e.message + '</div>';" +
               "  }" +
               "}" +
               "document.addEventListener('DOMContentLoaded', function() { setTimeout(checkHealth, 1000); });" +
               "</script>" +
               "</body>" +
               "</html>";
    }
}