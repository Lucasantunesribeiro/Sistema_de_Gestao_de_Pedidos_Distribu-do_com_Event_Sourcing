#!/bin/bash
# Teste todos os MCPs configurados

echo "🧪 Testando conectividade dos MCPs..."

test_mcp() {
    local name="$1"
    local command="$2"
    echo -n "  🔍 $name... "
    
    if timeout 5s $command --help > /dev/null 2>&1; then
        echo "✅"
    else
        echo "❌"
    fi
}

test_mcp "Maven" "npx -y mcp-maven-deps"
test_mcp "PostgreSQL" "npx -y @henkey/postgres-mcp-server"
test_mcp "Redis" "npx -y @modelcontextprotocol/server-redis"
test_mcp "shadcn/ui" "npx -y @jpisnice/shadcn-ui-mcp-server"
test_mcp "GitHub" "npx -y @modelcontextprotocol/server-github"
test_mcp "OpenAPI" "npx -y @reapi/mcp-openapi"
test_mcp "Memory" "npx -y @modelcontextprotocol/server-memory"
test_mcp "Package Version" "npx -y mcp-package-version"

if command -v uvx &> /dev/null; then
    test_mcp "Docker" "uvx docker-mcp"
    test_mcp "RabbitMQ" "uvx mcp-server-rabbitmq@latest"
else
    echo "  ⚠️  UV não encontrado - MCPs Docker/RabbitMQ não testados"
fi

echo ""
echo "✅ Teste de MCPs concluído!"
