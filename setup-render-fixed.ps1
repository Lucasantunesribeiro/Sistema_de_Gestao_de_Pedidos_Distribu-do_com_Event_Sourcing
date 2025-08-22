# 🚀 SETUP RENDER.COM - 100% GRATUITO (VERSÃO CORRIGIDA)
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

# Criar render.yaml usando here-string literal
$renderYaml = @'
services:
  # Order Service
  - type: web
    name: order-service
    env: java
    buildCommand: mvn clean install -DskipTests && cd services/order-service && mvn clean package -DskipTests
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
    buildCommand: mvn clean install -DskipTests && cd services/payment-service && mvn clean package -DskipTests
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
    buildCommand: mvn clean install -DskipTests && cd services/inventory-service && mvn clean package -DskipTests
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
    buildCommand: mvn clean install -DskipTests && cd services/order-query-service && mvn clean package -DskipTests
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
'@

$renderYaml | Out-File -FilePath "render.yaml" -Encoding UTF8
Write-Host "✅ render.yaml criado!" -ForegroundColor Green

# Criar profile render usando here-string literal
$renderProfile = @'
# Render.com Production Profile
server:
  port: ${PORT:8080}

spring:
  profiles:
    active: render
  datasource:
    url: ${DATABASE_URL}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      
  data:
    redis:
      url: ${REDIS_URL}
      
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
'@

# Criar perfis render para cada service
$services = @("order-service", "payment-service", "inventory-service", "order-query-service")
foreach ($service in $services) {
    $profilePath = "services\$service\src\main\resources\application-render.yml"
    
    # Verificar se o diretório existe
    $directory = Split-Path $profilePath -Parent
    if (!(Test-Path $directory)) {
        Write-Host "   📁 Criando diretório: $directory" -ForegroundColor Gray
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    
    $renderProfile | Out-File -FilePath $profilePath -Encoding UTF8
    Write-Host "✅ application-render.yml criado para $service" -ForegroundColor Green
}

# Criar frontend environment
$frontendEnv = @'
VITE_API_URL=https://order-service.onrender.com
VITE_QUERY_API_URL=https://query-service.onrender.com
VITE_PAYMENT_API_URL=https://payment-service.onrender.com
VITE_INVENTORY_API_URL=https://inventory-service.onrender.com
'@

$frontendEnv | Out-File -FilePath "frontend\.env.production" -Encoding UTF8
Write-Host "✅ Frontend .env.production criado!" -ForegroundColor Green

# Criar Dockerfile para cada serviço
$services = @("order-service", "payment-service", "inventory-service", "order-query-service")
foreach ($service in $services) {
    $dockerfile = @"
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy the built jar
COPY target/$service-1.0.0.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
"@
    
    $dockerfilePath = "services\$service\Dockerfile"
    $dockerfile | Out-File -FilePath $dockerfilePath -Encoding UTF8
    Write-Host "✅ Dockerfile criado para $service" -ForegroundColor Green
}

# Criar Dockerfile para frontend
$frontendDockerfile = @'
FROM node:18-alpine as builder

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM node:18-alpine

WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY package*.json ./

RUN npm ci --only=production && npm install -g serve

EXPOSE 3000

CMD ["serve", "-s", "dist", "-l", "3000"]
'@

$frontendDockerfile | Out-File -FilePath "frontend\Dockerfile" -Encoding UTF8
Write-Host "✅ Frontend Dockerfile criado!" -ForegroundColor Green

Write-Host "`n🎉 SETUP RENDER.COM CONCLUÍDO!" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Cyan

Write-Host "`n📋 PRÓXIMOS PASSOS:" -ForegroundColor Yellow
Write-Host "1. 🔗 Suba seu código para GitHub:" -ForegroundColor White
Write-Host "   git add ." -ForegroundColor Gray
Write-Host "   git commit -m 'Setup Render deployment'" -ForegroundColor Gray
Write-Host "   git push origin main" -ForegroundColor Gray
Write-Host ""
Write-Host "2. 📚 Vá para: https://render.com" -ForegroundColor White
Write-Host "3. 🔐 Faça signup/login gratuito" -ForegroundColor White
Write-Host "4. 🔗 Conecte seu repositório GitHub" -ForegroundColor White
Write-Host "5. 📂 Selecione este repositório" -ForegroundColor White
Write-Host "6. 🚀 Render detectará render.yaml automaticamente" -ForegroundColor White
Write-Host "7. ✅ Deploy automático será iniciado!" -ForegroundColor White

Write-Host "`n🌐 APÓS DEPLOY VOCÊ TERÁ:" -ForegroundColor Yellow
Write-Host "   Frontend: https://frontend.onrender.com" -ForegroundColor White
Write-Host "   Order API: https://order-service.onrender.com" -ForegroundColor White
Write-Host "   Query API: https://query-service.onrender.com" -ForegroundColor White
Write-Host "   Payment API: https://payment-service.onrender.com" -ForegroundColor White
Write-Host "   Inventory API: https://inventory-service.onrender.com" -ForegroundColor White

Write-Host "`n💡 DICAS:" -ForegroundColor Yellow
Write-Host "   • Push para GitHub = Deploy automático" -ForegroundColor White
Write-Host "   • Logs disponíveis no dashboard" -ForegroundColor White
Write-Host "   • SSL automático incluído" -ForegroundColor White
Write-Host "   • 750 horas/mês por serviço (mais que suficiente)" -ForegroundColor White

Write-Host "`n📁 ARQUIVOS CRIADOS:" -ForegroundColor Yellow
Write-Host "   • render.yaml - Configuração principal" -ForegroundColor White
Write-Host "   • application-render.yml - Perfis de produção (4x)" -ForegroundColor White  
Write-Host "   • Dockerfile - Para cada serviço (5x)" -ForegroundColor White
Write-Host "   • .env.production - Frontend environment" -ForegroundColor White

Write-Host "`n✨ DEPLOY 100% GRATUITO CONFIGURADO! ✨" -ForegroundColor Green
Write-Host "📝 Execute: git add . && git commit -m 'Deploy setup' && git push" -ForegroundColor Cyan