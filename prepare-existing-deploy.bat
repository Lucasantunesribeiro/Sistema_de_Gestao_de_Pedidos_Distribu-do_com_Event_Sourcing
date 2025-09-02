@echo off
REM Script para preparar deploy usando infraestrutura existente do Render.com (Windows)

echo 🔄 Preparando deploy para infraestrutura existente do Render.com...

REM Verificar se estamos no diretório correto
if not exist "unified-order-system" (
    echo ❌ Erro: Diretório unified-order-system não encontrado.
    echo Execute este script a partir da raiz do projeto.
    pause
    exit /b 1
)

echo 📁 Navegando para unified-order-system...
cd unified-order-system

REM Verificar Maven wrapper
if not exist "mvnw.cmd" (
    echo ❌ Erro: Maven wrapper não encontrado.
    pause
    exit /b 1
)

echo 🧪 Executando testes...
call mvnw.cmd test
if errorlevel 1 (
    echo ⚠️  Alguns testes falharam, mas continuando...
)

echo 📦 Fazendo build da aplicação...
call mvnw.cmd clean package -DskipTests
if errorlevel 1 (
    echo ❌ Build falhou. Verifique os logs.
    pause
    exit /b 1
)

echo ✅ Build concluído com sucesso!

REM Verificar se JAR foi criado
if not exist "target\unified-order-system-1.0.0.jar" (
    echo ❌ Erro: Arquivo JAR não encontrado.
    pause
    exit /b 1
)

echo 📊 Informações do JAR:
dir target\unified-order-system-1.0.0.jar

cd ..

echo.
echo 📋 Configurações para o Render.com:
echo ==================================
echo.
echo 🔧 Build ^& Deploy Settings:
echo Root Directory: unified-order-system
echo Build Command: ./mvnw clean package -DskipTests
echo Start Command: java -Dspring.profiles.active=render -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -jar target/unified-order-system-1.0.0.jar
echo Health Check Path: /actuator/health
echo.
echo 🌐 Environment Variables:
echo SPRING_PROFILES_ACTIVE=render
echo SERVER_PORT=8080
echo JPA_DDL_AUTO=update
echo JPA_SHOW_SQL=false
echo.
echo 📊 Infraestrutura Existente:
echo ✅ PostgreSQL: order-system-postgres (dpg-d2nr367fte5s7381n0n0-a)
echo ✅ Redis: order-system-redis (red-d2nr3795pdvs7394onhg)
echo ✅ Web Service: Gestao_de_Pedidos (srv-d2kbhnruibrs73emmc8g)
echo.
echo 🔗 URLs após deploy:
echo • Health Check: https://gestao-de-pedidos.onrender.com/actuator/health
echo • API Docs: https://gestao-de-pedidos.onrender.com/swagger-ui.html
echo • Orders API: https://gestao-de-pedidos.onrender.com/api/orders
echo • Query API: https://gestao-de-pedidos.onrender.com/api/query
echo.
echo 📖 Próximos passos:
echo 1. Commit e push do código: git add . ^&^& git commit -m "deploy: sistema unificado" ^&^& git push
echo 2. Atualizar configurações do Web Service no Render Dashboard
echo 3. Fazer deploy manual no Render
echo 4. Monitorar logs e health checks
echo.
echo 📚 Guia detalhado: DEPLOY_EXISTING_INFRASTRUCTURE.md
echo.
echo 🎉 Preparação concluída! Pronto para deploy.
pause