# Fix Render Deploy - Criar arquivos necessarios
Write-Host "Corrigindo deploy Render.com..." -ForegroundColor Green

# 1. Criar Dockerfile na raiz para build do shared-events
Write-Host "1. Criando Dockerfile na raiz..." -ForegroundColor Yellow

$rootDockerfile = @"
FROM openjdk:17-jdk-slim as builder

WORKDIR /app

# Copy all source code
COPY . .

# Build shared-events first
RUN cd shared-events && mvn clean install -DskipTests

# Build all services
RUN cd services/order-service && mvn clean package -DskipTests
RUN cd services/payment-service && mvn clean package -DskipTests  
RUN cd services/inventory-service && mvn clean package -DskipTests
RUN cd services/order-query-service && mvn clean package -DskipTests

FROM openjdk:17-jre-slim

WORKDIR /app

# Copy built jars
COPY --from=builder /app/services/order-service/target/order-service-1.0.0.jar ./
COPY --from=builder /app/services/payment-service/target/payment-service-1.0.0.jar ./
COPY --from=builder /app/services/inventory-service/target/inventory-service-1.0.0.jar ./
COPY --from=builder /app/services/order-query-service/target/order-query-service-1.0.0.jar ./

EXPOSE 8080

# Default to order-service (will be overridden by render.yaml)
CMD ["java", "-jar", "order-service-1.0.0.jar"]
"@

$rootDockerfile | Out-File -FilePath "Dockerfile" -Encoding UTF8
Write-Host "   Dockerfile raiz criado!" -ForegroundColor Green

# 2. Atualizar render.yaml para usar Docker corretamente
Write-Host "2. Atualizando render.yaml..." -ForegroundColor Yellow

$newRenderYaml = @"
services:
  - type: web
    name: order-service
    env: docker
    dockerfilePath: ./Dockerfile
    dockerCommand: java -jar order-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8081
      - key: SPRING_PROFILES_ACTIVE
        value: render
      - key: DATABASE_URL
        fromDatabase:
          name: ordersystem-db
          property: connectionString

  - type: web
    name: payment-service
    env: docker
    dockerfilePath: ./Dockerfile
    dockerCommand: java -jar payment-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8082
      - key: SPRING_PROFILES_ACTIVE
        value: render
      - key: DATABASE_URL
        fromDatabase:
          name: ordersystem-db
          property: connectionString

  - type: web
    name: inventory-service
    env: docker
    dockerfilePath: ./Dockerfile
    dockerCommand: java -jar inventory-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8083
      - key: SPRING_PROFILES_ACTIVE
        value: render
      - key: DATABASE_URL
        fromDatabase:
          name: ordersystem-db
          property: connectionString

  - type: web
    name: query-service
    env: docker
    dockerfilePath: ./Dockerfile
    dockerCommand: java -jar order-query-service-1.0.0.jar
    envVars:
      - key: PORT
        value: 8084
      - key: SPRING_PROFILES_ACTIVE
        value: render
      - key: DATABASE_URL
        fromDatabase:
          name: ordersystem-db
          property: connectionString

  - type: web
    name: frontend
    env: node
    buildCommand: cd frontend && npm install && npm run build
    startCommand: cd frontend && npm run preview -- --port `$PORT --host 0.0.0.0
    envVars:
      - key: PORT
        value: 3000
      - key: VITE_API_URL
        value: https://order-service.onrender.com

databases:
  - name: ordersystem-db
    databaseName: ordersystem
    user: ordersystem
"@

$newRenderYaml | Out-File -FilePath "render.yaml" -Encoding UTF8
Write-Host "   render.yaml atualizado!" -ForegroundColor Green

# 3. Criar arquivo de build script
Write-Host "3. Criando build script..." -ForegroundColor Yellow

$buildScript = @"
#!/bin/bash
set -e
echo "Building Sistema de Gestao de Pedidos..."

echo "1. Building shared-events..."
cd shared-events
mvn clean install -DskipTests -q
cd ..

echo "2. Building services..."
cd services/order-service
mvn clean package -DskipTests -q
cd ../..

cd services/payment-service  
mvn clean package -DskipTests -q
cd ../..

cd services/inventory-service
mvn clean package -DskipTests -q
cd ../..

cd services/order-query-service
mvn clean package -DskipTests -q
cd ../..

echo "Build completed successfully!"
"@

$buildScript | Out-File -FilePath "build.sh" -Encoding UTF8
Write-Host "   build.sh criado!" -ForegroundColor Green

# 4. Criar start script para cada service
Write-Host "4. Criando start scripts..." -ForegroundColor Yellow

$startScript = @"
#!/bin/bash
SERVICE_NAME=`${1:-order-service}
echo "Starting `$SERVICE_NAME..."
java -jar `$SERVICE_NAME-1.0.0.jar
"@

$startScript | Out-File -FilePath "start.sh" -Encoding UTF8
Write-Host "   start.sh criado!" -ForegroundColor Green

Write-Host "`nArquivos corrigidos!" -ForegroundColor Green
Write-Host "===================" -ForegroundColor Cyan

Write-Host "`nFaca commit e push:" -ForegroundColor Yellow
Write-Host "git add ." -ForegroundColor White
Write-Host "git commit -m 'Fix Render deploy with Dockerfile'" -ForegroundColor White
Write-Host "git push origin main" -ForegroundColor White

Write-Host "`nDepois va no Render e faca Manual Deploy!" -ForegroundColor Green