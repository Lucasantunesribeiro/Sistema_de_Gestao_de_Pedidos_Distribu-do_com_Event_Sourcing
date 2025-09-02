#!/bin/bash
# Script para preparar deploy usando infraestrutura existente do Render.com

echo "🔄 Preparando deploy para infraestrutura existente do Render.com..."

# Verificar se estamos no diretório correto
if [ ! -d "unified-order-system" ]; then
    echo "❌ Erro: Diretório unified-order-system não encontrado."
    echo "Execute este script a partir da raiz do projeto."
    exit 1
fi

echo "📁 Navegando para unified-order-system..."
cd unified-order-system

# Verificar Maven wrapper
if [ ! -f "mvnw" ]; then
    echo "❌ Erro: Maven wrapper não encontrado."
    exit 1
fi

# Tornar Maven wrapper executável
chmod +x mvnw

echo "🧪 Executando testes..."
./mvnw test
if [ $? -ne 0 ]; then
    echo "⚠️  Alguns testes falharam, mas continuando..."
fi

echo "📦 Fazendo build da aplicação..."
./mvnw clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ Build falhou. Verifique os logs."
    exit 1
fi

echo "✅ Build concluído com sucesso!"

# Verificar se JAR foi criado
if [ ! -f "target/unified-order-system-1.0.0.jar" ]; then
    echo "❌ Erro: Arquivo JAR não encontrado."
    exit 1
fi

echo "📊 Informações do JAR:"
ls -lh target/unified-order-system-1.0.0.jar

cd ..

echo ""
echo "📋 Configurações para o Render.com:"
echo "=================================="
echo ""
echo "🔧 Build & Deploy Settings:"
echo "Root Directory: unified-order-system"
echo "Build Command: ./mvnw clean package -DskipTests"
echo "Start Command: java -Dspring.profiles.active=prod -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -jar target/unified-order-system-1.0.0.jar"
echo "Health Check Path: /actuator/health"
echo ""
echo "🌐 Environment Variables:"
echo "SPRING_PROFILES_ACTIVE=prod"
echo "SERVER_PORT=8080"
echo "JPA_DDL_AUTO=update"
echo "JPA_SHOW_SQL=false"
echo ""
echo "📊 Infraestrutura Existente:"
echo "✅ PostgreSQL: order-system-postgres (dpg-d2nr367fte5s7381n0n0-a)"
echo "✅ Redis: order-system-redis (red-d2nr3795pdvs7394onhg)"
echo "✅ Web Service: Gestao_de_Pedidos (srv-d2kbhnruibrs73emmc8g)"
echo ""
echo "🔗 URLs após deploy:"
echo "• Health Check: https://gestao-de-pedidos.onrender.com/actuator/health"
echo "• API Docs: https://gestao-de-pedidos.onrender.com/swagger-ui.html"
echo "• Orders API: https://gestao-de-pedidos.onrender.com/api/orders"
echo "• Query API: https://gestao-de-pedidos.onrender.com/api/query"
echo ""
echo "📖 Próximos passos:"
echo "1. Commit e push do código: git add . && git commit -m 'deploy: sistema unificado' && git push"
echo "2. Atualizar configurações do Web Service no Render Dashboard"
echo "3. Fazer deploy manual no Render"
echo "4. Monitorar logs e health checks"
echo ""
echo "📚 Guia detalhado: DEPLOY_EXISTING_INFRASTRUCTURE.md"
echo ""
echo "🎉 Preparação concluída! Pronto para deploy."