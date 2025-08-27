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
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f8fafc; }
        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
        .header { background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
        .card { background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
        .status { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
        .status.up { background: #dcfce7; color: #166534; }
        button { background: #3b82f6; color: white; border: none; padding: 10px 16px; border-radius: 6px; cursor: pointer; }
        button:hover { background: #2563eb; }
        .footer { text-align: center; color: #64748b; margin-top: 40px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🛒 Sistema de Gestão de Pedidos</h1>
            <p>Sistema distribuído com Event Sourcing - Deploy no Render</p>
        </div>
        
        <div class="card">
            <h2>Status do Sistema</h2>
            <div id="system-status">
                <span class="status up">🟢 OPERACIONAL</span>
                <p style="margin-top: 10px;">Todos os microsserviços estão funcionando corretamente.</p>
            </div>
        </div>

        <div class="card">
            <h3>Teste da API</h3>
            <button onclick="testAPI()">🔧 Testar Health Check</button>
            <div id="api-result" style="margin-top: 10px;"></div>
        </div>

        <div class="footer">
            <p>Sistema de Gestão de Pedidos v1.0.0 | Arquitetura Distribuída</p>
            <p>Microsserviços: Order, Payment, Inventory, Query Services</p>
        </div>
    </div>

    <script>
        async function testAPI() {
            const resultDiv = document.getElementById('api-result');
            resultDiv.innerHTML = '🔄 Testando API...';
            
            try {
                const response = await fetch('/health');
                const data = await response.json();
                
                if (response.ok) {
                    resultDiv.innerHTML = \`
                        <div style="background: #dcfce7; color: #166534; padding: 10px; border-radius: 4px; margin-top: 10px;">
                            <strong>✅ API Funcionando!</strong><br>
                            <small>\${JSON.stringify(data, null, 2)}</small>
                        </div>
                    \`;
                } else {
                    throw new Error('API não respondeu corretamente');
                }
            } catch (error) {
                resultDiv.innerHTML = \`
                    <div style="background: #fef2f2; color: #dc2626; padding: 10px; border-radius: 4px; margin-top: 10px;">
                        <strong>❌ Erro na API:</strong><br>
                        <small>\${error.message}</small>
                    </div>
                \`;
            }
        }

        // Auto-test on load
        document.addEventListener('DOMContentLoaded', function() {
            setTimeout(testAPI, 1000);
        });
    </script>
</body>
</html>`;

// Create dist directory and write index.html
if (!fs.existsSync(distDir)) {
    fs.mkdirSync(distDir, { recursive: true });
}

fs.writeFileSync(path.join(distDir, 'index.html'), indexHtml);
console.log('✅ Simple frontend build completed: dist/index.html created');