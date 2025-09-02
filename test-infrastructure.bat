@echo off
REM Script para testar conectividade com infraestrutura existente (Windows)

echo 🔍 Testando conectividade com infraestrutura existente...

REM URL base do seu serviço
set BASE_URL=https://gestao-de-pedidos.onrender.com

echo 📡 Testando conectividade básica...
curl -s --connect-timeout 10 "%BASE_URL%" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Serviço está acessível
) else (
    echo ❌ Serviço não está acessível
    echo Verifique se o deploy foi realizado corretamente
    pause
    exit /b 1
)

echo.
echo 🏥 Testando health check...
curl -s --connect-timeout 10 "%BASE_URL%/actuator/health" > temp_health.json 2>nul

if %errorlevel% equ 0 (
    echo ✅ Health check endpoint acessível
    
    REM Verificar se a resposta contém status UP
    findstr /C:"\"status\":\"UP\"" temp_health.json >nul
    if %errorlevel% equ 0 (
        echo ✅ Aplicação está UP
        
        REM Verificar componentes específicos
        findstr /C:"\"database\"" temp_health.json >nul
        if %errorlevel% equ 0 (
            echo ✅ Database conectado
        ) else (
            echo ⚠️  Database status não encontrado
        )
        
        findstr /C:"\"redis\"" temp_health.json >nul
        if %errorlevel% equ 0 (
            echo ✅ Redis conectado
        ) else (
            echo ⚠️  Redis status não encontrado
        )
        
    ) else (
        echo ❌ Aplicação não está UP
        echo Resposta:
        type temp_health.json
    )
) else (
    echo ❌ Health check não acessível
)

if exist temp_health.json del temp_health.json

echo.
echo 📚 Testando documentação da API...
curl -s --connect-timeout 10 "%BASE_URL%/swagger-ui.html" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Swagger UI acessível
) else (
    echo ⚠️  Swagger UI não acessível (pode estar carregando)
)

echo.
echo 🛒 Testando endpoint de pedidos...
curl -s --connect-timeout 10 "%BASE_URL%/api/orders" > temp_orders.json 2>nul

if %errorlevel% equ 0 (
    echo ✅ Endpoint de pedidos acessível
    findstr /C:"[" temp_orders.json >nul
    if %errorlevel% equ 0 (
        echo ✅ Resposta válida (array JSON)
    ) else (
        echo ⚠️  Resposta inesperada:
        type temp_orders.json
    )
) else (
    echo ❌ Endpoint de pedidos não acessível
)

if exist temp_orders.json del temp_orders.json

echo.
echo 🔍 Testando endpoint de query...
curl -s --connect-timeout 10 "%BASE_URL%/api/query/orders" > temp_query.json 2>nul

if %errorlevel% equ 0 (
    echo ✅ Endpoint de query acessível
    findstr /C:"[" temp_query.json >nul
    if %errorlevel% equ 0 (
        echo ✅ Resposta válida (array JSON)
    ) else (
        echo ⚠️  Resposta inesperada:
        type temp_query.json
    )
) else (
    echo ❌ Endpoint de query não acessível
)

if exist temp_query.json del temp_query.json

echo.
echo 📊 Resumo dos testes:
echo ====================
echo 🌐 URL Base: %BASE_URL%
echo 🏥 Health Check: %BASE_URL%/actuator/health
echo 📚 API Docs: %BASE_URL%/swagger-ui.html
echo 🛒 Orders API: %BASE_URL%/api/orders
echo 🔍 Query API: %BASE_URL%/api/query
echo.
echo 💡 Dicas:
echo • Se algum teste falhou, verifique os logs no Render Dashboard
echo • Aguarde alguns minutos após o deploy para todos os serviços ficarem prontos
echo • Verifique se as variáveis de ambiente estão configuradas corretamente
echo.
echo 🎉 Teste de infraestrutura concluído!
pause