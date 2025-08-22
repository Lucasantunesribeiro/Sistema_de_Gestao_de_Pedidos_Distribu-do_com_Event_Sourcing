# Setup Render.com - Versao Simples
# Sistema de Gestao de Pedidos Distribuido

Write-Host "Setup Render.com - Deploy 100% Gratuito" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan

Write-Host "Criando arquivos para Render.com..." -ForegroundColor Yellow

# 1. Criar render.yaml
Write-Host "1. Criando render.yaml..." -ForegroundColor White

$renderContent = @"
services:
  - type: web
    name: order-service
    env: java
    buildCommand: cd shared-events && mvn clean install -DskipTests && cd ../services/order-service && mvn clean package -DskipTests
    startCommand: cd services/order-service && java -jar target/order-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8081
      - key: SPRING_PROFILES_ACTIVE
        value: render

  - type: web
    name: payment-service
    env: java
    buildCommand: cd shared-events && mvn clean install -DskipTests && cd ../services/payment-service && mvn clean package -DskipTests
    startCommand: cd services/payment-service && java -jar target/payment-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8082
      - key: SPRING_PROFILES_ACTIVE
        value: render

  - type: web
    name: inventory-service
    env: java
    buildCommand: cd shared-events && mvn clean install -DskipTests && cd ../services/inventory-service && mvn clean package -DskipTests
    startCommand: cd services/inventory-service && java -jar target/inventory-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8083
      - key: SPRING_PROFILES_ACTIVE
        value: render

  - type: web
    name: query-service
    env: java
    buildCommand: cd shared-events && mvn clean install -DskipTests && cd ../services/order-query-service && mvn clean package -DskipTests
    startCommand: cd services/order-query-service && java -jar target/order-query-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8084
      - key: SPRING_PROFILES_ACTIVE
        value: render

  - type: web
    name: frontend
    env: node
    buildCommand: cd frontend && npm install && npm run build
    startCommand: cd frontend && npm run preview -- --port `$PORT --host 0.0.0.0
    envVars:
      - key: PORT
        value: 3000

databases:
  - name: ordersystem-db
    databaseName: ordersystem
    user: ordersystem
"@

$renderContent | Out-File -FilePath "render.yaml" -Encoding UTF8
Write-Host "   render.yaml criado!" -ForegroundColor Green

# 2. Criar perfil render para cada service
Write-Host "2. Criando perfis de producao..." -ForegroundColor White

$profileContent = @"
server:
  port: `${PORT:8080}

spring:
  profiles:
    active: render
  datasource:
    url: `${DATABASE_URL:jdbc:h2:mem:testdb}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.PostgreSQLDialect

logging:
  level:
    com.ordersystem: INFO
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
"@

# Criar para cada service
$services = @("order-service", "payment-service", "inventory-service", "order-query-service")
foreach ($service in $services) {
    $profileDir = "services\$service\src\main\resources"
    if (!(Test-Path $profileDir)) {
        New-Item -ItemType Directory -Path $profileDir -Force | Out-Null
    }
    
    $profilePath = "$profileDir\application-render.yml"
    $profileContent | Out-File -FilePath $profilePath -Encoding UTF8
    Write-Host "   application-render.yml criado para $service" -ForegroundColor Green
}

# 3. Criar frontend env
Write-Host "3. Configurando frontend..." -ForegroundColor White

$frontendDir = "frontend"
if (!(Test-Path $frontendDir)) {
    New-Item -ItemType Directory -Path $frontendDir -Force | Out-Null
}

$envContent = @"
VITE_API_URL=https://order-service.onrender.com
VITE_QUERY_API_URL=https://query-service.onrender.com
VITE_PAYMENT_API_URL=https://payment-service.onrender.com
VITE_INVENTORY_API_URL=https://inventory-service.onrender.com
"@

$envContent | Out-File -FilePath "frontend\.env.production" -Encoding UTF8
Write-Host "   .env.production criado para frontend" -ForegroundColor Green

# 4. Criar Dockerfiles simples
Write-Host "4. Criando Dockerfiles..." -ForegroundColor White

foreach ($service in $services) {
    $dockerContent = @"
FROM openjdk:17-jre-slim
WORKDIR /app
COPY target/$service-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
"@
    
    $dockerPath = "services\$service\Dockerfile"
    $dockerContent | Out-File -FilePath $dockerPath -Encoding UTF8
    Write-Host "   Dockerfile criado para $service" -ForegroundColor Green
}

# Frontend Dockerfile
$frontendDockerContent = @"
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build
RUN npm install -g serve
EXPOSE 3000
CMD ["serve", "-s", "dist", "-l", "3000"]
"@

$frontendDockerContent | Out-File -FilePath "frontend\Dockerfile" -Encoding UTF8
Write-Host "   Dockerfile criado para frontend" -ForegroundColor Green

Write-Host "`nSetup concluido com sucesso!" -ForegroundColor Green
Write-Host "=============================" -ForegroundColor Cyan

Write-Host "`nProximos passos:" -ForegroundColor Yellow
Write-Host "1. Subir para GitHub:" -ForegroundColor White
Write-Host "   git add ." -ForegroundColor Gray
Write-Host "   git commit -m 'Setup Render'" -ForegroundColor Gray  
Write-Host "   git push origin main" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Ir para https://render.com" -ForegroundColor White
Write-Host "3. Conectar repositorio GitHub" -ForegroundColor White
Write-Host "4. Deploy automatico!" -ForegroundColor White

Write-Host "`nArquivos criados:" -ForegroundColor Yellow
Write-Host "- render.yaml" -ForegroundColor White
Write-Host "- 4x application-render.yml" -ForegroundColor White
Write-Host "- 5x Dockerfile" -ForegroundColor White
Write-Host "- frontend/.env.production" -ForegroundColor White

Write-Host "`nDeploy 100% gratuito configurado!" -ForegroundColor Green