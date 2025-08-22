# 🐳 SETUP DOCKER LOCAL - 100% GRATUITO
# Sistema de Gestão de Pedidos Distribuído

Write-Host "🐳 SETUP DOCKER LOCAL - Deploy 100% Gratuito" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Cyan

Write-Host "📋 DOCKER LOCAL - SOLUÇÃO TOTALMENTE GRATUITA" -ForegroundColor Yellow
Write-Host "✅ Zero custos - roda na sua máquina" -ForegroundColor Green
Write-Host "✅ Controle total do ambiente" -ForegroundColor Green  
Write-Host "✅ Desenvolvimento e produção idênticos" -ForegroundColor Green
Write-Host "✅ Escalabilidade local" -ForegroundColor Green
Write-Host "✅ Dados persistentes" -ForegroundColor Green

Write-Host "`n🔧 VERIFICANDO DEPENDÊNCIAS..." -ForegroundColor Yellow

# Verificar Docker
try {
    $dockerVersion = docker --version 2>$null
    if ($dockerVersion) {
        Write-Host "✅ Docker encontrado: $dockerVersion" -ForegroundColor Green
    } else {
        throw "Docker não encontrado"
    }
} catch {
    Write-Host "❌ Docker não instalado!" -ForegroundColor Red
    Write-Host "📥 Baixe e instale Docker Desktop de: https://docker.com/products/docker-desktop" -ForegroundColor White
    Write-Host "   Após instalar, execute este script novamente." -ForegroundColor Yellow
    exit 1
}

# Verificar Docker Compose
try {
    $composeVersion = docker-compose --version 2>$null
    if ($composeVersion) {
        Write-Host "✅ Docker Compose encontrado: $composeVersion" -ForegroundColor Green
    } else {
        throw "Docker Compose não encontrado"
    }
} catch {
    Write-Host "❌ Docker Compose não encontrado!" -ForegroundColor Red
    Write-Host "📥 Docker Compose vem incluído no Docker Desktop" -ForegroundColor White
    exit 1
}

Write-Host "`n🔧 CRIANDO CONFIGURAÇÃO DOCKER OTIMIZADA..." -ForegroundColor Yellow

# Criar docker-compose.production.yml otimizado
$dockerCompose = @"
version: '3.8'

services:
  # Infraestrutura
  postgres:
    image: postgres:15-alpine
    container_name: ordersystem-postgres
    environment:
      POSTGRES_DB: ordersystem
      POSTGRES_USER: ordersystem
      POSTGRES_PASSWORD: ordersystem123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ordersystem"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: ordersystem-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: ordersystem-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ordersystem
      RABBITMQ_DEFAULT_PASS: ordersystem123
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_port_connectivity"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Microsserviços
  order-service:
    build:
      context: .
      dockerfile: services/order-service/Dockerfile
    container_name: ordersystem-order-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/ordersystem
      - REDIS_URL=redis://redis:6379
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_USERNAME=ordersystem
      - RABBITMQ_PASSWORD=ordersystem123
      - SERVER_PORT=8081
    ports:
      - "8081:8081"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  payment-service:
    build:
      context: .
      dockerfile: services/payment-service/Dockerfile
    container_name: ordersystem-payment-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/ordersystem
      - REDIS_URL=redis://redis:6379
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_USERNAME=ordersystem
      - RABBITMQ_PASSWORD=ordersystem123
      - SERVER_PORT=8082
    ports:
      - "8082:8082"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  inventory-service:
    build:
      context: .
      dockerfile: services/inventory-service/Dockerfile
    container_name: ordersystem-inventory-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/ordersystem
      - REDIS_URL=redis://redis:6379
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_USERNAME=ordersystem
      - RABBITMQ_PASSWORD=ordersystem123
      - SERVER_PORT=8083
    ports:
      - "8083:8083"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  query-service:
    build:
      context: .
      dockerfile: services/order-query-service/Dockerfile
    container_name: ordersystem-query-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/ordersystem
      - REDIS_URL=redis://redis:6379
      - RABBITMQ_HOST=rabbitmq
      - RABBITMQ_USERNAME=ordersystem
      - RABBITMQ_PASSWORD=ordersystem123
      - SERVER_PORT=8084
    ports:
      - "8084:8084"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8084/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  # Frontend
  frontend:
    build:
      context: frontend
      dockerfile: Dockerfile.production
    container_name: ordersystem-frontend
    environment:
      - VITE_API_URL=http://localhost:8081
      - VITE_QUERY_API_URL=http://localhost:8084
      - VITE_PAYMENT_API_URL=http://localhost:8082
      - VITE_INVENTORY_API_URL=http://localhost:8083
    ports:
      - "3000:3000"
    depends_on:
      - order-service
      - query-service
      - payment-service
      - inventory-service

  # Nginx Proxy (opcional - para produção)
  nginx:
    image: nginx:alpine
    container_name: ordersystem-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - frontend
      - order-service
      - query-service
      - payment-service
      - inventory-service

volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:

networks:
  default:
    name: ordersystem-network
"@

