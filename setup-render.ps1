# 🚀 SETUP RENDER.COM - 100% GRATUITO
# Sistema de Gestão de Pedidos Distribuído

Write-Host "🆓 SETUP RENDER.COM - Deploy 100% Gratuito" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Cyan

Write-Host "📋 RENDER.COM - PLATAFORMA GRATUITA RECOMENDADA" -ForegroundColor Yellow
Write-Host "✅ PostgreSQL 1GB gratuito" -ForegroundColor Green
Write-Host "✅ Redis 25MB gratuito" -ForegroundColor Green  
Write-Host "✅ 750 horas/mês por serviço" -ForegroundColor Green
Write-Host "✅ Deploy automático via Git" -ForegroundColor Green
Write-Host "✅ SSL automático" -ForegroundColor Green
Write-Host "✅ Logs e monitoring inclusos" -ForegroundColor Green

Write-Host "`n🔧 PREPARANDO ARQUIVOS PARA RENDER..." -ForegroundColor Yellow

# Criar render.yaml para deployment automático
$renderConfig = @"
services:
  # Order Service
  - type: web
    name: order-service
    env: java
    buildCommand: cd services/order-service && mvn clean package -DskipTests
    startCommand: cd services/order-service && java -jar target/order-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8081
      - key: SPRING_PROFILES_ACTIVE
        value: render
      - key: DATABASE_URL
        fromDatabase:
          name: ordersystem-db
          property: connectionString
      - key: REDIS_URL
        fromService:
          type: redis
          name: ordersystem-redis
          property: connectionString

  # Payment Service  
  - type: web
    name: payment-service
    env: java
    buildCommand: cd services/payment-service && mvn clean package -DskipTests
    startCommand: cd services/payment-service && java -jar target/payment-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8082
      - key: SPRING_PROFILES_ACTIVE
        value: render
      - key: DATABASE_URL
        fromDatabase:
          name: ordersystem-db
          property: connectionString
      - key: REDIS_URL
        fromService:
          type: redis
          name: ordersystem-redis
          property: connectionString

  # Inventory Service
  - type: web
    name: inventory-service
    env: java
    buildCommand: cd services/inventory-service && mvn clean package -DskipTests
    startCommand: cd services/inventory-service && java -jar target/inventory-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8083
      - key: SPRING_PROFILES_ACTIVE
        value: render
      - key: DATABASE_URL
        fromDatabase:
          name: ordersystem-db
          property: connectionString
      - key: REDIS_URL
        fromService:
          type: redis
          name: ordersystem-redis
          property: connectionString

  # Query Service
  - type: web
    name: query-service
    env: java
    buildCommand: cd services/order-query-service && mvn clean package -DskipTests
    startCommand: cd services/order-query-service && java -jar target/order-query-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8084
      - key: SPRING_PROFILES_ACTIVE
        value: render
      - key: DATABASE_URL
        fromDatabase:
          name: ordersystem-db
          property: connectionString
      - key: REDIS_URL
        fromService:
          type: redis
          name: ordersystem-redis
          property: connectionString

  # Frontend
  - type: web
    name: frontend
    env: node
    buildCommand: cd frontend && npm install && npm run build
    startCommand: cd frontend && npm run preview -- --port $PORT --host 0.0.0.0
    envVars:
      - key: PORT
        value: 3000
      - key: VITE_API_URL
        value: https://order-service.onrender.com

databases:
  - name: ordersystem-db
    databaseName: ordersystem
    user: ordersystem

services:
  - type: redis
    name: ordersystem-redis
    maxmemoryPolicy: allkeys-lru
"@

$renderConfig | Out-File -FilePath "render.yaml" -Encoding UTF8
Write-Host "✅ render.yaml criado!" -ForegroundColor Green

# Criar profile render para cada service
$renderProfile = @"
# Render.com Production Profile
server:
  port: `${PORT:8080}

spring:
  profiles:
    active: render
  datasource:
    url: `${DATABASE_URL}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      
  data:
    redis:
      url: `${REDIS_URL}
      
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    database-platform: org.hibernate.dialect.PostgreSQLDialect

logging:
  level:
    com.ordersystem: INFO
    org.springframework: WARN
    org.hibernate: WARN
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

# RabbitMQ - usar CloudAMQP gratuito
rabbitmq:
  host: `${CLOUDAMQP_URL:localhost}
  port: 5672
  username: guest
  password: guest
"@

# Criar perfis render para cada service
$services = @("order-service", "payment-service", "inventory-service", "order-query-service")
foreach ($service in $services) {
    $profilePath = "services\$service\src\main\resources\application-render.yml"
    $renderProfile | Out-File -FilePath $profilePath -Encoding UTF8
    Write-Host "✅ application-render.yml criado para $service" -ForegroundColor Green
}

# Criar build script para Render
$buildScript = @"
#!/bin/bash
echo "🔧 Building Shared Events..."
cd shared-events && mvn clean install -DskipTests
echo "✅ Shared Events built successfully!"
"@

$buildScript | Out-File -FilePath "build.sh" -Encoding UTF8
Write-Host "✅ build.sh criado!" -ForegroundColor Green

# Atualizar frontend para produção
$frontendEnv = @"
VITE_API_URL=https://order-service.onrender.com
VITE_QUERY_API_URL=https://query-service.onrender.com
VITE_PAYMENT_API_URL=https://payment-service.onrender.com
VITE_INVENTORY_API_URL=https://inventory-service.onrender.com
"@

$frontendEnv | Out-File -FilePath "frontend\.env.production" -Encoding UTF8
Write-Host "✅ Frontend .env.production criado!" -ForegroundColor Green

Write-Host "`n🎉 SETUP RENDER.COM CONCLUÍDO!" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Cyan

Write-Host "`n📋 PRÓXIMOS PASSOS:" -ForegroundColor Yellow
Write-Host "1. 📚 Vá para: https://render.com" -ForegroundColor White
Write-Host "2. 🔐 Faça signup/login gratuito" -ForegroundColor White
Write-Host "3. 🔗 Conecte seu repositório GitHub" -ForegroundColor White
Write-Host "4. 📂 Selecione este repositório" -ForegroundColor White
Write-Host "5. 🚀 Render detectará render.yaml automaticamente" -ForegroundColor White
Write-Host "6. ✅ Deploy automático será iniciado!" -ForegroundColor White

Write-Host "`n🌐 APÓS DEPLOY VOCÊ TERÁ:" -ForegroundColor Yellow
Write-Host "   Frontend: https://frontend.onrender.com" -ForegroundColor White
Write-Host "   APIs: https://{service}.onrender.com" -ForegroundColor White
Write-Host "   PostgreSQL: Automático" -ForegroundColor White
Write-Host "   Redis: Automático" -ForegroundColor White

Write-Host "`n💡 DICAS:" -ForegroundColor Yellow
Write-Host "   • Push para GitHub = Deploy automático" -ForegroundColor White
Write-Host "   • Logs disponíveis no dashboard" -ForegroundColor White
Write-Host "   • SSL automático incluído" -ForegroundColor White
Write-Host "   • 750 horas/mês por serviço (mais que suficiente)" -ForegroundColor White

Write-Host "`n✨ DEPLOY 100% GRATUITO CONFIGURADO! ✨" -ForegroundColor Green