$dockerCompose | Out-File -FilePath "docker-compose.production.yml" -Encoding UTF8
Write-Host "✅ docker-compose.production.yml criado!" -ForegroundColor Green

# Criar script de inicialização do banco
$initDb = @"
-- Inicialização do banco de dados
CREATE DATABASE IF NOT EXISTS ordersystem;
CREATE DATABASE IF NOT EXISTS ordersystem_query;

-- Criar usuários
CREATE USER IF NOT EXISTS 'ordersystem'@'%' IDENTIFIED BY 'ordersystem123';
GRANT ALL PRIVILEGES ON ordersystem.* TO 'ordersystem'@'%';
GRANT ALL PRIVILEGES ON ordersystem_query.* TO 'ordersystem'@'%';
FLUSH PRIVILEGES;
"@

$initDb | Out-File -FilePath "init-db.sql" -Encoding UTF8
Write-Host "✅ init-db.sql criado!" -ForegroundColor Green

# Criar Dockerfile para frontend production
$frontendDockerfile = @"
FROM node:18-alpine as builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
"@

$frontendDockerfile | Out-File -FilePath "frontend\Dockerfile.production" -Encoding UTF8
Write-Host "✅ Frontend Dockerfile.production criado!" -ForegroundColor Green

# Criar nginx.conf para proxy
$nginxConf = @"
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server order-service:8081;
        server query-service:8084;
        server payment-service:8082;
        server inventory-service:8083;
    }

    server {
        listen 80;
        
        # Frontend
        location / {
            proxy_pass http://frontend:3000;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
        }
        
        # API Gateway
        location /api/ {
            proxy_pass http://backend;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        }
    }
}
"@

$nginxConf | Out-File -FilePath "nginx.conf" -Encoding UTF8
Write-Host "✅ nginx.conf criado!" -ForegroundColor Green

# Criar scripts de gerenciamento
$startScript = @"
@echo off
echo 🚀 Iniciando Sistema de Gestão de Pedidos...
echo.

echo 🔧 Building images...
docker-compose -f docker-compose.production.yml build

echo 🚀 Starting services...
docker-compose -f docker-compose.production.yml up -d

echo ⏳ Aguardando services ficarem prontos...
timeout /t 30 /nobreak >nul

echo 🔍 Verificando status...
docker-compose -f docker-compose.production.yml ps

echo.
echo ✅ Sistema iniciado com sucesso!
echo 🌐 Frontend: http://localhost:3000
echo 📊 Order API: http://localhost:8081
echo 📊 Query API: http://localhost:8084
echo 📊 Payment API: http://localhost:8082
echo 📊 Inventory API: http://localhost:8083
echo 🐰 RabbitMQ Management: http://localhost:15672 (user: ordersystem, pass: ordersystem123)
echo.
"@

$startScript | Out-File -FilePath "start-system.bat" -Encoding UTF8
Write-Host "✅ start-system.bat criado!" -ForegroundColor Green

$stopScript = @"
@echo off
echo 🛑 Parando Sistema de Gestão de Pedidos...
docker-compose -f docker-compose.production.yml down
echo ✅ Sistema parado!
"@

$stopScript | Out-File -FilePath "stop-system.bat" -Encoding UTF8
Write-Host "✅ stop-system.bat criado!" -ForegroundColor Green

Write-Host "`n🎉 SETUP DOCKER LOCAL CONCLUÍDO!" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Cyan

Write-Host "`n🚀 PARA INICIAR O SISTEMA:" -ForegroundColor Yellow
Write-Host "   .\start-system.bat" -ForegroundColor White
Write-Host "   ou" -ForegroundColor Gray
Write-Host "   docker-compose -f docker-compose.production.yml up -d" -ForegroundColor White

Write-Host "`n🛑 PARA PARAR O SISTEMA:" -ForegroundColor Yellow
Write-Host "   .\stop-system.bat" -ForegroundColor White
Write-Host "   ou" -ForegroundColor Gray
Write-Host "   docker-compose -f docker-compose.production.yml down" -ForegroundColor White

Write-Host "`n🌐 APÓS INICIAR VOCÊ TERÁ:" -ForegroundColor Yellow
Write-Host "   Frontend: http://localhost:3000" -ForegroundColor White
Write-Host "   Order API: http://localhost:8081" -ForegroundColor White
Write-Host "   Query API: http://localhost:8084" -ForegroundColor White
Write-Host "   Payment API: http://localhost:8082" -ForegroundColor White
Write-Host "   Inventory API: http://localhost:8083" -ForegroundColor White
Write-Host "   RabbitMQ: http://localhost:15672" -ForegroundColor White

Write-Host "`n💡 COMANDOS ÚTEIS:" -ForegroundColor Yellow
Write-Host "   docker-compose logs -f                 # Ver logs" -ForegroundColor White
Write-Host "   docker-compose ps                      # Ver status" -ForegroundColor White
Write-Host "   docker-compose restart <service>       # Reiniciar serviço" -ForegroundColor White
Write-Host "   docker-compose pull                    # Atualizar images" -ForegroundColor White

Write-Host "`n✨ DEPLOY LOCAL 100% GRATUITO CONFIGURADO! ✨" -ForegroundColor Green